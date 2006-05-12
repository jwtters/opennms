package org.opennms.netmgt.collectd;

import java.io.File;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;

import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsIpInterface.CollectionType;
import org.opennms.netmgt.poller.IPv4NetworkInterface;
import org.opennms.netmgt.snmp.SnmpAgentConfig;

public class CollectionAgent extends IPv4NetworkInterface {

    // the interface of the Agent
	private OnmsIpInterface m_iface;
    
     // miscellaneous junk?
	private CollectionSet m_collectionSet;
    private int m_maxVarsPerPdu = 0;
    private int m_ifCount = -1;

	public CollectionAgent(OnmsIpInterface iface) {
		super(iface.getInetAddress());
		m_iface = iface;
	}
	
	public OnmsIpInterface getIpInterface() {
		return m_iface;
	}
	
	public OnmsNode getNode() {
		return m_iface.getNode();
	}
    
	InetAddress getInetAddress() {
	
		if (getType() != CollectionAgent.TYPE_IPV4)
			throw new RuntimeException("Unsupported interface type, "
					+ "only TYPE_IPV4 currently supported");
	
		return (InetAddress) getAddress();
	}

    public void setMaxVarsPerPdu(int maxVarsPerPdu) {
        m_maxVarsPerPdu = maxVarsPerPdu;
		if (log().isDebugEnabled()) {
			log().debug("maxVarsPerPdu=" + maxVarsPerPdu);
		}
	}

	public String getHostAddress() {
		return getInetAddress().getHostAddress();
	}

	public void setSavedIfCount(int ifCount) {
        m_ifCount = ifCount;
	}

	int getSavedIfCount() {
        return m_ifCount;
	}

	int getNodeId() {
		return getIpInterface().getNode().getId() == null ? -1 : getIpInterface().getNode().getId().intValue();
	}

	int getIfIndex() {
		return (getIpInterface().getIfIndex() == null ? -1 : getIpInterface().getIfIndex().intValue());
	}

	public String getSysObjectId() {
		return getIpInterface().getNode().getSysObjectId();
	}

	private CollectionType getCollectionType() {
		return getIpInterface().getIsSnmpPrimary();
	}

    // TODO: I probably need to do this a little differently so I can remove
    // the getCollectionSet reference
    public int getMaxVarsPerPdu() {
        
        if (m_maxVarsPerPdu < 1) {
            m_maxVarsPerPdu = getCollectionSet().getMaxVarsPerPdu();
            log().info("using maxVarsPerPdu from dataCollectionConfig");
        }
        
        if (m_maxVarsPerPdu < 1) {
            SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(getInetAddress());
            m_maxVarsPerPdu = agentConfig.getMaxVarsPerPdu();
            log().info("using maxVarsPerPdu from snmpconfig");
        }
        
        if (m_maxVarsPerPdu < 1) {
            log().warn("MaxVarsPerPdu CANNOT BE LESS THAN 1.  Using 10");
            return 10;
        }

        return m_maxVarsPerPdu;
    }

    private void logCompletion() {
    	
    	if (log().isDebugEnabled()) {
    		log().debug(
    				"initialize: initialization completed: nodeid = " + getNodeId()
    				+ ", address = " + getHostAddress()
    				+ ", primaryIfIndex = " + getIfIndex()
    				+ ", isSnmpPrimary = " + getCollectionType()
    				+ ", sysoid = " + getSysObjectId()
    				);
    	}
    
    }

    private void validateSysObjId() {
    	if (getSysObjectId() == null) {
    		throw new RuntimeException("System Object ID for interface "
    				+ getHostAddress()
    				+ " does not exist in the database.");
    	}
    }

    private void logCollectionParms() {
    	if (log().isDebugEnabled()) {
    		log().debug(
    				"initialize: db retrieval info: nodeid = " + getNodeId()
    				+ ", address = " + getHostAddress()
    				+ ", primaryIfIndex = " + getIfIndex()
    				+ ", isSnmpPrimary = " + getCollectionType()
    				+ ", sysoid = " + getSysObjectId()
    				);
    	}
    }

    private void validateIsSnmpPrimary() {
    	if (!CollectionType.PRIMARY.equals(getCollectionType())) {
    		throw new RuntimeException("Interface "
    				+ getHostAddress()
    				+ " is not the primary SNMP interface for nodeid "
    				+ getNodeId());
    	}
    }

    private void validatePrimaryIfIndex() {
    	if (getIfIndex() == -1) {
    		// allow this for nodes without ipAddrTables
    		// throw new RuntimeException("Unable to retrieve ifIndex for
    		// interface " + ipAddr.getHostAddress());
    		if (log().isDebugEnabled()) {
    			log().debug(
    					"initialize: db retrieval info: node " + getNodeId()
    					+ " does not have a legitimate "
    					+ "primaryIfIndex.  Assume node does not "
    					+ "supply ipAddrTable and continue...");
    		}
    	}
    }

    private void validateAgent() {
        logCollectionParms();
        validateIsSnmpPrimary();
        validatePrimaryIfIndex();
        validateSysObjId();
        getCollectionSet().verifyCollectionIsNecessary();
        logCompletion();
    }

    public void initialize(OnmsSnmpCollection snmpCollection) {
        setCollectionSet(snmpCollection.createCollectionSet(this));
    	validateAgent();
    }
    
    public String toString() {
        return getHostAddress();
    }

    public SnmpAgentConfig getAgentConfig() {
        SnmpAgentConfig agentConfig = SnmpPeerFactory.getInstance().getAgentConfig(getInetAddress());
        agentConfig.setMaxVarsPerPdu(getMaxVarsPerPdu());
        return agentConfig;
    }

    void logIfCounts(CollectionSet collectionSet) {
        log().debug("collect: nodeId: " + getNodeId()
        				+ " interface: " + getHostAddress()
        				+ " ifCount: " + collectionSet.getIfNumber().getIfNumber() 
                       + " savedIfCount: " + getSavedIfCount());
    }

    boolean ifCountHasChanged() {
        return (getSavedIfCount() != -1) && (m_collectionSet.getIfNumber().getIfNumber() != getSavedIfCount());
    }

    private void setCollectionSet(CollectionSet collectionSet) {
        m_collectionSet = collectionSet;
    }

    CollectionSet getCollectionSet() {
        return m_collectionSet;
    }

    public void triggerRescan() {
        m_collectionSet.triggerRescan();       
    }


}
