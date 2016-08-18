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
