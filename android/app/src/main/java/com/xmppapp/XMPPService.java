package com.xmppapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by jacob on 2/22/16.
 */
public class XMPPService extends Service {
    XMPPController xmppController;

    @Override
    public void onCreate() {
        super.onCreate();

        xmppController = ((MainApplication) getApplicationContext()).xmppController;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
