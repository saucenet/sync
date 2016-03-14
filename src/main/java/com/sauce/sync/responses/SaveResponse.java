package com.sauce.sync.responses;

import com.sauce.sync.models.SauceEntity;

import java.util.List;

/**
 * Created by sauce on 5/26/15.
 */
public class SaveResponse {
    private List<SauceEntity> savedEntities;

    public List<SauceEntity> getSavedEntities() { return savedEntities; }

    public static SaveResponse syncResponseToSavedResponse(SyncResponse syncResponse) {
        SaveResponse response = new SaveResponse();
        response.savedEntities = syncResponse.getSyncedEntities();
        return response;
    }


}
