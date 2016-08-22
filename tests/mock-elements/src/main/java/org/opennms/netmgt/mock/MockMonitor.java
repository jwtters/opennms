/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2004-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.mock;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.opennms.netmgt.poller.Distributable;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerConfigLoader;
import org.opennms.netmgt.poller.PollerRequest;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.monitors.AbstractServiceMonitor;
import org.opennms.netmgt.snmp.InetAddrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Mark this as {@link Distributable} so that we can reuse it for the remote poller tests.
 */
@Distributable
public class MockMonitor extends AbstractServiceMonitor {

	private static final Logger LOG = LoggerFactory.getLogger(MockMonitor.class);

    private MockNetwork m_network;

    private String m_svcName;

    /**
     * Simple constructor so that the MockMonitor can be used as a placeholder {@link ServiceMonitor}
     * inside config files.
     */
    public MockMonitor() {
        m_network = new MockNetwork(); // So that this can be use in synchronized() statements
    }

    /**
     * @param network
     * @param svcName
     */
    public MockMonitor(MockNetwork network, String svcName) {
        m_network = network;
        m_svcName = svcName;
    }

    @Override
    public PollStatus invokePoll(PollerRequest request) {
        final String ipAddr = InetAddrUtils.str(request.getAddress());
        if ("invalid".equals(request.getRuntimeAttributes().get("status"))) {
            LOG.info("Invalid Poll: {}/{}", ipAddr, m_svcName);
            m_network.receivedInvalidPoll(ipAddr, m_svcName);
            return PollStatus.unknown("Mock.");
        } else {
            int statusCode = Integer.valueOf(request.getRuntimeAttributes().get("status"));
            String reason = request.getRuntimeAttributes().get("reason");
            return PollStatus.get(statusCode, reason);
        }
    }

    @Override
    public PollStatus poll(MonitoredService monSvc, Map<String, Object> parameters) {
        throw new RuntimeException("Should never be called.");
    }

    @Override
    public PollerConfigLoader getConfigLoader() {
        // JW: TODO: Clean this up!
        return new PollerConfigLoader() {
            @Override
            public Map<String, String> getRuntimeAttributes(Integer nodeId, String location, InetAddress address, Integer port) {
                Map<String, String> attributes = new HashMap<>();
                synchronized(m_network) {
                    final String ipAddr = InetAddrUtils.str(address);
                    MockService svc = m_network.getService(nodeId, ipAddr, m_svcName);
                    if (svc == null) {
                        LOG.info("Invalid Poll: {}/{}", ipAddr, m_svcName);
                        m_network.receivedInvalidPoll(ipAddr, m_svcName);
                        attributes.put("status", "invalid");
                    } else {
                        LOG.info("Poll: [{}/{}/{}]", svc.getInterface().getNode().getLabel(), ipAddr, m_svcName);
                        PollStatus pollStatus = svc.poll();
                        attributes.put("status", Integer.toString(pollStatus.getStatusCode()));
                        attributes.put("reason", pollStatus.getReason());
                    }
                }
                return attributes;
            }
        };
    }
}
