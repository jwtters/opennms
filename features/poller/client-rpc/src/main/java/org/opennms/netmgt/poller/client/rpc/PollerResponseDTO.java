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

@XmlRootElement(name = "poller-response")
@XmlAccessorType(XmlAccessType.NONE)
public class PollerResponseDTO implements RpcResponse, PollerResponse {

    @XmlElement(name = "poll-status")
    private PollStatus pollStatus;
    
    @XmlAttribute(name = "failure-message")
    private String failureMesage;

    PollerResponseDTO() {

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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((failureMesage == null) ? 0 : failureMesage.hashCode());
        result = prime * result + ((pollStatus == null) ? 0 : pollStatus.hashCode());
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
        PollerResponseDTO other = (PollerResponseDTO) obj;
        if (failureMesage == null) {
            if (other.failureMesage != null)
                return false;
        } else if (!failureMesage.equals(other.failureMesage))
            return false;
        if (pollStatus == null) {
            if (other.pollStatus != null)
                return false;
        } else if (!pollStatus.equals(other.pollStatus))
            return false;
        return true;
    }

}
