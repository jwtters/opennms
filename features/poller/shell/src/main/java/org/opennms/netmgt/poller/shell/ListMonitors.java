package org.opennms.netmgt.poller.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;

@Command(scope = "poller", name = "list-monitors", description = "Lists all of the available monitors ")
public class ListMonitors extends OsgiCommandSupport{
    
    private ServicePollerRegistry registry;

    @Override
    protected Object doExecute() throws Exception {
        registry.getClassNames().stream().forEachOrdered(e -> {
            System.out.printf("%s\n", e);
        });
        return null;
    }

    public void setRegistry(ServicePollerRegistry registry) {
        this.registry = registry;
    }

}
