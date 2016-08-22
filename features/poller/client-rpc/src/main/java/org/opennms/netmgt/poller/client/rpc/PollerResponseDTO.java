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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.core.rpc.api.RpcResponse;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.PollerResponse;

import com.google.common.base.Objects;

@XmlRootElement(name = "poller-response")
@XmlAccessorType(XmlAccessType.NONE)
public class PollerResponseDTO implements RpcResponse, PollerResponse {

    @XmlElement(name = "poll-status")
    private PollStatus pollStatus;
    
    @XmlAttribute(name = "failure-message")
    private String failureMesage;

    public PollerResponseDTO() {
        // no-arg constructor for JAXB
    }
    
    public PollerResponseDTO(Throwable t) {
       setFailureMesage(t.getMessage());
    }

    PollerResponseDTO(PollStatus pollStatus) {
        this.pollStatus = pollStatus;
    }

    @Override
    public PollStatus getPollStatus() {
        return pollStatus;
    }

    public void setPollStatus(PollStatus pollStatus) {
        this.pollStatus = pollStatus;
    }

    public String getFailureMesage() {
        return failureMesage;
    }

    public void setFailureMesage(String failureMesage) {
        this.failureMesage = failureMesage;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(failureMesage, pollStatus);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PollerResponseDTO other = (PollerResponseDTO) obj;
        return Objects.equal(this.failureMesage, other.failureMesage)
                && Objects.equal(this.pollStatus, other.pollStatus);
    }

}
