package org.opennms.netmgt.poller.monitors;

import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import org.opennms.core.spring.BeanUtils;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.core.utils.PropertiesUtils;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.snmp.InetAddrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opennms.netmgt.poller.monitors.CiscoPingMibMonitor.*;

public class CiscoPingMibConfigLoader extends SnmpConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CiscoPingMibConfigLoader.class);

    private NodeDao nodeDao;

    private InetAddress determineProxyAddress(Map<String, Object> parameters, MonitoredService svc) {

        LOG.debug("Determining the proxy address on which to set up the ciscoPingEntry for target interface {}",
                svc.getAddress());
        OnmsNode proxyNode = null;
        InetAddress proxyAddress = null;
        String proxyNodeId = ParameterMap.getKeyedString(parameters, PARM_PROXY_NODE_ID, null);
        String proxyNodeFS = ParameterMap.getKeyedString(parameters, PARM_PROXY_FOREIGN_SOURCE, null);
        String proxyNodeFI = ParameterMap.getKeyedString(parameters, PARM_PROXY_FOREIGN_ID, null);

        String rawProxyIpAddr = ParameterMap.getKeyedString(parameters, PARM_PROXY_IP_ADDR, null);
        String proxyIpAddr = rawProxyIpAddr;
        if (rawProxyIpAddr != null) {
            proxyIpAddr = PropertiesUtils.substitute(rawProxyIpAddr, getServiceProperties(svc));
            LOG.debug("Expanded value '{}' of parameter {} to '{}' for service {} on interface {}", rawProxyIpAddr,
                    PARM_PROXY_IP_ADDR, proxyIpAddr, svc.getSvcName(), svc.getAddress());
        }

        /*
         * If we have a foreign-source and foreign-id, short circuit to use that
         */
        if (proxyNodeFS != null && !proxyNodeFS.equals("") && proxyNodeFI != null && !proxyNodeFI.equals("")) {
            LOG.debug("Trying to look up proxy node with foreign-source {}, foreign-id {} for target interface {}",
                    proxyNodeFS, proxyNodeFI, svc.getAddress());
            proxyNode = nodeDao.findByForeignId(proxyNodeFS, proxyNodeFI);
            LOG.debug("Found a node via foreign-source / foreign-id '{}'/'{}' to use as proxy", proxyNodeFS,
                    proxyNodeFI);
            if (proxyNode != null && proxyNode.getPrimaryInterface() != null)
                proxyAddress = proxyNode.getPrimaryInterface().getIpAddress();
        }
        if (proxyAddress != null) {
            LOG.debug("Using address {} from node '{}':'{}' as proxy for service '{}' on interface {}", proxyAddress,
                    proxyNodeFS, proxyNodeFI, svc.getSvcName(), svc.getIpAddr());
            return proxyAddress;
        }

        /* No match with foreign-source / foreign-id? Try with a node ID */
        if (proxyNodeId != null && Integer.valueOf(proxyNodeId) != null) {
            LOG.debug("Trying to look up proxy node with database ID {} for target interface {}", proxyNodeId,
                    svc.getAddress());
            proxyNode = nodeDao.get(Integer.valueOf(proxyNodeId));
            if (proxyNode != null && proxyNode.getPrimaryInterface() != null)
                proxyAddress = proxyNode.getPrimaryInterface().getIpAddress();
        }
        if (proxyAddress != null) {
            LOG.debug("Using address {} from node with DBID {} as proxy for service '{}' on interface {}", proxyAddress,
                    proxyNodeId, svc.getSvcName(), svc.getIpAddr());
            return proxyAddress;
        }

        /* No match with any node criteria? Try for a plain old IP address. */
        LOG.info("Trying to use address {} as proxy-ping agent address for target interface {}", proxyIpAddr,
                svc.getAddress());
        try {
            if (!"".equals(proxyIpAddr)) {
                proxyAddress = InetAddressUtils.addr(proxyIpAddr);
            }
        } catch (final IllegalArgumentException e) {
        }
        if (proxyAddress != null) {
            LOG.debug("Using address {} (user-specified) as proxy for service '{}' on interface {}", proxyAddress,
                    svc.getSvcName(), svc.getIpAddr());
            return proxyAddress;
        }

        LOG.info(
                "Unable to determine proxy address for service '{}' on interface '{}'. The poll will be unable to proceed.",
                svc.getSvcName(), svc.getIpAddr());
        return null;
    }

    @Override
    public Map<String, String> getRuntimeAttributes(MonitoredService svc, Map<String, Object> parameters) {

        Map<String, String> runtimeAttributes = super.getRuntimeAttributes(svc, parameters);
        if (nodeDao == null) {
            nodeDao = BeanUtils.getBean("daoContext", "nodeDao", NodeDao.class);
        }
        if (svc == null) {
            throw new IllegalArgumentException("Valid MonitoredService should be passed ");
        }

        InetAddress proxyIpAddr = determineProxyAddress(parameters, svc);
        runtimeAttributes.put(PROXY_IP_ADDR, InetAddrUtils.str(proxyIpAddr));
        return runtimeAttributes;
    }

    private Properties getServiceProperties(MonitoredService svc) {
        Properties properties = new Properties();
        properties.setProperty("ipaddr", svc.getIpAddr());
        properties.setProperty("nodeid", new Integer(svc.getNodeId()).toString());
        properties.setProperty("nodelabel", svc.getNodeLabel());
        properties.setProperty("svcname", svc.getSvcName());
        return properties;
    }

}
