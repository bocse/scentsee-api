package com.bocse.scentsee.interceptor;

import com.aerospike.client.*;
import com.aerospike.client.policy.WritePolicy;
import com.bocse.scentsee.exceptions.RateLimitExceededException;
import com.bocse.scentsee.exceptions.UnauthorizedAccessException;
import com.bocse.scentsee.users.User;
import com.bocse.scentsee.users.UserManagement;
import com.bocse.scentsee.utils.IPRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by bocse on 11.02.2016.
 */
@Service
public class RateLimitInterceptor extends HandlerInterceptorAdapter {
    private final String keySeparator = "-";
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    AerospikeClient aerospikeClient;
    @Value("${aerospike.namespace}")
    private String aerospikeNamespace;
    @Autowired
    private ObjectMapper jacksonObjectMapper;
    @Value("${rate.enforce}")
    private Boolean rateEnforce;
    @Value("${rate.limitingInterval}")
    private Long rateLimitingInterval = 3600 * 1000L;
    @Value("${rate.maximumPerIP}")
    private Long maximumPerIP = 50L;

    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler)
            throws Exception {

        logger.debug("Intercepted");
        if (!rateEnforce)
            return true;

        if (aerospikeClient == null) {
            logger.error("Cannot impose rate limit. Aerospike down.");
            return true;
        }

        if (!aerospikeClient.isConnected()) {
            logger.error("Cannot impose rate limit. Aerospike down.");
            return true;
        }

        String accessKey = getAccessKey(request);

        if (accessKey == null) {
            String IP = getIP(request);
            Long currentRate = getIPRate(IP);

            if (currentRate == null) {
                setIPRate(IP, 1L);
            } else {
                if (currentRate > maximumPerIP) {
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    throw new RateLimitExceededException("You have made more than " + maximumPerIP + " requests in the last hour.  Please contact us and request a ScentSee Premium account for professional use.");
                }
                incrementIPRate(IP, 1L);
            }

        } else {
            String secretKey = request.getParameter("secretKey");
            UserManagement userManagement = new UserManagement(aerospikeClient, aerospikeNamespace);
            User user = userManagement.getUserByAccessKey(accessKey);
            if (user == null) {
                throw new UnauthorizedAccessException("Access/Secret key mismatch");
            }
            if (!user.getSecretKey().equals(secretKey)) {
                throw new UnauthorizedAccessException("Access/Secret key mismatch");
            }
            Long currentRate = getUserRate(accessKey);

            if (currentRate == null) {
                setUserRate(accessKey, 1L);
            } else {
                if (currentRate > user.getLimit()) {
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    throw new RateLimitExceededException("You have made more than " + user.getLimit() + " requests in the last hour. Please contact us and request a ScentSee Premium account for professional use.");
                }
                incrementUserRate(accessKey, 1L);
            }
            //
        }
        //throw new IllegalStateException("Rate exceeded");
        //return false;
        return true;
    }

    private void setIPRate(String key, Long value) {
        setRate("ip-rate", key, value);
    }

    private void setUserRate(String key, Long value) {
        setRate("user-rate", key, value);
    }

    private void setRate(String set, String key, Long value) {
        Key aerospikeKey = new Key(aerospikeNamespace, set, key + keySeparator + getTimeUnit());
        WritePolicy writePolicy = new WritePolicy();
        //writePolicy.expiration = (int) (rateLimitingInterval / 1000) * 24;
        Bin binKey = new Bin("key", key);
        Bin timeUnit = new Bin("time", getTimeUnit());
        Bin binIP = new Bin("count", value);
        aerospikeClient.put(writePolicy, aerospikeKey, binKey, timeUnit, binIP);
    }

    private void incrementIPRate(String key, Long increment) {
        incrementRate("ip-rate", key, increment);
    }

    private void incrementUserRate(String key, Long increment) {
        incrementRate("user-rate", key, increment);
    }

    private void incrementRate(String set, String key, Long increment) {
        Key aerospikeKey = new Key(aerospikeNamespace, set, key + keySeparator + getTimeUnit());
        WritePolicy writePolicy = new WritePolicy();
        //writePolicy.expiration = (int) (rateLimitingInterval / 1000) * 24;
        Bin binIP = new Bin("count", increment);
        Record record = aerospikeClient.operate(writePolicy, aerospikeKey, Operation.add(binIP));
    }

    private Long getIPRate(String key) {
        return getRate("ip-rate", key);
    }

    private Long getUserRate(String key) {
        return getRate("user-rate", key);
    }

    private Long getRate(String set, String key) {

        Key aerospikeKey = new Key("scentsee", set, key + keySeparator + getTimeUnit());
        Record record = aerospikeClient.get(null, aerospikeKey);
        if (record == null)
            return null;
        else {
            if (record.bins.containsKey("count")) {
                return record.getLong("count");
            } else {
                return null;
            }
        }
    }

    private Long getTimeUnit() {
        return System.currentTimeMillis() / rateLimitingInterval;
    }

    private String getIP(HttpServletRequest request) {
        return IPRetriever.getClientIpAddress(request);
    }

    private String getAccessKey(HttpServletRequest request) {

        return request.getParameter("accessKey");
    }
}