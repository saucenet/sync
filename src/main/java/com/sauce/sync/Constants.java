package com.sauce.sync;

import com.sauce.sync.models.ConflictResolutionType;

/**
 * Created by sauce on 1/22/15.
 */
public class Constants {

    // see README.md for info on how to configure this stuff
    public static final String WEB_CLIENT_ID = "your web client id";
    public static final String ANDROID_CLIENT_ID = "your android client id";
    public static final String IOS_CLIENT_ID = "your ios client id";
    public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

    public static final String GCM_API_KEY =  "your gcm api key";


    public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";
    public static final String PLUS_LOGIN_SCOPE = "https://www.googleapis.com/auth/plus.login";
    public static final String PLUS_ME_SCOPE = "https://www.googleapis.com/auth/plus.me";


    /** the reaper reclaims deleted records after a delay (REAPER_DELAY).
     * This field also configures when 'tooFarOutOfSync' exceptions are thrown.  By default,
     * if a client is out of sync by longer than a week, a tooFarOutOfSync exception is thrown
     * because the client may not be away of deletes, since the reaper may have reclaimed them.
     */
    public static final long REAPER_DELAY = 60 * 60 * 24 * 7 * 1000; // milliseconds of a week by default
    /** reaper splay is used to prevent a situation where the clocks on GAE might not be synced up
     * so the reaper only deletes records removed after REAPER_DELAY + REAPER_SPLAY
     */
    public static final long REAPER_SPLAY = 60 * 60 * 1000; // 1 hour

    /** by default conflicts are resolved by throwing a "conflict exception".
     *  SERVER_WINS and CLIENT_WINS can be set explicitly by clients */
    public static ConflictResolutionType DEFAULT_CONFLICT_RESOLUTION_STRATEGY = ConflictResolutionType.MANUAL;
}
