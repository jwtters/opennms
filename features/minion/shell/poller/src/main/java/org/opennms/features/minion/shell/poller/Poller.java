package org.opennms.features.minion.shell.poller;


import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;

@Command(scope = "poller", name = "list-monitors", description = "Lists all of the available monitors.")
@Service
public class Poller implements Action {

    @Reference
    ServicePollerRegistry servicePollerRegistry;

    @Override
    public Object execute() throws Exception {

        servicePollerRegistry.getClassNames().stream().forEachOrdered(e -> {
            System.out.printf(" %s", e);
        });
        return null;
    }

}
