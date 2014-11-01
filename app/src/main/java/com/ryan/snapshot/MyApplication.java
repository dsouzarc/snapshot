package com.ryan.snapshot;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(getApplicationContext(), "bakAKyvOrQMPzvBbc6yIc8JRoVv9zsZS2dinh7V7", "YhPwVxYKxeaWZTugfqXFsMnYE8jRQ6irDKLPGupg");
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();
    }
}
