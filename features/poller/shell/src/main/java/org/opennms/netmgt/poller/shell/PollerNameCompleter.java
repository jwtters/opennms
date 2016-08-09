package org.opennms.netmgt.poller.shell;

import java.util.List;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;



public class PollerNameCompleter implements Completer{

    private ServicePollerRegistry servicePollerRegistry;
    
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter serviceNames = new StringsCompleter();
        serviceNames.getStrings().addAll(servicePollerRegistry.getClassNames());
        return serviceNames.complete(buffer, cursor, candidates);
    }

    public void setServicePollerRegistry(ServicePollerRegistry servicePollerRegistry) {
        this.servicePollerRegistry = servicePollerRegistry;
    }

}
