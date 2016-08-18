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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerConfigLoader;
import org.opennms.netmgt.poller.PollerRequestBuilder;
import org.opennms.netmgt.poller.PollerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollerRequestBuilderImpl implements PollerRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PollerRequestBuilderImpl.class);

    private String location;

    private String serviceName;

    private String className;

    private InetAddress address;

    private LocationAwarePollerClientImpl client;

    private Map<String, String> attributes = new HashMap<>();

    public PollerRequestBuilderImpl(LocationAwarePollerClientImpl client) {
        this.client = client;
    }

    @Override
    public PollerRequestBuilder withLocation(String location) {
        this.location = location;
        return this;
    }

    @Override
    public PollerRequestBuilder withClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public PollerRequestBuilder withAddress(InetAddress address) {
        this.address = address;
        return this;
    }

    @Override
    public PollerRequestBuilder withAttribute(String key, String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PollerRequestBuilder withAttributes(Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    @Override
    public CompletableFuture<PollerResponse> execute() {
        if (address == null) {
            throw new IllegalArgumentException("Address is required.");
        } else if (className == null) {
            throw new IllegalArgumentException("Poller class name is required.");
        }
        PollerConfigLoader configLoader = client.getRegistry().getConfigLoaderByMonitorClassName(className);

        final PollerRequestDTO dto = new PollerRequestDTO();
        dto.setAddress(address);
        dto.setClassName(className);
        dto.setLocation(location);
        dto.addPollerAttributes(attributes);
        dto.setServiceName(serviceName);
        if (configLoader != null) {
            dto.addRuntimeAttributes(configLoader.getRuntimeAttributes(location, address));
        }

        // Execute the request
        return client.getDelegate().execute(dto).thenApply(results -> {
            if (results.getPollStatus().getStatusCode() == PollStatus.SERVICE_AVAILABLE) {
                LOG.info(" {} is available", serviceName);
            } else {
                throw new RuntimeException(results.getPollStatus().getReason());
            }
            return results;
        });
    }

}
