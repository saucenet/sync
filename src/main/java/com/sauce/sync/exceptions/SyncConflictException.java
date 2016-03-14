package com.sauce.sync.exceptions;

import com.google.api.server.spi.ServiceException;
import com.sauce.sync.models.Conflict;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sauce on 4/26/15.
 */
public class SyncConflictException extends ServiceException {
    private List<Conflict> conflicts = new ArrayList<Conflict>();

    public SyncConflictException(Exception e) {
        super(409, e);
    }

    public SyncConflictException setConflicts(List<Conflict> conflicts) {
        this.conflicts = conflicts;
        return this;
    }

    public List<Conflict> getConflicts() {
        return conflicts;
    }
}
