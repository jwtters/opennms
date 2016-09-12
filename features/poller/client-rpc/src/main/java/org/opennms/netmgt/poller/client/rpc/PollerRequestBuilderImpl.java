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

import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerConfigLoader;
import org.opennms.netmgt.poller.PollerRequestBuilder;
import org.opennms.netmgt.poller.PollerResponse;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.ServiceMonitorAdaptor;
import org.opennms.netmgt.poller.monitors.AbstractServiceMonitor;

public class PollerRequestBuilderImpl implements PollerRequestBuilder {

    private MonitoredService service;

    private Integer nodeId;

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
    public PollerRequestBuilder withService(MonitoredService service) {
        this.service = service;
        if (service != null) {
            this.nodeId = service.getNodeId();
            this.location = service.getNodeLocation();
            this.address = service.getAddress();
            this.serviceName = service.getSvcName();
        }
        return this;
    }

    @Override
    public PollerRequestBuilder withNodeId(Integer nodeId) {
        this.nodeId = nodeId;
        return this;
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
    public PollerRequestBuilder withMonitorClassName(String className) {
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
        dto.setLocation(serviceMonitor.getEffectiveLocation(location));
        dto.addPollerAttributes(attributes);
        dto.setServiceName(serviceName);

        final PollerConfigLoader configLoader = serviceMonitor.getConfigLoader();
        if (configLoader != null) {
            final Integer port = AbstractServiceMonitor.getKeyedInteger(attributes, PORT, null);
            dto.addRuntimeAttributes(configLoader.getRuntimeAttributes(nodeId, location, address, port, attributes, service));
        }

        // Execute the request
        return client.getDelegate().execute(dto).thenApply(results -> {
            PollStatus pollStatus = results.getPollStatus();
            for (ServiceMonitorAdaptor adaptor : adaptors) {
                pollStatus = adaptor.handlePollResult(service, attributes, pollStatus);
            }
            results.setPollStatus(pollStatus);
            return results;
        });
    }

}
