package org.opennms.netmgt.poller.monitors;

import java.net.InetAddress;
import java.util.Map;

import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.poller.PollerConfigLoader;
import org.opennms.netmgt.poller.monitors.snmp.SnmpMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SnmpConfigLoader implements PollerConfigLoader {

    @Autowired
    @Qualifier("snmpPeerFactory")
    private SnmpAgentConfigFactory m_agentConfigFactory;

    public void setAgentConfigFactory(SnmpAgentConfigFactory agentConfigFactory) {
        this.m_agentConfigFactory = agentConfigFactory;
    }

    @Override
    public Map<String, String> getRuntimeAttributes(String location, InetAddress address) {
        if (m_agentConfigFactory == null) {
            throw new IllegalStateException("Cannot determine agent configuration without a SnmpAgentConfigFactory.");
        }
        return m_agentConfigFactory.getAgentConfig(address).toMap();
    }

    @Override
    public String getPollerClassName() {
        return SnmpMonitor.class.getCanonicalName();
    }

}
