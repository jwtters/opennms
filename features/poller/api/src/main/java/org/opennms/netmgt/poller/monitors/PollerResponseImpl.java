package org.opennms.netmgt.poller.monitors;

import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerResponse;

public class PollerResponseImpl implements PollerResponse {
    
    private PollStatus m_pollStatus;
    
    public PollerResponseImpl(PollStatus pollStatus) {
        m_pollStatus = pollStatus;
    }

    @Override
    public PollStatus getPollStatus() {
        return m_pollStatus;
    }

}
