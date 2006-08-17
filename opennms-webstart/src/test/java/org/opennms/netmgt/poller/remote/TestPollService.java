/**
 * 
 */
package org.opennms.netmgt.poller.remote;

import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.PollStatus;

class TestPollService implements PollService {
	
	private int m_repetitions;
	private int m_pollCount;
	private PollStatus m_currentStatus;

	public TestPollService(int repetitions, PollStatus initialStatus) {
		m_repetitions = repetitions;
		m_currentStatus = initialStatus;
	}
	
	public TestPollService(int repetitions) {
		this(repetitions, PollStatus.up());
	}
	
	public TestPollService() {
		this(2);
	}

	public PollStatus poll(OnmsMonitoredService monitoredService) {
		PollStatus status = m_currentStatus;
		
		m_pollCount++;
		if (m_pollCount % m_repetitions == 0) {
			m_currentStatus = (m_currentStatus.isDown() ? PollStatus.up(100+m_pollCount) : PollStatus.down("pollCount is "+m_pollCount));
		}
		
		return status;
		
	}
	
}