package com.sauce.sync.conflicts;

import com.sauce.sync.models.Conflict;
import com.sauce.sync.models.SauceEntity;

import java.util.List;

/**
 * Created by sauce on 5/15/15.
 */
public class Resolution {
    private List<Conflict> unresolvedConflicts;
    private List<SauceEntity> resolvedConflicts;

    public static Resolution getUnresolvedResolution(List<Conflict> unresolvedConflicts) {
        Resolution resolution = new Resolution();
        resolution.unresolvedConflicts = unresolvedConflicts;
        return resolution;
    }

    public static Resolution getResolvedResolution(List<SauceEntity> resolvedConflicts) {
        Resolution resolution = new Resolution();
        resolution.resolvedConflicts = resolvedConflicts;
        return resolution;
    }

    public List<SauceEntity> getResolvedConflicts() {
        return resolvedConflicts;
    }

    public List<Conflict> getUnresolvedConflicts() {
        return unresolvedConflicts;
    }

    public boolean resolved() {
        if(unresolvedConflicts != null && !unresolvedConflicts.isEmpty()) {
            return false;
        }
        return true;
    }
}
