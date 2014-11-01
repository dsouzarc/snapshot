package com.ryan.snapshot;

/**
 * Singleton API 
 */
public class SnapShot_API {
    private static final SnapShot_API API = new SnapShot_API();

    private SnapShot_API() {
    }

    public static SnapShot_API getApi() {
        return API;
    }
}
