package com.sauce.sync.conflicts.resolvers;

import com.sauce.sync.conflicts.ConflictResolutionStrategy;
import com.sauce.sync.conflicts.Resolution;
import com.sauce.sync.models.Conflict;
import com.sauce.sync.models.SauceEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sauce on 5/14/15.
 */
public class ServerWinsResolver implements ConflictResolutionStrategy {
    @Override
    public Resolution resolveConflicts(List<Conflict> conflicts) {
        List<SauceEntity> resolvedConflicts = new ArrayList<SauceEntity>();
        for(Conflict conflict: conflicts) {
            resolvedConflicts.add(conflict.getServerEntity());
        }
        return Resolution.getResolvedResolution(resolvedConflicts);
    }
}
