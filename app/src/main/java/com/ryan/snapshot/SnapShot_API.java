package com.ryan.snapshot;

import android.app.Activity;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;

public class SnapShot_API {

    public static Activity theActivity;


    private static final SnapShot_API API = new SnapShot_API();

    private static MobileServiceClient mClient;

    private SnapShot_API() {
        try {
            mClient = new MobileServiceClient("https://snapshot.azure-mobile.net/",
                    "gzWFegbXiTLVoLkHtqvDKPzctugOGH61", theActivity);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static SnapShot_API getApi() {
        return API;
    }

    public static  MobileServiceClient getClient() {
        return mClient;
    }
}
