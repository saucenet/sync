package com.sauce.sync.utils;

/**
 * Created by sauce on 2/16/16.
 */
public class SyncIdUtils {

    private SyncIdUtils() {}

    public static int getLastSyncedId(String syncId) {
        String left = syncId.substring(0, syncId.indexOf('.'));
        return Integer.valueOf(left);
    }

    public static long getLastSyncedTime(String syncId) {
        String right = syncId.substring(syncId.indexOf('.') + 1);
        return Long.valueOf(right);
    }

}
