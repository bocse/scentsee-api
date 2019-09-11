package com.bocse.scentsee.service.eventLogging;


import com.aerospike.client.AerospikeClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Created by bocse on 20.12.2015.
 */
@Service
public class FileBusinessLoggingService implements BusinessLoggingService {

    @Autowired
    AerospikeClient aerospikeClient;
    private Logger businessLogger = LoggerFactory.getLogger("com.bocse.scentsee.businessLogger");
    //TODO:remove pretty printer
    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @Override
    @Async
    public void logEvent(BusinessEvent event) {
        if (aerospikeClient == null)
            return;
        try {
            event.commit(aerospikeClient);
        } catch (Exception ex) {
            // logger.error("Error:",e);
        }
    }

}
