package org.opennms.netmgt.poller.client.rpc;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.core.rpc.xml.AbstractXmlRpcModule;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;
import org.springframework.beans.factory.annotation.Autowired;


public class PollerClientRpcModule extends AbstractXmlRpcModule<PollerRequestDTO, PollStatus> {
    
    public static final String RPC_MODULE_ID = "Poller";

    @Autowired
    private ServicePollerRegistry servicePollerRegistry;


    public PollerClientRpcModule() {
        super(PollerRequestDTO.class, PollStatus.class);
    }

    public void setServicePollerRegistry(ServicePollerRegistry servicePollerRegistry) {
        this.servicePollerRegistry = servicePollerRegistry;
    }


    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    @Override
    public CompletableFuture<PollStatus> execute(PollerRequestDTO request) {
        String className = request.getClassName();
        String serviceName = request.getServiceName();
        Map<String, String> attributeMap = request.getAttributeMap();
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(attributeMap);
        InetAddress address = request.getAddress();
        ServiceMonitor poller = servicePollerRegistry.getMonitorByClassName(className);
        MonitoredServiceImpl svc = new MonitoredServiceImpl(address, serviceName);
        PollStatus pollstatus = poller.poll(svc, attributes);
        final CompletableFuture<PollStatus> future = new CompletableFuture<>();
        future.complete(pollstatus);
        return future;
    }

}
