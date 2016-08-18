package org.opennms.netmgt.poller;

import java.net.InetAddress;
import java.util.Map;

public interface PollerRequest {
    
    /**
     * @return the address of the host against with the monitor should be invoked.
     */
    InetAddress getAddress();

    /**
     * @return additional attributes stored outside of the detector's configuration that
     * may be required when running the detector.
     */
    Map<String, String> getRuntimeAttributes();

    /**
     * @return additional attributes stored outside of the detector's configuration that
     * may be required when running the detector.
     */
    Map<String, String> getAttributeMap();
    
    String getClassName();
}
