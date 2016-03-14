package com.sauce.sync.models;

/**
 * Created by sauce on 4/22/15.
 */
public class Conflict {
    private SauceEntity clientEntity;
    private SauceEntity serverEntity;
    public Conflict(SauceEntity clientEntity, SauceEntity serverEntity) {
        this.clientEntity = clientEntity;
        this.serverEntity = serverEntity;
    }

    public SauceEntity getClientEntity() {
        return clientEntity;
    }

    public SauceEntity getServerEntity() {
        return serverEntity;
    }

    @Override
    public String toString() {
        return "Conflict{" +
                "clientEntity=" + clientEntity +
                ", serverEntity=" + serverEntity +
                '}';
    }
}
