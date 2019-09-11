package com.bocse.scentsee.beans;

/**
 * Created by bogdan.bocse on 10/08/16.
 */
public enum UploadStatus {
    RECEIVED,
    STORED,
    STORED_HA,
    PROCESSED,
    FAILED_EMPTY,
    FAILED_BAD_FORMAT,
    FAILED_UNKNOWN_REASON
}
