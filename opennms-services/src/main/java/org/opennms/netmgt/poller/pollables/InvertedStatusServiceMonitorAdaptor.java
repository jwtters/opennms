package org.opennms.netmgt.poller.pollables;

import java.util.Map;

import org.opennms.core.utils.ParameterMap;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitorAdaptor;

public class InvertedStatusServiceMonitorAdaptor implements ServiceMonitorAdaptor {

    @Override
    public PollStatus handlePollResult(MonitoredService svc, Map<String, Object> parameters, PollStatus status) {
        if ("true".equals(ParameterMap.getKeyedString(parameters, "invert-status", "false"))) {
            if (status.isAvailable()) {
                return PollStatus.unavailable("This is an inverted service and the underlying service has started responding");
            } else {
                return PollStatus.available();
            }
        }
        return status;
    }
}
