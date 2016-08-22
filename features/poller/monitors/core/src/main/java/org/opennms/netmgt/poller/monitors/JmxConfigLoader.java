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

package org.opennms.netmgt.poller.monitors;

import java.net.InetAddress;
import java.util.Map;

import org.opennms.core.spring.BeanUtils;
import org.opennms.netmgt.dao.jmx.JmxConfigDao;
import org.opennms.netmgt.poller.PollerConfigLoader;
import org.opennms.netmgt.snmp.InetAddrUtils;

public class JmxConfigLoader implements PollerConfigLoader {

    protected JmxConfigDao m_jmxConfigDao;

    @Override
    public Map<String, String> getRuntimeAttributes(String location, InetAddress address, Integer port) {
        
        if (m_jmxConfigDao == null) {
            m_jmxConfigDao = BeanUtils.getBean("daoContext", "jmxConfigDao", JmxConfigDao.class);
        }
        if (port == null) {
            throw new IllegalArgumentException("Need to specify port number in the form of port=number for Jsr160Monitor");
        }
        return m_jmxConfigDao.getConfig().lookupMBeanServer(InetAddrUtils.str(address), port.toString()).getParameterMap();

    }

}
