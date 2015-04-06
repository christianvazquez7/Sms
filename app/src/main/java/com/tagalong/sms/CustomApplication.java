package com.tagalong.sms;

/**
 * Created by christianvazquez on 3/10/15.
 */
import android.app.Application;

import com.parse.Parse;

public class CustomApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Parse.initialize(this, "gtpR5QrRLjQE4NMnBM3oWJ69TY3hNnbiNbQz90Xd", "NHh3oaQZHKaHhY1Fqq3sCedy6dd82JD1DRZYW6rO");
    }

}
