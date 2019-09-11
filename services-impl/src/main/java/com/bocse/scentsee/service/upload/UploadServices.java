package com.bocse.scentsee.service.upload;

import com.aerospike.client.*;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.bocse.perfume.affiliate.*;
import com.bocse.perfume.data.AffiliatePerfume;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.signature.SignatureEvaluator;
import com.bocse.scentsee.beans.UploadStatus;
import com.bocse.scentsee.service.eventLogging.BusinessLoggingService;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bocse on 12/08/16.
 */
@Service
public class UploadServices {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${aerospike.namespace}")
    protected String aerospikeNamespace;
    @Value("${s3.bucketName}")
    protected String s3Bucket;
    @Value("${s3.accessKey}")
    protected String s3AccessKey;
    @Value("${s3.secretKey}")
    protected String s3SecretKey;
    @Value("${presentation.cdn.host}")
    String cdnHost;
    @Value("${presentation.fallbackImage}")
    String fallbackImage;
    @Value("${datasource.notesFile}")
    String notesFilePath;
    @Value("${maintenance.startHour}")
    Integer maintenanceStartHour;
    @Value("${maintenance.endHour}")
    Integer maintenanceEndHour;
    @Value("${datasource.perfumeFile}")
    String perfumeFilePath;
    @Value("${datasource.perfumeFileUpdated}")
    String perfumeUpdatedFilePath;
    @Value("${datasource.uploadFolder}")
    String uploadFolder;
    @Autowired
    AerospikeClient aerospikeClient;
    @Autowired
    BusinessLoggingService businessLoggingService;
    @Autowired
    PerfumeIterator perfumeIterator;
    private String aerospikeSetName = "uploads";
    @Autowired
    private SignatureEvaluator signatureEvaluator;

    @Async
    public void doProcessing(String key, String stockIdentifier, byte[] contentBytes) throws IOException {
        try {
            putInS3(stockIdentifier, key, contentBytes);
            updateToAerospike(key, "status", UploadStatus.STORED_HA);
            updateToAerospike(key, "description", "Stored in persistent layer.");
        } catch (Exception ex) {
            logger.error("Error while loading to S3", ex);
            updateToAerospike(key, "status", UploadStatus.FAILED_UNKNOWN_REASON);
            updateToAerospike(key, "description", ex.getMessage());
            throw ex;
        }
        try {
            process(stockIdentifier, contentBytes);
            updateToAerospike(key, "status", UploadStatus.PROCESSED);
            updateToAerospike(key, "description", "Processed successfully.");

        } catch (Exception ex) {
            logger.error("Error while processing file", ex);
            updateToAerospike(key, "status", UploadStatus.FAILED_BAD_FORMAT);
            updateToAerospike(key, "description", ex.getMessage());
            throw ex;
        }
    }


    public void writeToAerospike(String accessKey, String stockIdentifier, String key, String fileName, String filePath, UploadStatus status, Long size, Long timestamp, String description) {
        Key aerospikeKey = new Key(aerospikeNamespace, aerospikeSetName, key);
        Bin binOwner = new Bin("owner", accessKey);
        Bin binStockIdentifier = new Bin("stockid", stockIdentifier);
        Bin binKey = new Bin("key", key);
        Bin binFileName = new Bin("filename", fileName);
        Bin binFilePath = new Bin("filepath", filePath);
        Bin binStatus = new Bin("status", status);
        Bin binSize = new Bin("size", size);
        Bin binTimestamp = new Bin("timestamp", timestamp);
        Bin binDescription = new Bin("description", description);
        aerospikeClient.put(null, aerospikeKey, binOwner, binStockIdentifier, binKey, binFileName, binFilePath, binStatus, binSize, binTimestamp, binDescription);
    }

    public void updateToAerospike(String key, String field, Object value) {
        Key aerospikeKey = new Key(aerospikeNamespace, aerospikeSetName, key);
        Bin bin = new Bin(field, value);
        Record record = aerospikeClient.operate(null, aerospikeKey, Operation.put(bin));
    }

    public void putInS3(String stockIdentifier, String key, byte[] contentBytes) {
        AmazonS3 client = new AmazonS3Client(
                new BasicAWSCredentials(s3AccessKey, s3SecretKey));
        InputStream stream = new ByteArrayInputStream(contentBytes);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(contentBytes.length);
        meta.setContentType("application/csv");
        client.putObject(s3Bucket, stockIdentifier + "/" + key, stream, meta);
    }

    public void process(String stockIdentifier, byte[] contentBytes) throws IOException {
        Affiliate affiliate=null;

        //TODO: Add your own implementation for mapping affiliates
        if (affiliate==null)
            return;

        if (affiliate.getAllAffiliatePerfumes().isEmpty()) {
            throw new IllegalStateException("No perfumes were read. Possible bad format.");
        }
        final File notesFile = new File(notesFilePath);
        Boolean shouldReload = signatureEvaluator.shouldReload(notesFile);
        if (shouldReload) {
            logger.info("Reloading notes");
            signatureEvaluator.iterateAndKeep(notesFile);
            signatureEvaluator.swap();
        } else {

            logger.info("Not realoding notes");

        }


        File perfumeFile;
        final File originalPerfumeFile = new File(perfumeFilePath);
        final File updatedPerfumeFile = new File(perfumeUpdatedFilePath);

        if (updatedPerfumeFile.exists())
            perfumeFile = updatedPerfumeFile;
        else
            perfumeFile = originalPerfumeFile;

        if (shouldReload) {
            logger.info("Reloading perfumes.");
            perfumeIterator.iterateAndKeep(perfumeFile);
            for (Perfume perfume : perfumeIterator.getBackgroundPerfumeList()) {
                signatureEvaluator.embedPerfumeSignature(perfume);
            }

            for (Perfume perfume : perfumeIterator.getBackgroundPerfumeList()) {
                if (perfume.getPictureURL().isEmpty()) {
                    perfume.setPictureURL(fallbackImage);
                } else {
                    perfume.setPictureURL(cdnHost + perfume.getId() + ".jpg");
                }
            }

        } else {
            logger.info("Not reloading, nothing changed.");
            perfumeIterator.mirror();
        }
        AtomicInteger affiliateMatchCount = new AtomicInteger(0);
        final StopWatch stopWatch = new StopWatch();
        int index = 0;
        stopWatch.start();
        for (Perfume perfume : perfumeIterator.getBackgroundPerfumeList()) {
            boolean hasMatch = false;

            List<AffiliatePerfume> affiliatePerfumes = affiliate.lookup(perfume);

            if (affiliatePerfumes.size() > 0) {
                hasMatch = true;
                affiliateMatchCount.addAndGet(1);
                perfume.getAffiliateProducts().put(affiliate.getAffiliateName(), affiliatePerfumes);
            } else {
                perfume.getAffiliateProducts().remove(affiliate.getAffiliateName());
            }


            index++;
            if (index % 1000 == 0) {
                logger.info("Done " + (100.0 * index / perfumeIterator.getPerfumeList().size()) + " %");
                logger.info("Matches with partner " + stockIdentifier + ": " + affiliateMatchCount.toString());
            }
        }
        if (shouldReload) {
            //signatureEvaluator.swap();
            perfumeIterator.swap();
        }
        perfumeIterator.serialize(updatedPerfumeFile);
        stopWatch.stop();

        logger.info("Done " + (100.0 * index / perfumeIterator.getPerfumeList().size()) + " %");
        logger.info("Matches with partner " + stockIdentifier + ": " + affiliateMatchCount.toString());
        logger.info("FINISHED reloading data for " + stockIdentifier + " in " + stopWatch.getTime() + "ms");
    }
}
