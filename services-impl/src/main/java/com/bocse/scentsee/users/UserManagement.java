package com.bocse.scentsee.users;

import com.aerospike.client.*;
import com.aerospike.client.policy.Priority;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by bocse on 02.03.2016.
 */


public class UserManagement {

    protected String aerospikeNamespace;
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private AerospikeClient aerospikeClient;

    public UserManagement(AerospikeClient aerospikeClient, String aerospikeNamespace) {
        this.aerospikeClient = aerospikeClient;
        this.aerospikeNamespace = aerospikeNamespace;
    }

    public void createUser(User user, Boolean overwrite) {
        User existingUser = this.getUserByEmail(user.getEmail());
        if (existingUser != null) {
            if (overwrite) {
                System.out.println("User exists, overwriting user and regenerating keys.");
                this.deleteUserByAccessKey(existingUser.getAccessKey());
            } else {
                throw new IllegalStateException("User with email " + user.getEmail() + " already exists");
            }
        }

        user.setAccessKey(RandomStringUtils.randomAlphanumeric(32));
        user.setSecretKey(RandomStringUtils.randomAlphanumeric(64));
        user.setActive(true);
        user.setUserId(RandomStringUtils.randomAlphanumeric(10));
        user.setCreationTimestamp(System.currentTimeMillis());
        Key aerospikeKey = new Key(aerospikeNamespace, "users", user.getAccessKey());
        Bin access = new Bin("access", user.getAccessKey());
        Bin secret = new Bin("secret", user.getSecretKey());
        Bin active = new Bin("active", user.getActive());
        Bin firstName = new Bin("firstName", user.getFirstName());
        Bin lastName = new Bin("lastName", user.getLastName());
        Bin password = new Bin("password", user.getPassword());
        Bin id = new Bin("id", user.getUserId());
        Bin email = new Bin("email", user.getEmail());
        Bin creation = new Bin("creation", user.getCreationTimestamp());
        Bin limit = new Bin("limit", user.getLimit());
        Bin interval = new Bin("interval", user.getInterval());
        Bin stockAuth = new Bin("stockauth", user.getStockAuthorization());
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.expiration = -1;
        aerospikeClient.put(writePolicy, aerospikeKey, access, secret, id, email, password, firstName, lastName, active, creation, limit, interval, stockAuth);
        System.out.println("aceessKey: \n" + user.getAccessKey());
        System.out.println("secretKey: \n" + user.getSecretKey());
    }


    public void createInvitationRequest(String email, String fullName, String reason, String ip) {

        Key aerospikeKey = new Key(aerospikeNamespace, "invrq", email);
        Bin emailBin = new Bin("email", email);
        Bin fullNameBin = new Bin("fullName", fullName);
        Bin reasonBin = new Bin("reason", reason);
        Bin ipBin = new Bin("ip", ip);
        Bin isGrantedBin = new Bin("isGranted", false);
        Bin priorityBin = new Bin("priority", 1);
        Bin requestDateBin = new Bin("requestDate", System.currentTimeMillis());

        WritePolicy writePolicy = new WritePolicy();
        writePolicy.expiration = -1;
        aerospikeClient.put(writePolicy, aerospikeKey, emailBin, fullNameBin, reasonBin, isGrantedBin, requestDateBin, priorityBin, ipBin);
    }

    public List<Map<String, String>> getAllInvites() {
        final List<Map<String, String>> result = new ArrayList<>();
        ScanPolicy policy = new ScanPolicy();
        policy.concurrentNodes = true;
        policy.priority = Priority.LOW;
        policy.includeBinData = true;

        aerospikeClient.scanAll(policy, aerospikeNamespace, "invrq", new ScanCallback() {

            @Override
            public void scanCallback(Key key, Record record)
                    throws AerospikeException {
                Map<String, String> row = new HashMap<>();
                row.put("email", record.getString("email"));
                row.put("fullName", record.getString("fullName"));
                row.put("reason", record.getString("reason"));
                row.put("ip", record.getString("ip"));
                row.put("requestDate", new DateTime(record.getLong("requestDate")).toString());
                row.put("isGranted", String.valueOf(record.getBoolean("isGranted")));
                result.add(row);

            }
        });
        return result;
    }

    private User recordToUser(Record record) {
        User user = new User();
        user.setAccessKey(record.getString("access"));
        user.setSecretKey(record.getString("secret"));
        user.setLimit(record.getLong("limit"));
        user.setInterval(record.getLong("interval"));
        user.setEmail(record.getString("email"));
        user.getStockAuthorization().clear();
        if (record.getValue("stockauth") != null) {
            user.getStockAuthorization().addAll((List<String>) record.getList("stockauth"));
        }
        user.setActive(record.getBoolean("active"));
        user.setCreationTimestamp(record.getLong("creation"));
        user.setUserId(record.getString("id"));
        user.setFirstName(record.getString("firstName"));
        user.setLastName(record.getString("lastName"));
        user.setPassword(record.getString("password"));
        return user;
    }

    public User getUserByAccessKey(String accessKey) {
        Key aerospikeKey = new Key(aerospikeNamespace, "users", accessKey);
        Record record = aerospikeClient.get(null, aerospikeKey);
        if (record == null)
            return null;
        return recordToUser(record);
    }

    public Boolean deleteUserByAccessKey(String accessKey) {
        Key key = new Key(aerospikeNamespace, "users", accessKey);
        return aerospikeClient.delete(null, key);
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        ScanPolicy policy = new ScanPolicy();
        policy.concurrentNodes = true;
        policy.priority = Priority.LOW;
        policy.includeBinData = true;

        aerospikeClient.scanAll(policy, aerospikeNamespace, "users", new ScanCallback() {
            @Override
            public void scanCallback(Key key, Record record) throws AerospikeException {
                users.add(recordToUser(record));
            }
        });
        Collections.sort(users, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return o1.getEmail().compareTo(o2.getEmail());
            }
        });
        return users;
    }

    public User getUserByEmail(String email) {
        Statement stmt = new Statement();
        stmt.setNamespace(aerospikeNamespace);
        stmt.setSetName("users");
        stmt.setFilters(Filter.equal("email", email));
        RecordSet rs = aerospikeClient.query(null, stmt);
        Record keptRecord = null;
        try {
            while (rs.next()) {
                Key key = rs.getKey();
                Record record = rs.getRecord();
                if (keptRecord == null)
                    keptRecord = record;
                if (record.getLong("creation") > keptRecord.getLong("creation")) {
                    keptRecord = record;
                }
                //return recordToUser(record);
            }
        } finally {
            rs.close();
        }
        if (keptRecord != null)
            return recordToUser(keptRecord);
        else
        return null;
    }
}
