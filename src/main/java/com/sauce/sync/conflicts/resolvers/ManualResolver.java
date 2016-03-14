package com.sauce.sync.conflicts.resolvers;

import com.sauce.sync.conflicts.ConflictResolutionStrategy;
import com.sauce.sync.conflicts.Resolution;
import com.sauce.sync.models.Conflict;

import java.util.List;

/**
 * Created by sauce on 5/15/15.
 */
public class ManualResolver implements ConflictResolutionStrategy {
    @Override
    public Resolution resolveConflicts(List<Conflict> conflicts) {
        return Resolution.getUnresolvedResolution(conflicts);
    }
}
