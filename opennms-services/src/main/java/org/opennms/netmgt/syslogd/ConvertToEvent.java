//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2009 The OpenNMS Group, Inc. All rights
// reserved.
// OpenNMS(R) is a derivative work, containing both original code, included
// code and modified
// code that was published under the GNU General Public License. Copyrights
// for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2003 Jan 31: Cleaned up some unused imports.
// 2009 Mar 23: Add support for discarding messages. - jeffg@opennms.org
//
// Original code base Copyright (C) 1999-2001 Oculan Corp. All rights
// reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact:
// OpenNMS Licensing <license@opennms.org>
// http://www.opennms.org/
// http://www.opennms.com/
//

package org.opennms.netmgt.syslogd;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.syslogd.HideMatch;
import org.opennms.netmgt.config.syslogd.HideMessage;
import org.opennms.netmgt.config.syslogd.ParameterAssignment;
import org.opennms.netmgt.config.syslogd.UeiList;
import org.opennms.netmgt.config.syslogd.UeiMatch;
import org.opennms.netmgt.dao.castor.CastorUtils;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;
import org.opennms.netmgt.xml.event.Logmsg;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Parms;
import org.opennms.netmgt.xml.event.Value;

/**
 * @author <a href="mailto:joed@opennms.org">Johan Edstrom</a>
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @author <a href="mailto:mhuot@opennms.org">Mike Huot</a>
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="http://www.oculan.com">Oculan Corporation </a>
 */

// This routine do the majority of the Syslogd's work
// Improvements most likely are to be made.

// This routine do the majority of the Syslogd's work
// Improvements most likely are to be made.
final class ConvertToEvent {

    /** Constant <code>HIDDEN_MESSAGE="The message logged has been removed due"{trunked}</code> */
    protected static final String HIDDEN_MESSAGE = "The message logged has been removed due to configuration of Syslogd; it may contain sensitive data.";

    /**
     * The received XML event, decoded using the US-ASCII encoding.
     */
    private String m_eventXML;

    /**
     * The decoded event document. The classes are defined in an XSD and
     * generated by castor.
     */
    private Log m_log;

    /**
     * The Internet address of the sending agent.
     */
    private InetAddress m_sender;

    /**
     * The port of the agent on the remote system.
     */
    private int m_port;

    /**
     * The list of event that have been acknowledged.
     */
    private List<Event> m_ackEvents;

    private Event m_event;

    /**
     * Private constructor to prevent the used of <em>new</em> except by the
     * <code>make</code> method.
     */
    private ConvertToEvent() {
        // constructor not supported
        // except through make method!
    }

    /**
     * Constructs a new event encapsulation instance based upon the
     * information passed to the method. The passed datagram data is decoded
     * into a string using the <tt>US-ASCII</tt> character encoding.
     *
     * @param packet The datagram received from the remote agent.
     * @throws java.io.UnsupportedEncodingException
     *          Thrown if the data buffer cannot be decoded using the
     *          US-ASCII encoding.
     * @throws MessageDiscardedException 
     */
    static ConvertToEvent make(final DatagramPacket packet, final String matchPattern, final int hostGroup, final int messageGroup, final UeiList ueiList, final HideMessage hideMessage, final String discardUei)
            throws UnsupportedEncodingException, MessageDiscardedException {
        return make(packet.getAddress(), packet.getPort(), packet.getData(), packet.getLength(), matchPattern, hostGroup, messageGroup, ueiList, hideMessage, discardUei);
    }

