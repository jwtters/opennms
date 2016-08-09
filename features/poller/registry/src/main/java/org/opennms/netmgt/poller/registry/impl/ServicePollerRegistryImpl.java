/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.poller.registry.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.registry.api.ServicePollerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class ServicePollerRegistryImpl implements ServicePollerRegistry, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePollerRegistryImpl.class);

    @Autowired(required = false)
    Set<ServiceMonitor> m_serviceMonitors;

    private final Map<String, ServiceMonitor> m_monitorsByClassName = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.debug("afterPropertiesSet called , registering monitors");
        if (m_serviceMonitors != null) {
            for (ServiceMonitor serviceMonitor : m_serviceMonitors) {
                Map<String, String> props = new HashMap<>();
                onBind(serviceMonitor, props);
            }
        }
        LOG.debug("Registered ServiceMonitors classes are: {}",  getClassNames());
    }

    @SuppressWarnings({ "rawtypes" })
    public synchronized void onBind(ServiceMonitor serviceMonitor, Map properties) {
        LOG.debug("bind called with {}: {}", serviceMonitor, properties);
        if (serviceMonitor != null) {
            final String className = serviceMonitor.getClass().getCanonicalName();
            m_monitorsByClassName.put(className, serviceMonitor);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public synchronized void onUnbind(ServiceMonitor serviceMonitor, Map properties) {
        LOG.debug("Unbind called with {}: {}", serviceMonitor, properties);
        if (serviceMonitor != null) {
            final String className = serviceMonitor.getClass().getCanonicalName();
            m_monitorsByClassName.remove(className, serviceMonitor);
        }
    }

    @Override
    public ServiceMonitor getMonitorByClassName(String className) {
        return m_monitorsByClassName.get(className);
    }

    @Override
    public Set<String> getClassNames() {
        return Collections.unmodifiableSet(m_monitorsByClassName.keySet());
    }

}
