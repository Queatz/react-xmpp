package com.xmppapp;

import android.app.Application;

/**
 * Created by jacob on 2/22/16.
 */
public class MainApplication extends Application {
    public XMPPController xmppController;

    @Override
    public void onCreate() {
        super.onCreate();

        xmppController = new XMPPController(this);
    }

    @Override
    public void onTerminate() {
        xmppController.close();
        super.onTerminate();
    }
}
