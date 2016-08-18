/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.poller.monitors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.net.io.CRLFLineReader;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.core.utils.TimeoutTracker;
import org.opennms.netmgt.poller.Distributable;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.NetworkInterface;
import org.opennms.netmgt.poller.NetworkInterfaceNotSupportedException;
import org.opennms.netmgt.poller.PollStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <P>
 * This class is designed to be used by the service poller framework to test the
 * availability of the SMTP service on remote interfaces. The class implements
 * the ServiceMonitor interface that allows it to be used along with other
 * plug-ins by the service poller framework.
 * </P>
 *
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
@Distributable
@Component
public final class SmtpMonitor extends AbstractServiceMonitor {

    public static final Logger LOG = LoggerFactory.getLogger(SmtpMonitor.class);

    /**
     * Default SMTP port.
     */
    private static final int DEFAULT_PORT = 25;

    /**
     * Default retries.
     */
    private static final int DEFAULT_RETRY = 0;

    /**
     * Default timeout. Specifies how long (in milliseconds) to block waiting
     * for data from the monitored interface.
     */
    private static final int DEFAULT_TIMEOUT = 3000;

    /**
     * The name of the local host.
     */
    private static final String LOCALHOST_NAME = InetAddressUtils.getLocalHostName();

    /**
     * Used to check for a multiline response. A multiline response begins with
     * the same 3 digit response code, but has a hyphen after the last number
     * instead of a space.
     */
    private static final Pattern MULTILINE;

