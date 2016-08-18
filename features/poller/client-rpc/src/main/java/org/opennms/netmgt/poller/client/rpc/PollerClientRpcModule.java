package org.opennms.netmgt.poller.client.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.opennms.core.rpc.xml.AbstractXmlRpcModule;
import org.opennms.netmgt.poller.PollerResponse;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class PollerClientRpcModule extends AbstractXmlRpcModule<PollerRequestDTO, PollerResponseDTO> {

    public static final String RPC_MODULE_ID = "Poller";

    @Autowired
    private ServicePollerRegistry servicePollerRegistry;

    @Autowired
    @Qualifier("pollerExecutor")
    private Executor executor;

    public PollerClientRpcModule() {
        super(PollerRequestDTO.class, PollerResponseDTO.class);
    }

    public void setServicePollerRegistry(ServicePollerRegistry servicePollerRegistry) {
        this.servicePollerRegistry = servicePollerRegistry;
    }

    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    @Override
    public CompletableFuture<PollerResponseDTO> execute(PollerRequestDTO request) {
        String className = request.getClassName();

        ServiceMonitor poller = servicePollerRegistry.getMonitorByClassName(className);
        if (poller == null) {
            throw new IllegalArgumentException("No poller found with class name '" + className + "'.");
        }

        return CompletableFuture.supplyAsync(new Supplier<PollerResponseDTO>() {

            @Override
            public PollerResponseDTO get() {
                PollerResponse pollerResponse = poller.poll(request);
                return new PollerResponseDTO(pollerResponse.getPollStatus());
            }

        }, executor);

    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

}