    /**
     * Constructs a new event encapsulation instance based upon the
     * information passed to the method. The passed byte array is decoded into
     * a string using the <tt>US-ASCII</tt> character encoding.
     *
     * @param addr The remote agent's address.
     * @param port The remote agent's port
     * @param data The XML data in US-ASCII encoding.
     * @param len  The length of the XML data in the buffer.
     * @throws java.io.UnsupportedEncodingException
     *          Thrown if the data buffer cannot be decoded using the
     *          US-ASCII encoding.
     * @throws MessageDiscardedException 
     */
    static ConvertToEvent make(final InetAddress addr, final int port, final byte[] data,
                               final int len, final String matchPattern, final int hostGroup, final int messageGroup,
                               final UeiList ueiList, final HideMessage hideMessage, final String discardUei)
            throws UnsupportedEncodingException, MessageDiscardedException {

        final ConvertToEvent e = new ConvertToEvent();

        String deZeroedData = new String(data, 0, len, "US-ASCII");
        if (deZeroedData.endsWith("\0")) {
            deZeroedData = deZeroedData.substring(0, deZeroedData.length() - 1);
        }
        
        e.m_sender = addr;
        e.m_port = port;
        e.m_eventXML = deZeroedData;
        e.m_ackEvents = new ArrayList<Event>(16);
        e.m_log = null;

        ThreadCategory.setPrefix(Syslogd.LOG4J_CATEGORY);
        ThreadCategory log = ThreadCategory.getInstance();

        if (log.isDebugEnabled())
            log.debug("In the make part of UdpReceivedSyslog " + e.toString());

        // Build a basic event out of the syslog message

        final Event event = new Event();
        event.setSource("syslogd");

        // Set nodeId
        long nodeId = SyslogdIPMgr.getNodeId(addr.getHostAddress().replaceAll("/", ""));
        if (nodeId != -1)
            event.setNodeid(nodeId);

        // Set event host
        try {
            event.setHost(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException uhE) {
            event.setHost("unresolved.host");
            log.warn("Failed to resolve local hostname", uhE);
        }

        event.setInterface(addr.getHostAddress().replaceAll("/", ""));

        event.setTime(org.opennms.netmgt.EventConstants.formatToString(new java.util.Date()));
        final Logmsg logmsg = new Logmsg();
        logmsg.setDest("logndisplay");

        String message = deZeroedData;

        int lbIdx = message.indexOf('<');
        int rbIdx = message.indexOf('>');

        if (lbIdx < 0 || rbIdx < 0 || lbIdx >= (rbIdx - 1)) {
            log.warn("Syslogd received an unparsable message!");
        }

        int priCode = 0;
        String priStr = message.substring(lbIdx + 1, rbIdx);

        try {
            priCode = Integer.parseInt(priStr);
        } catch (final NumberFormatException ex) {
            log.debug("ERROR Bad priority code '" + priStr + "'");

        }

        final int facility = SyslogDefs.extractFacility(priCode);
        final int priority = SyslogDefs.extractPriority(priCode);

        final String priorityTxt = SyslogDefs.getPriorityName(priority);
        // event.setSeverity(priorityTxt);
        // We leave the priority alone, this might need to be set.

        final String facilityTxt = SyslogDefs.getFacilityName(facility);

        //Check for UEI matching or allow a simple standard one.

        event.setUei("uei.opennms.org/syslogd/" + facilityTxt + "/" + priorityTxt);

        // message = message.substring(rbIdx + 1, (message.length() - 1));
        message = message.substring(rbIdx + 1, (message.length()));

        // Check to see if message looks non-standard.
        // In this case, it means that there is not a standard
        // date in the front of the message text.
        boolean stdMsg = true;

        if (message.length() < 16) {
            stdMsg = false;
        } else if (message.charAt(3) != ' ' || message.charAt(6) != ' '
                || message.charAt(9) != ':' || message.charAt(12) != ':'
                || message.charAt(15) != ' ') {
            stdMsg = false;
        }

        String timestamp;

        if (!stdMsg) {
            try {
                timestamp = SyslogTimeStamp.getInstance().format(new Date());
            } catch (IllegalArgumentException ex) {
                log.debug("ERROR INTERNAL DATE ERROR!");
                timestamp = "";
            }
        } else {
            timestamp = message.substring(0, 15);
            message = message.substring(16);
        }

        // These 2 debugs will aid in analyzing the regexes as syslog seems
        // to differ a lot depending on implementation or message structure.

        final boolean traceEnabled = log.isEnabledFor(ThreadCategory.Level.TRACE);

        if (traceEnabled) {
            log.trace("Message : " + message);
            log.trace("Pattern : " + matchPattern);
            log.trace("Host group: " + hostGroup);
            log.trace("Message group: " + messageGroup);
        }

        // We will also here find out if, the host needs to
        // be replaced, the message matched to a UEI, and
        // last if we need to actually hide the message.
        // this being potentially helpful in avoiding showing
        // operator a password or other data that should be
        // confidential.

        final Pattern pattern = Pattern.compile(matchPattern);
        final Matcher m = pattern.matcher(message);

        /*
        * We matched on a regexp for host/message pair.
        * This can be a forwarded message as in BSD Style
        * or syslog-ng.
        * We assume that the host is given to us
        * as an IP/Hostname and that the resolver
        * on the ONMS host actually can resolve the
        * node to match against nodeId.
         */

        if (m.matches()) {

            if (traceEnabled) {
                log.trace("Regexp matched message: " + message);
                log.trace("Host: " + m.group(hostGroup));
                log.trace("Message: " + m.group(messageGroup));
            }

            // We will try to extract an IP address from a hostname.....
            final StringBuffer sb = new StringBuffer();

            try {
                final InetAddress address = InetAddress.getByName(m.group(hostGroup));
                final byte[] ipAddr = address.getAddress();

                // Convert to dot representation
                for (int i = 0; i < ipAddr.length; i++) {
                    if (i > 0) {
                        sb.append(".");
                    }
                    sb.append(ipAddr[i] & 0xFF);
                }
            } catch (final UnknownHostException e1) {
                log.warn("Could not parse the host: " + e1);

            }

            final String myHost = sb.toString();
            if (!"".equals(myHost)) {
                nodeId = SyslogdIPMgr.getNodeId(myHost.replaceAll("/", ""));

                if (nodeId != -1) {
                    event.setNodeid(nodeId);
                }
                // Clean up for further processing....
                event.setInterface(myHost.replaceAll("/", ""));
                message = m.group(messageGroup);
                if (traceEnabled) {
                    log.trace("Regexp used to find node: " + event.getNodeid());
                }
            }
        } else {
            log.debug("Regexp not matched: " + message);            
            throw new MessageDiscardedException();
        }

        // We will need these shortly
        final Parms eventParms = new Parms();
        Parm eventParm = null;
        Value parmValue = null;
        
        Pattern msgPat;
        Matcher msgMat;

        // Time to verify UEI matching.

        final List<UeiMatch> ueiMatch = ueiList == null ? ueiList : ueiList.getUeiMatchCollection();
        if (ueiMatch == null) {
            log.warn("No ueiList configured.");
        } else {
            for (final UeiMatch uei : ueiMatch) {
                if (uei.getMatch().getType().equals("substr")) {
                    if (traceEnabled) {
                        log.trace("Attempting substring match for text of a Syslogd event to :" + uei.getMatch().getExpression());
                    }
                	if (message.contains(uei.getMatch().getExpression())) {
                	    if (discardUei.equals(uei.getUei())) {
                	        if (log.isDebugEnabled()) {
                	            log.trace("Specified UEI '" + uei.getUei() + "' is same as discard-uei, discarding this message.");
                	        }
                	        throw new MessageDiscardedException();
                	    }
                        //We can pass a new UEI on this
                	    if (traceEnabled) {
                	        log.trace("Changed the UEI of a Syslogd event, based on substring match, to :" + uei.getUei());
                	    }
                        event.setUei(uei.getUei());
                        // I think we want to stop processing here so the first
                        // ueiMatch wins, right?
                        break;
                    }
                } else if (uei.getMatch().getType().equals("regex")) {
                    if (traceEnabled) {
                        log.trace("Attempting regex match for text of a Syslogd event to :" + uei.getMatch().getExpression());
                    }
                    try {
                		msgPat = Pattern.compile(uei.getMatch().getExpression(), Pattern.MULTILINE);
                		msgMat = msgPat.matcher(message);
                    } catch(PatternSyntaxException pse) {
                		log.warn("Failed to compile regex pattern '"+uei.getMatch().getExpression()+"'", pse);
                		msgMat = null;
                	}
                	if ((msgMat != null) && (msgMat.find())) {
                        if (discardUei.equals(uei.getUei())) {
                            if (log.isDebugEnabled()) {
                                log.debug("Specified UEI '" + uei.getUei() + "' is same as discard-uei, discarding this message.");
                            }
                            throw new MessageDiscardedException();
                        }
    
                        // We matched a UEI
                        if (traceEnabled) {
                            log.trace("Changed the UEI of a Syslogd event, based on regex match, to :" + uei.getUei());
                        }
                        event.setUei(uei.getUei());
                        if (msgMat.groupCount() > 0 && uei.getMatch().isDefaultParameterMapping()) {
                            log.trace("Doing default parameter mappings for this regex match.");
                            for (int groupNum = 1; groupNum <= msgMat.groupCount(); groupNum++) {
                                if (traceEnabled) {
                                    log.trace("Added parm 'group"+groupNum+"' with value '"+msgMat.group(groupNum)+"' to Syslogd event based on regex match group");
                                }
                                eventParm = new Parm();
                                eventParm.setParmName("group"+groupNum);
                                parmValue = new Value();
                                parmValue.setContent(msgMat.group(groupNum));
                                eventParm.setValue(parmValue);
                                eventParms.addParm(eventParm);
                            }
                        }
                        if (msgMat.groupCount() > 0 && uei.getParameterAssignmentCount() > 0) {
                            log.trace("Doing user-specified parameter assignments for this regex match.");
                            for (ParameterAssignment assignment : uei.getParameterAssignmentCollection()) {
                                eventParm = new Parm();
                                eventParm.setParmName(assignment.getParameterName());
                                parmValue = new Value();
                                String vettedValue = msgMat.group(assignment.getMatchingGroup());
                                if (vettedValue == null)
                                    vettedValue = "";
                                parmValue.setContent(vettedValue);
                                eventParm.setValue(parmValue);
                                eventParms.addParm(eventParm);
                                if (traceEnabled) {
                                    log.trace("Added parm '" + eventParm.getParmName() + "' with value '" + parmValue.getContent() + "' to Syslogd event based on user-specified parameter assignment");
                                }
                            }
                        }
                        // I think we want to stop processing here so the first
                        // ueiMatch wins, right?
                		break;
                	}
                }
            }
        }

        // Time to verify if we need to hide the message
        boolean doHide = false;
        final List<HideMatch> hideMatch = hideMessage == null ? null : hideMessage.getHideMatchCollection();
        if (hideMatch == null) {
            log.warn("No hideMessage configured.");
        } else {
            for (final HideMatch hide : hideMatch) {
                if (hide.getMatch().getType().equals("substr")) {
                    if (message.contains(hide.getMatch().getExpression())) {
                        // We should hide the message based on this match
                    	doHide = true;
                    }            	
                } else if (hide.getMatch().getType().equals("regex")) {
                	try {
                    	msgPat = Pattern.compile(hide.getMatch().getExpression(), Pattern.MULTILINE);
                    	msgMat = msgPat.matcher(message);            		
                	} catch (PatternSyntaxException pse) {
                		log.warn("Failed to compile regex pattern '"+hide.getMatch().getExpression()+"'", pse);
                		msgMat = null;
                	}
                	if ((msgMat != null) && (msgMat.find())) {
                        // We should hide the message based on this match
                		doHide = true;
                	}
                }
                if (doHide) {
    	            log.debug("Hiding syslog message from Event - May contain sensitive data");
    	            message = HIDDEN_MESSAGE;
    	            // We want to stop here, no point in checking further hideMatches
    	            break;
                }
            }
        }

        lbIdx = message.indexOf('[');
        rbIdx = message.indexOf(']');
        final int colonIdx = message.indexOf(':');
        final int spaceIdx = message.indexOf(' ');

        int processId = 0;
        String processName = "";
        String processIdStr = "";

        if (lbIdx < (rbIdx - 1) && colonIdx == (rbIdx + 1) && spaceIdx == (colonIdx + 1)) {
            processName = message.substring(0, lbIdx);
            processIdStr = message.substring(lbIdx + 1, rbIdx);
            message = message.substring(colonIdx + 2);

            try {
                processId = Integer.parseInt(processIdStr);
            } catch (NumberFormatException ex) {
                log.debug("Bad process id '" + processIdStr + "'");
                processId = 0;
            }
        } else if (lbIdx < 0 && rbIdx < 0 && colonIdx > 0 && spaceIdx == (colonIdx + 1)) {
            processName = message.substring(0, colonIdx);
            message = message.substring(colonIdx + 2);
        }

        // Using parms provides configurability.
        logmsg.setContent(message);
        event.setLogmsg(logmsg);

        // Add appropriate parms
        eventParm = new Parm();
        eventParm.setParmName("syslogmessage");
        parmValue = new Value();
        parmValue.setContent((message));
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);

        eventParm = new Parm();
        eventParm.setParmName("severity");
        parmValue = new Value();
        parmValue.setContent("" + priorityTxt);
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);