    /**
     * Init MULTILINE
     */
    static {
        try {
            MULTILINE = Pattern.compile("^[0-9]{3}-");
        } catch (PatternSyntaxException ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <P>
     * Poll the specified address for SMTP service availability.
     * </P>
     *
     * <P>
     * During the poll an attempt is made to connect on the specified port (by
     * default TCP port 25). If the connection request is successful, the banner
     * line generated by the interface is parsed and if the extracted return
     * code indicates that we are talking to an SMTP server we continue. Next,
     * an SMTP 'HELO' command is sent to the interface. Again the response is
     * parsed and a return code extracted and verified. Finally, an SMTP 'QUIT'
     * command is sent. Provided that the interface's response is valid we set
     * the service status to SERVICE_AVAILABLE and return.
     * </P>
     */
    @Override
    public PollStatus poll(MonitoredService svc, Map<String, Object> parameters) {
        NetworkInterface<InetAddress> iface = svc.getNetInterface();

        // Get interface address from NetworkInterface
        //
        if (iface.getType() != NetworkInterface.TYPE_INET) {
            throw new NetworkInterfaceNotSupportedException("Unsupported interface type, only TYPE_INET currently supported");
        }

        TimeoutTracker tracker = new TimeoutTracker(parameters, DEFAULT_RETRY, DEFAULT_TIMEOUT);

        int port = ParameterMap.getKeyedInteger(parameters, "port", DEFAULT_PORT);

        // Get interface address from NetworkInterface
        //
        InetAddress ipAddr = iface.getAddress();

        final String hostAddress = InetAddressUtils.str(ipAddr);
        LOG.debug("poll: address = {}, port = {}, {}", hostAddress, port, tracker);

        PollStatus serviceStatus = PollStatus.unavailable();

        for (tracker.reset(); tracker.shouldRetry() && !serviceStatus.isAvailable(); tracker.nextAttempt()) {
            Socket socket = null;
            try {
                // create a connected socket
                //
                tracker.startAttempt();

                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddr, port), tracker.getConnectionTimeout());
                socket.setSoTimeout(tracker.getSoTimeout());

                LOG.debug("SmtpMonitor: connected to host: {} on port: {}", ipAddr, port);

                // We're connected, so upgrade status to unresponsive
                serviceStatus = PollStatus.unresponsive();
                // Forcing to check for CRLF instead of any other line terminator as per RFC specification
                CRLFLineReader rdr = new CRLFLineReader(new InputStreamReader(socket.getInputStream(), "ASCII"));

                //
                // Tokenize the Banner Line, and check the first
                // line for a valid return.
                //
                String banner = sendMessage(socket, rdr, null);
                LOG.debug("poll: banner = {}", banner);

                StringTokenizer t = new StringTokenizer(banner);
                int rc = Integer.parseInt(t.nextToken());
                if (rc == 220) {
                    //
                    // Send the HELO command
                    //
                    String cmd = "HELO " + LOCALHOST_NAME + "\r\n";
                    socket.getOutputStream().write(cmd.getBytes());

                    //
                    // get the returned string, tokenize, and
                    // verify the correct output.
                    //
                    String response = rdr.readLine();
                    double responseTime = tracker.elapsedTimeInMillis();

                    if (response == null) {
                        continue;
                    }

                    if (MULTILINE.matcher(response).find()) {
                        // Ok we have a multi-line response...first three
                        // chars of the response line are the return code.
                        // The last line of the response will start with
                        // return code followed by a space.
                        String multiLineRC = new String(response.getBytes("ASCII"), 0, 3, "ASCII");

                        // Create new regExp to look for last line
                        // of this multi line response
                        Pattern endMultiline = null;
                        try {
                            endMultiline = Pattern.compile(multiLineRC);
                        } catch (PatternSyntaxException ex) {
                            throw new java.lang.reflect.UndeclaredThrowableException(ex);
                        }

                        // read until we hit the last line of the multi-line
                        // response
                        do {
                            response = rdr.readLine();
                        } while (response != null && !endMultiline.matcher(response).find());
                        if (response == null) {
                            continue;
                        }
                    }

                    t = new StringTokenizer(response);
                    rc = Integer.parseInt(t.nextToken());
                    if (rc == 250) {
                        response = sendMessage(socket, rdr, "QUIT\r\n");

                        t = new StringTokenizer(response);
                        rc = Integer.parseInt(t.nextToken());

                        if (rc == 221) {
                            serviceStatus = PollStatus.available(responseTime);
                        }
                    }
                } else if (rc == 554) {
                    String response = sendMessage(socket, rdr, "QUIT\r\n");
                    serviceStatus = PollStatus.unavailable("Server rejecting transactions with 554");
                }

                // If we get this far and the status has not been set to
                // available, then something didn't verify during the banner
                // checking or HELO/QUIT comand process.
                if (!serviceStatus.isAvailable()) {
                    serviceStatus = PollStatus.unavailable(serviceStatus.getReason());
                }
            } catch (NumberFormatException e) {
                String reason = "NumberFormatException while polling address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } catch (NoRouteToHostException e) {
                String reason = "No route to host exception for address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
                break; // Break out of for(;;)
            } catch (InterruptedIOException e) {
                String reason = "Did not receive expected response within timeout " + tracker;
                LOG.debug(reason);
                serviceStatus = PollStatus.unavailable(reason);
            } catch (ConnectException e) {
                String reason = "Unable to connect to address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } catch (IOException e) {
                String reason = "IOException while polling address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } finally {
                try {
                    // Close the socket
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.fillInStackTrace();
                    LOG.debug("poll: Error closing socket.", e);
                }
            }
        }

        //
        // return the status of the service
        //
        return serviceStatus;
    }

    private String sendMessage(Socket socket, BufferedReader rdr, String command) throws IOException {
        if (command != null && !"".equals(command)) {
            socket.getOutputStream().write(command.getBytes("ASCII"));
        }

        //
        // get the returned string, tokenize, and
        // verify the correct output.
        //
        String response = rdr.readLine();
        if (response == null) {
            return "";
        }
        if (MULTILINE.matcher(response).find()) {
            // Ok we have a multi-line response...first three
            // chars of the response line are the return code.
            // The last line of the response will start with
            // return code followed by a space.
            String multiLineRC = new String(response.getBytes("ASCII"), 0, 3, "ASCII") + " ";

            // Create new regExp to look for last line
            // of this multi line response
            Pattern endMultiline = null;
            try {
                endMultiline = Pattern.compile(multiLineRC);
            } catch (PatternSyntaxException ex) {
                throw new java.lang.reflect.UndeclaredThrowableException(ex);
            }

            // read until we hit the last line of the multi-line
            // response
            do {
                response = rdr.readLine();
            } while (response != null && !endMultiline.matcher(response).find());
            if (response == null) {
                return "";
            }
        }
        return response;
    }

}
