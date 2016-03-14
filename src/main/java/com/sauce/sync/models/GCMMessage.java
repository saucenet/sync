package com.sauce.sync.models;

/**
 * Created by sauce on 11/3/15.
 */
public class GCMMessage {
    private String to;

    public GCMMessage(String to) {
        this.to = to;
    }

    public String getTo() { return to; }

    @Override
    public String toString() {
        return "GCMMessage{" +
                "to='" + to + '\'' +
                '}';
    }
}