        eventParm = new Parm();
        eventParm.setParmName("timestamp");
        parmValue = new Value();
        parmValue.setContent(timestamp);
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);

        eventParm = new Parm();
        eventParm.setParmName("process");
        parmValue = new Value();
        parmValue.setContent(processName);
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);

        eventParm = new Parm();
        eventParm.setParmName("service");
        parmValue = new Value();
        parmValue.setContent("" + facilityTxt);
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);

        eventParm = new Parm();
        eventParm.setParmName("processid");
        parmValue = new Value();
        parmValue.setContent("" + processId);
        eventParm.setValue(parmValue);
        eventParms.addParm(eventParm);

        // Good thing(TM)
        event.setParms(eventParms);

        e.m_event = event;
        return e;
    }

    /**
     * Decodes the XML package from the remote agent. If an error occurs or
     * the datagram had malformed XML then an exception is generated.
     *
     * @return The top-level <code>Log</code> element of the XML document.
     * @throws org.exolab.castor.xml.ValidationException
     *          Throws if the documents data does not match the defined XML
     *          Schema Definition.
     * @throws org.exolab.castor.xml.MarshalException
     *          Thrown if the XML is malformed and cannot be converted.
     */
    Log unmarshal() throws ValidationException, MarshalException {
        if (m_log == null) {
            m_log = CastorUtils.unmarshal(Log.class, new ByteArrayInputStream(this.m_eventXML.getBytes()));
        }
        return m_log;
    }

    /**
     * Adds the event to the list of events acknowledged in this event XML
     * document.
     *
     * @param e The event to acknowledge.
     */
    void ackEvent(final Event e) {
        if (!m_ackEvents.contains(e))
            m_ackEvents.add(e);
    }

    /**
     * Returns the raw XML data as a string.
     */
    String getXmlData() {
        return m_eventXML;
    }

    /**
     * Returns the sender's address.
     */
    InetAddress getSender() {
        return m_sender;
    }

    /**
     * Returns the sender's port
     */
    int getPort() {
        return m_port;
    }

    /**
     * Get the acknowledged events
     *
     * @return a {@link java.util.List} object.
     */
    public List<Event> getAckedEvents() {
        return m_ackEvents;
    }

    /**
     * <p>getEvent</p>
     *
     * @return a {@link org.opennms.netmgt.xml.event.Event} object.
     */
    public Event getEvent() {
        return m_event;
    }

    /**
     * {@inheritDoc}
     *
     * Returns true if the instance matches the object based upon the remote
     * agent's address &amp; port. If the passed instance is from the same
     * agent then it is considered equal.
     */
    public boolean equals(final Object o) {
        if (o != null && o instanceof ConvertToEvent) {
            final ConvertToEvent e = (ConvertToEvent) o;
            return (this == e || (m_port == e.m_port && m_sender.equals(e.m_sender)));
        }
        return false;
    }

    /**
     * Returns the hash code of the instance. The hash code is computed by
     * taking the bitwise XOR of the port and the agent's Internet address
     * hash code.
     *
     * @return The 32-bit has code for the instance.
     */
    public int hashCode() {
        return (m_port ^ m_sender.hashCode());
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        return new ToStringBuilder(this)
            .append("Sender", m_sender)
            .append("Port", m_port)
            .append("Acknowledged Events", m_ackEvents)
            .append("Event", m_event)
            .toString();
    }
}
