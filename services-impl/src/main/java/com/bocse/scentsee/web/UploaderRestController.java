package com.bocse.scentsee.web;

import com.aerospike.client.*;
import com.aerospike.client.policy.Priority;
import com.aerospike.client.policy.ScanPolicy;
import com.bocse.scentsee.beans.UploadStatus;
import com.bocse.scentsee.service.upload.UploadServices;
import com.bocse.scentsee.users.User;
import com.bocse.scentsee.users.UserManagement;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/rest/partner")
public class UploaderRestController {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${aerospike.namespace}")
    protected String aerospikeNamespace;
    @Autowired
    AerospikeClient aerospikeClient;
    @Value("${datasource.uploadFolder}")
    String uploadFolder;
    @Autowired
    private UploadServices uploadService;
    private String aerospikeSetName = "uploads";


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public RedirectView handleFileUpload(
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey,
            @RequestParam(value = "stockIdentifier", required = true) String stockIdentifier,
            @RequestParam(value = "redirectTarget", required = false) String redirectTarget,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
//            ,
//                                   RedirectAttributes redirectAttributes
    ) throws IOException {
        final DateTime dateTime = DateTime.now();
        final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy_dd_MM_HH_mm_ss");
        final String key = stockIdentifier + "_" + dtf.print(dateTime) + "_" + file.getOriginalFilename() + "_" + file.getSize();
        final String outputFilePath = uploadFolder + key;

        if (!file.isEmpty()) {
            try {

                if (!file.getOriginalFilename().toLowerCase().trim().endsWith(".csv")) {
                    throw new IllegalStateException("Only CSV files are allowed for upload");
                }
                UserManagement userManagement = new UserManagement(aerospikeClient, aerospikeNamespace);
                User user = userManagement.getUserByAccessKey(accessKey);
                if (user == null) {
                    throw new IllegalStateException("Unknown user");
                }
                if (!user.getStockAuthorization().contains(stockIdentifier)) {
                    throw new IllegalStateException("This user is not authorized to update this stock list.");
                }
                final String referer = request.getHeader("Referer");
                if (redirectTarget == null)
                    redirectTarget = referer;
                if (redirectTarget == null)
                    redirectTarget = "http://scentsee.com/#/partner-file-upload";

                final byte[] contentBytes = file.getBytes();

                if (contentBytes.length == 0) {
                    uploadService.writeToAerospike(accessKey, stockIdentifier, key, file.getOriginalFilename(), outputFilePath, UploadStatus.FAILED_BAD_FORMAT, file.getSize(), dateTime.getMillis(), "Reason: zero sized file.");
                    redirectAttributes.addFlashAttribute("message", "Upload failed.");
                    redirectAttributes.addFlashAttribute("success", "false");
                    return new RedirectView(redirectTarget);

                }


                FileUtils.writeByteArrayToFile(new File(outputFilePath), contentBytes);
                uploadService.writeToAerospike(accessKey, stockIdentifier, key, file.getOriginalFilename(), outputFilePath, UploadStatus.STORED, file.getSize(), dateTime.getMillis(), "Awaiting processing.");

                uploadService.doProcessing(key, stockIdentifier, contentBytes);
                redirectAttributes.addFlashAttribute("message", "Upload successful.");
                redirectAttributes.addFlashAttribute("success", "true");
                uploadService.putInS3(stockIdentifier, key, contentBytes);
                return new RedirectView(redirectTarget);

            } catch (IOException | RuntimeException e) {
                logger.error("Error while uploading: ", e);
                uploadService.writeToAerospike(accessKey, stockIdentifier, key, file.getOriginalFilename(), outputFilePath, UploadStatus.FAILED_BAD_FORMAT, file.getSize(), dateTime.getMillis(), "Reason: " + e.getMessage());
                redirectAttributes.addFlashAttribute("message", "Upload failed.");
                redirectAttributes.addFlashAttribute("success", "false");
                return new RedirectView(redirectTarget);

            }
        } else {
            uploadService.writeToAerospike(accessKey, stockIdentifier, key, file.getOriginalFilename(), outputFilePath, UploadStatus.FAILED_BAD_FORMAT, file.getSize(), dateTime.getMillis(), "Reason: zero sized file.");
            redirectAttributes.addFlashAttribute("message", "Upload failed.");
            redirectAttributes.addFlashAttribute("success", "false");

            return new RedirectView(redirectTarget);

            //redirectAttributes.addFlashAttribute("message", "Failed to upload " + file.getOriginalFilename() + " because it was empty");
        }

    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getStockIdentifiers", method = RequestMethod.GET)
    public List<String> getStockIdentifiers(
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey
    ) {
        UserManagement userManagement = new UserManagement(aerospikeClient, aerospikeNamespace);
        User user = userManagement.getUserByAccessKey(accessKey);
        return user.getStockAuthorization();
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public List<String> testMethod(

    ) {
        return new ArrayList<String>();
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/getUploadStatus", method = RequestMethod.GET)
    public Map<String, Object> getAllAssets(
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey,
            @RequestParam(value = "stockIdentifier", required = true) String stockIdentifier
    ) {
        Map<String, Object> result = new HashMap<>();
        final List<Map<String, Object>> fileList = new ArrayList<>();


        try {
            ScanPolicy policy = new ScanPolicy();
            policy.concurrentNodes = true;
            policy.priority = Priority.LOW;
            policy.includeBinData = true;

            aerospikeClient.scanAll(policy, aerospikeNamespace, aerospikeSetName, new ScanCallback() {
                @Override
                public void scanCallback(Key key, Record record) throws AerospikeException {
                    if (stockIdentifier.equals(record.getString("stockid"))) {
                        Map<String, Object> fileDescriptor = new HashMap<>();
                        fileDescriptor.put("name", record.getString("filename"));
                        fileDescriptor.put("key", record.getString("key"));
                        fileDescriptor.put("timestamp", record.getLong("timestamp"));
                        fileDescriptor.put("dateTime", new DateTime(record.getLong("timestamp")).toString());
                        fileDescriptor.put("fileSize", record.getLong("size"));
                        fileDescriptor.put("totalItems", record.getLong("itotal"));
                        fileDescriptor.put("matchedItems", record.getLong("imatched"));
                        fileDescriptor.put("status", record.getValue("status"));
                        fileDescriptor.put("description", record.getString("description"));
                        fileList.add(fileDescriptor);
                    }
                }
            });
        } finally {

        }
        Collections.sort(fileList, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return -((Long) o1.get("timestamp")).compareTo((Long) o2.get("timestamp"));
            }
        });
        result.put("payload", fileList);
        return result;
    }

}