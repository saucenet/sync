package com.sauce.sync.json;

/**
 * Created by sauce on 5/11/15.
 */
public class OAuthTokenInfoResponse {
    private String user_id;

    public String getUserId() {
        return user_id;
    }

    @Override
    public String toString() {
        return "OAuthTokenInfoResponse{" +
                "user_id='" + user_id + '\'' +
                '}';
    }
}
