package com.sauce.sync.responses;

import com.sauce.sync.models.SauceEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sauce on 5/20/15.
 */
public class QueryResponse {
    private List<SauceEntity> results = new ArrayList<SauceEntity>();

    public QueryResponse(List<SauceEntity> results) {
        this.results = results;
    }

    public List<SauceEntity> getResults() {
        return results;
    }

}
