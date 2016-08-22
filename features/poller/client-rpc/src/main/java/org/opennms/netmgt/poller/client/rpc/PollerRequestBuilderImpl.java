/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.poller.client.rpc;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerConfigLoader;
import org.opennms.netmgt.poller.PollerRequestBuilder;
import org.opennms.netmgt.poller.PollerResponse;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.ServiceMonitorAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollerRequestBuilderImpl implements PollerRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PollerRequestBuilderImpl.class);

    private String location;

    private String serviceName;

    private ServiceMonitor serviceMonitor;

    private InetAddress address;

    private LocationAwarePollerClientImpl client;

    private static final String PORT = "port";

    private final Map<String, Object> attributes = new HashMap<>();

    private final List<ServiceMonitorAdaptor> adaptors = new LinkedList<>();

    public PollerRequestBuilderImpl(LocationAwarePollerClientImpl client) {
        this.client = client;
    }

    @Override
    public PollerRequestBuilder withLocation(String location) {
        this.location = location;
        return this;
    }

    @Override
    public PollerRequestBuilder withMonitor(ServiceMonitor serviceMonitor) {
        this.serviceMonitor = serviceMonitor;
        return this;
    }

    @Override
    public PollerRequestBuilder withClassName(String className) {
        this.serviceMonitor = client.getRegistry().getMonitorByClassName(className);
        return this;
    }

    @Override
    public PollerRequestBuilder withAddress(InetAddress address) {
        this.address = address;
        return this;
    }

    @Override
    public PollerRequestBuilder withAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    @Override
    public PollerRequestBuilder withAttributes(Map<String, Object> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    @Override
    public PollerRequestBuilder withAdaptor(ServiceMonitorAdaptor adaptor) {
        adaptors.add(adaptor);
        return this;
    }

    @Override
    public PollerRequestBuilder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    public CompletableFuture<PollerResponse> execute() {
        if (address == null) {
            throw new IllegalArgumentException("Address is required.");
        } else if (serviceMonitor == null) {
            throw new IllegalArgumentException("Monitor or monitor class name is required.");
        }

        final PollerRequestDTO dto = new PollerRequestDTO();
        dto.setAddress(address);
        dto.setClassName(serviceMonitor.getClass().getCanonicalName());
        dto.setLocation(location);
        dto.addPollerAttributes(attributes);
        dto.setServiceName(serviceName);

        // Attempt to extract the port from the list of attributes
        Integer port = null;
        final Object portObj = attributes.get(PORT);
        if (portObj != null) {
            if (portObj instanceof String) {
                final String portString = (String)portObj;
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException nfe) {
                    LOG.warn("Failed to parse port as integer from: ", portString);
                }
            } else {
                LOG.warn("Port attribute is not a string.");
            }
        }

        final PollerConfigLoader configLoader = serviceMonitor.getConfigLoader();
        if (configLoader != null) {
            dto.addRuntimeAttributes(configLoader.getRuntimeAttributes(location, address, port));
        }

        // Execute the request
        return client.getDelegate().execute(dto).thenApply(results -> {
            PollStatus pollStatus = results.getPollStatus();
            for (ServiceMonitorAdaptor adaptor : adaptors) {
                // JW: TODO: FIXME: Pass the appropriate parms.
                pollStatus = adaptor.handlePollResult(null, null, pollStatus);
            }
            results.setPollStatus(pollStatus);
            return results;
        });
    }

}
