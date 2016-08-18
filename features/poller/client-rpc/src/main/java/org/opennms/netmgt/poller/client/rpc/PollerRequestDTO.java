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

package org.opennms.netmgt.poller.client.rpc;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.core.network.InetAddressXmlAdapter;
import org.opennms.core.rpc.api.RpcRequest;
import org.opennms.netmgt.poller.PollerRequest;

@XmlRootElement(name = "poller-request")
@XmlAccessorType(XmlAccessType.NONE)
public class PollerRequestDTO implements RpcRequest, PollerRequest{

    @XmlAttribute(name = "location")
    private String location;

    @XmlAttribute(name = "class-name")
    private String className;

    @XmlAttribute(name = "service-name")
    private String serviceName;

    @XmlAttribute(name = "address")
    @XmlJavaTypeAdapter(InetAddressXmlAdapter.class)
    private InetAddress address;

    @XmlElement(name = "poller-attribute")
    private List<PollerAttributeDTO> pollerAttributes = new ArrayList<>();

    @XmlElement(name = "runtime-attribute")
    private List<PollerAttributeDTO> runtimeAttributes = new ArrayList<>();

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public List<PollerAttributeDTO> getPollerAttributes() {
        return pollerAttributes;
    }

    public void setPollerAttributes(List<PollerAttributeDTO> pollerAttributes) {
        this.pollerAttributes = pollerAttributes;
    }

    public void addPollerAttribute(String key, String value) {
        pollerAttributes.add(new PollerAttributeDTO(key, value));
    }

    public void addPollerAttributes(Map<String, String> attributes) {
        attributes.entrySet().stream().forEach(e -> this.addPollerAttribute(e.getKey(), e.getValue()));
    }

    @Override
    public Map<String, String> getAttributeMap() {
        return pollerAttributes.stream()
                .collect(Collectors.toMap(PollerAttributeDTO::getKey, PollerAttributeDTO::getValue));
    }

    public void setRuntimeAttributes(List<PollerAttributeDTO> attributes) {
        this.runtimeAttributes = attributes;
    }

    public void addRuntimeAttribute(String key, String value) {
        runtimeAttributes.add(new PollerAttributeDTO(key, value));
    }

    public void addRuntimeAttributes(Map<String, String> attributes) {
        attributes.entrySet().stream().forEach(e -> this.addRuntimeAttribute(e.getKey(), e.getValue()));
    }
    
    @Override
    public Map<String, String> getRuntimeAttributes() {
        Map<String, String> runtimeAttributeMap = new HashMap<String, String>();
        for (PollerAttributeDTO agentAttribute : runtimeAttributes) {
            runtimeAttributeMap.put(agentAttribute.getKey(), agentAttribute.getValue());
        }
        return runtimeAttributeMap;
    }

    @Override
    public Long getTimeToLiveMs() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((pollerAttributes == null) ? 0 : pollerAttributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PollerRequestDTO other = (PollerRequestDTO) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (pollerAttributes == null) {
            if (other.pollerAttributes != null)
                return false;
        } else if (!pollerAttributes.equals(other.pollerAttributes))
            return false;
        return true;
    }

}
