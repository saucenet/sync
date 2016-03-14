package com.sauce.sync.models;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Created by sauce on 4/19/15.
 */
@Entity
public class SauceUser {
    @Id private String userId;
    @Index private String email;
    private long lastSyncedId;
    private long lastSyncedTime;
    private String syncId;

    public SauceUser() {}
    public SauceUser(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getSyncId() { return lastSyncedId + "." + lastSyncedTime; }
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getLastSyncedId() { return lastSyncedId; }
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public long getLastSyncedTime() { return lastSyncedTime; }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<SauceUser> getKey() {
        return Key.create(SauceUser.class, userId);
    }

    public long incrementLastSyncedId(long lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
        lastSyncedId++;
        return lastSyncedId;
    }

    public long setLastSyncedTime(long lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
        return this.lastSyncedTime;
    }
}
