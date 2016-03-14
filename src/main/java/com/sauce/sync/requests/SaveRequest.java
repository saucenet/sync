package com.sauce.sync.requests;

import com.sauce.sync.models.ConflictResolutionType;
import com.sauce.sync.models.SauceEntity;

import java.util.List;

/**
 * Created by sauce on 5/26/15.
 */
public class SaveRequest {
    private List<SauceEntity> objectsToSave;

    public List<SauceEntity> getObjectsToSave() { return objectsToSave; }

    public SyncRequest toSyncRequest() {
        if(objectsToSave == null || objectsToSave.isEmpty()) {
            throw new IllegalArgumentException("objectsToSave cant be null or empty");
        }
        SyncRequest syncRequest = new SyncRequest(0, System.currentTimeMillis(), objectsToSave, null, ConflictResolutionType.CLIENT_WINS);
        return syncRequest;
    }
}
