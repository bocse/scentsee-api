package com.bocse.scentsee.users;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bocse on 02.03.2016.
 */
public class User {
    private final static transient String salt1 = "fgergrgerger";
    private final static transient String salt2 = "gergrer";
    private String userId;
    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String accessKey;
    private String secretKey;
    private List<String> stockAuthorization = new ArrayList<>();
    private Boolean active;
    private Long creationTimestamp;
    private Long limit;
    private Long interval;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;//org.apache.commons.codec.digest.DigestUtils.sha256Hex(password);
    }

    public void setPasswordWithHash(String password) {
        this.password = org.apache.commons.codec.digest.DigestUtils.sha256Hex(salt1 + password + salt2);
    }

    public Boolean validatePassword(String password) {
        if (org.apache.commons.codec.digest.DigestUtils.sha256Hex(salt1 + password + salt2).equals(this.password))
            return true;
        else
            return false;
    }

    public List<String> getStockAuthorization() {
        return stockAuthorization;
    }

    public void setStockAuthorization(List<String> stockAuthorization) {
        this.stockAuthorization = stockAuthorization;
    }
}
