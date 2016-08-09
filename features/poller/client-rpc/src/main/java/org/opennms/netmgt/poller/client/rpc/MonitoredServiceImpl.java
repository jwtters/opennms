package org.opennms.netmgt.poller.client.rpc;

import java.net.InetAddress;

import org.opennms.netmgt.poller.InetNetworkInterface;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.NetworkInterface;

//Copy of SimpleMonitoredService 

public class MonitoredServiceImpl implements MonitoredService{
    
    /** The IP address. */
    private InetAddress ipAddress;
    
    /** The node id. */
    private int nodeId;
    
    /** The node label. */
    private String nodeLabel;
    
    /** The service name. */
    private String svcName;
    
    
    
    public MonitoredServiceImpl(InetAddress ipAddress, String svcName) {
        super();
        this.ipAddress = ipAddress;
        this.svcName = svcName;
    }

    public MonitoredServiceImpl(InetAddress ipAddress, int nodeId, String nodeLabel, String svcName) {
        this.ipAddress = ipAddress;
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.svcName = svcName;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getSvcUrl()
     */
    public String getSvcUrl() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getSvcName()
     */
    public String getSvcName() {
        return svcName;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getIpAddr()
     */
    public String getIpAddr() {
        return ipAddress.getHostAddress();
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getNodeId()
     */
    public int getNodeId() {
        return nodeId;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getNodeLabel()
     */
    public String getNodeLabel() {
        return nodeLabel;
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getNetInterface()
     */
    public NetworkInterface<InetAddress> getNetInterface() {
        return new InetNetworkInterface(getAddress());
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.poller.MonitoredService#getAddress()
     */
    public InetAddress getAddress() {
        return ipAddress;
    }

}
