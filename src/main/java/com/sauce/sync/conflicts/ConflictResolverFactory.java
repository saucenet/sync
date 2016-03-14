package com.sauce.sync.conflicts;

import com.sauce.sync.conflicts.resolvers.ClientWinsResolver;
import com.sauce.sync.conflicts.resolvers.ManualResolver;
import com.sauce.sync.conflicts.resolvers.ServerWinsResolver;
import com.sauce.sync.models.Conflict;
import com.sauce.sync.models.ConflictResolutionType;

import java.util.List;

/**
 * Created by sauce on 5/14/15.
 */
public class ConflictResolverFactory {

    public static ConflictResolutionStrategy getConflictResolver(ConflictResolutionType conflictResolutionStrategy) {

        switch(conflictResolutionStrategy) {
            case SERVER_WINS:
                return new ServerWinsResolver();
            case CLIENT_WINS:
                return new ClientWinsResolver();
            case MANUAL:
                return new ManualResolver();
            default:
                throw new UnsupportedOperationException("unknown conflict resolution strategy:" + conflictResolutionStrategy);
        }
    }

    public static Resolution getResolverAndResolveConflicts(ConflictResolutionType conflictResolutionType, List<Conflict> conflicts) {
        return getConflictResolver(conflictResolutionType).resolveConflicts(conflicts);
    }


}

