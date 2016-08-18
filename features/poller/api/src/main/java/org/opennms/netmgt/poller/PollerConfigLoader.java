package org.opennms.netmgt.poller;

import java.net.InetAddress;
import java.util.Map;

public interface PollerConfigLoader {
    
    public Map<String, String> getRuntimeAttributes(String location, InetAddress address);
    
    public String getPollerClassName();

}
