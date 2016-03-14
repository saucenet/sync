package com.sauce.sync.conflicts;

import com.sauce.sync.models.Conflict;

import java.util.List;

/**
 * Created by sauce on 5/14/15.
 */
public interface ConflictResolutionStrategy {
    public Resolution resolveConflicts(List<Conflict> conflicts);
}
