package com.bocse.scentsee.service.eventLogging;

import com.aerospike.client.AerospikeClient;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by bocse on 20.12.2015.
 */


public abstract class BusinessEvent {
    @Value("${aerospike.namespace}")
    protected String aerospikeNamespace = "scentsee";

    private String ip;
    private String userAgent;

    public abstract void commit(AerospikeClient aerospikeClient);

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
