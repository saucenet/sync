package com.sauce.sync.requests;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.sauce.sync.Constants;
import com.sauce.sync.models.ConflictResolutionType;
import com.sauce.sync.models.SauceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by sauce on 4/21/15.
 */
public class SyncRequest {
    private List<SauceEntity> objectsToSync;
    private Long lastSyncedId = 0l;
    private Long lastSyncedTime = 0l;
    private String syncId = "0.0";
    private ConflictResolutionType conflictResolutionType;
    private QueryRequest deltaQuery;
    private String senderId;

    private static final Logger log = Logger.getLogger(SyncRequest.class.getName());

    public SyncRequest() {}

    public SyncRequest(long lastSyncedId, long lastSyncedTime, List<SauceEntity> objectsToSync, QueryRequest deltaQuery, ConflictResolutionType conflictResolutionType) {
        this.objectsToSync = objectsToSync;
        this.lastSyncedId = lastSyncedId;
        this.lastSyncedTime = lastSyncedTime;
        this.conflictResolutionType = conflictResolutionType;
        this.deltaQuery = deltaQuery;
    }

    public SyncRequest(long lastSyncedId, long lastSyncedTime) {
        this.lastSyncedId = lastSyncedId;
        this.lastSyncedTime = lastSyncedTime;
    }

    public List<SauceEntity> getObjectsToSync() { return objectsToSync; }
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getLastSyncedId() { return lastSyncedId; }
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getLastSyncedTime() { return lastSyncedTime; }
    public ConflictResolutionType getConflictResolutionType() { return conflictResolutionType; }
    public QueryRequest getDeltaQuery() { return deltaQuery; }
    public String getSyncId() { return syncId;}
    public void setSyncId(String syncId) {
        if(syncId == null) {
            syncId = "0.0";
        }
        int indexOfPeriod = syncId.indexOf('.');
        if(indexOfPeriod == -1) {
            throw new IllegalArgumentException("invalid syncId");
        }

        lastSyncedId = Long.valueOf(syncId.substring(0,indexOfPeriod));
        lastSyncedTime = Long.valueOf(syncId.substring(indexOfPeriod+1));
    }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public void validateRequest() {
        List<String>  errorMessages = new ArrayList<String>();

        if(syncId == null) {
            errorMessages.add("syncId cant be null");
        }

        if(!errorMessages.isEmpty()){
            throw new IllegalArgumentException(errorMessages.toString());
        }

        //loop to check objectsToSync
        if(objectsToSync != null) {
            for(SauceEntity objectToSync: objectsToSync) {
                //
            }
        }

        //no more validation from here on, just trusted operations, since endpoints serializes empty as null
        //we can populate some defaults here
        if(conflictResolutionType == null) {
            conflictResolutionType = Constants.DEFAULT_CONFLICT_RESOLUTION_STRATEGY;
        }

        if(objectsToSync == null) {
            objectsToSync = new ArrayList<SauceEntity>();
        }

        return;
    }

}
