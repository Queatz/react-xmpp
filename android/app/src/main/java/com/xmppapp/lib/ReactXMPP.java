/**
 * Created by Jacob Ferrero on 2/16/16.
 */

package com.xmppapp.lib;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.xmppapp.MainApplication;
import com.xmppapp.XMPPController;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;

import javax.annotation.Nonnull;

/**
 * The module exposed to React.
 */
public class ReactXMPP extends ReactContextBaseJavaModule {

    private static final String LOGGER = "ReactXMPP";

    DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    XMPPController xmppController;

    public ReactXMPP(ReactApplicationContext reactContext) {
        super(reactContext);

        xmppController = ((MainApplication) reactContext.getApplicationContext()).xmppController;
    }

    @Override
    public String getName() {
        return "ReactXMPP";
    }

    /**
     * Connect to an XMPP server
     *
     * @param username
     * @param password
     * @param server
     * @return true or false depending on if there were any errors thrown
     */
    @ReactMethod
    public void connect(
            final String username,
            final String password,
            final String server,
            final String securityMode
    ) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(server)) {
            sendReactEvent(XMPPEventConstants.XMPP_EVENT_LOGIN_ERROR, "Missing server");
            return;
        }

        xmppController.setCallbackHandler(new XMPPController.XMPPCallbackHandler() {
            @Override
            public void onEvent(@Nonnull String eventName, @Nullable Object params) {
                sendReactEvent(eventName, params);
            }
        });

        xmppController.connect(username, password, server, securityModeFromString(securityMode));
    }

    @ReactMethod
    public void disconnect() {
        xmppController.disconnect();
    }

    @ReactMethod
    public void message(final String message, final String withJid) {
        xmppController.message(message, withJid);
    }

    private ConnectionConfiguration.SecurityMode securityModeFromString(@Nullable final String securityMode) {
        if (securityMode == null) {
            return ConnectionConfiguration.SecurityMode.ifpossible;
        }

        try {
            return ConnectionConfiguration.SecurityMode.valueOf(securityMode);
        } catch (IllegalArgumentException e) {
            Log.e(LOGGER, "Unknwon security mode: " + securityMode);
            return ConnectionConfiguration.SecurityMode.ifpossible;
        }
    }

    private void sendReactEvent(String eventName, @Nullable Object params) {
        eventEmitter = getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        eventEmitter.emit(eventName, params);
    }
}