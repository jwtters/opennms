package org.opennms.netmgt.poller.client.rpc;

import java.util.Objects;

import org.opennms.core.rpc.api.RpcClient;
import org.opennms.core.rpc.api.RpcClientFactory;
import org.opennms.netmgt.poller.LocationAwarePollerClient;
import org.opennms.netmgt.poller.PollerRequestBuilder;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class LocationAwarePollerClientImpl implements LocationAwarePollerClient, InitializingBean {

    @Autowired
    private ServicePollerRegistry registry;

    @Autowired
    private PollerClientRpcModule pollerClientRpcModule;

    @Autowired
    private RpcClientFactory rpcClientFactory;

    private RpcClient<PollerRequestDTO, PollerResponseDTO> delegate;

    public LocationAwarePollerClientImpl() { }

    public LocationAwarePollerClientImpl(RpcClientFactory rpcClientFactory) {
        this.rpcClientFactory = Objects.requireNonNull(rpcClientFactory);
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        delegate = rpcClientFactory.getClient(pollerClientRpcModule);
    }

    protected RpcClient<PollerRequestDTO, PollerResponseDTO> getDelegate() {
        return delegate;
    }

    @Override
    public PollerRequestBuilder poll() {
        return new PollerRequestBuilderImpl(this);
    }

    public ServicePollerRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(ServicePollerRegistry registry) {
        this.registry = registry;
    }

}
