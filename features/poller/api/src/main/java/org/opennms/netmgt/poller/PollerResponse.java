package org.opennms.netmgt.poller;


import org.opennms.core.rpc.api.RpcResponse;

public interface PollerResponse extends RpcResponse{
    
    PollStatus getPollStatus();

}
