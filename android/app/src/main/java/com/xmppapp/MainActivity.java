package com.xmppapp;

import android.content.Intent;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.xmppapp.lib.XMPPEventConstants;
import com.xmppapp.lib.XMPPReactPackage;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends ReactActivity {

    @Override
    protected String getMainComponentName() {
        return XMPPEventConstants.COMPONENT_MANE;
    }

    @Override
    protected boolean getUseDeveloperSupport() {
        return BuildConfig.DEBUG;
    }

    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.asList(
                new MainReactPackage(),
                new XMPPReactPackage()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        startService(new Intent(this, XMPPService.class));
    }
}
