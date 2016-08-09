package org.opennms.netmgt.poller.client.rpc;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollerRequestBuilderImpl implements PollerRequestBuilder{

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
    public PollerRequestBuilder withServiceName(String serviceName) {
        this.serviceName = serviceName;
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
    public CompletableFuture<PollStatus> execute() {
        if (address == null) {
            throw new IllegalArgumentException("Address is required.");
        } else if (className == null) {
            throw new IllegalArgumentException("Poller class name is required.");
        }
        
        final PollerRequestDTO dto = new PollerRequestDTO();
        dto.setAddress(address);
        dto.setClassName(className);
        dto.setLocation(location);
        dto.addPollerAttributes(attributes);
        dto.setServiceName(serviceName);
        
        // Execute the request
        return client.getDelegate().execute(dto).thenApply(results -> {
            if (results.getStatusCode() == PollStatus.SERVICE_AVAILABLE) {
                LOG.info(" {} is available", serviceName);
            }else {
                throw new RuntimeException(results.getReason());
            }
            return results;
        });
    }

}
