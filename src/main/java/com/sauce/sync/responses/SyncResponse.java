package com.sauce.sync.responses;

import com.sauce.sync.models.Conflict;
import com.sauce.sync.models.SauceEntity;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by sauce on 4/19/15.
 */
public class SyncResponse {
    private List<SauceEntity> syncedEntities;
    private List<SauceEntity> syncedDelta;
    private List<Conflict> conflicts;
    private List<SauceEntity> tooFarOutOfSyncEntities;
    private String syncId;

    public List<SauceEntity> getSyncedEntities() { return syncedEntities; }
    public List<SauceEntity> getSyncedDelta() { return syncedDelta; }
    public List<SauceEntity> getTooFarOutOfSyncEntities() { return tooFarOutOfSyncEntities; }
    public List<Conflict> getConflicts() { return conflicts; }
    public String getSyncId()  { return syncId; }
    public boolean success() { return conflicts != null && !conflicts.isEmpty(); }

    private static final Logger log = Logger.getLogger(SyncResponse.class.getName());

    public static SyncResponse getConflictSyncResponse(List<Conflict> conflicts, String syncId) {
        SyncResponse response = new SyncResponse();
        response.conflicts = conflicts;
        response.syncId = syncId;
        return response;
    }

    public static SyncResponse getSuccessSyncResponse(List<SauceEntity> syncedEntities, List<SauceEntity> syncedDelta, String syncId) {
        SyncResponse response = new SyncResponse();
        response.syncedEntities = syncedEntities;
        response.syncedDelta = syncedDelta;
        response.syncId = syncId;
        return response;
    }

    public static SyncResponse getTooFarOutOfSyncResponse(List<SauceEntity> tooFarOutOfSyncEntities, String syncId) {
        SyncResponse response = new SyncResponse();
        response.tooFarOutOfSyncEntities = tooFarOutOfSyncEntities;
        response.syncId = syncId;

        log.info("tooFarOutOfSyncResponse:" + response);
        return response;
    }

    @Override
    public String toString() {
        return "SyncResponse{" +
                "syncedEntities=" + syncedEntities +
                ", syncedDelta=" + syncedDelta +
                ", conflicts=" + conflicts +
                ", tooFarOutOfSyncEntities=" + tooFarOutOfSyncEntities +
                ", syncId=" + syncId +
                '}';
    }
}
