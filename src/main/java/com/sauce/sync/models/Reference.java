package com.sauce.sync.models;

/**
 * Created by sauce on 5/11/15.
 */
public class Reference {
    private String key;
    private Integer arrayIndex;

    public Reference() {/*for endpoints bean*/}
    public Reference(String key, Integer arrayIndex) {
        this.key = key;
        this.arrayIndex = arrayIndex;
    }

    public String getKey() { return key; }
    public Integer getArrayIndex() { return arrayIndex; }
}
