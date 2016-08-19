package org.opennms.netmgt.poller;

import java.util.Map;

public interface ServiceMonitorAdaptor {

    public PollStatus handlePollResult(MonitoredService svc, Map<String, Object> parameters, PollStatus status);

}
