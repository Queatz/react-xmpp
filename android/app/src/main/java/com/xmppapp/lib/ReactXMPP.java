/**
 * Created by Jacob Ferrero on 2/16/16.
 */

package com.xmppapp.lib;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.util.XmppStringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReactXMPP extends ReactContextBaseJavaModule implements ChatMessageListener, ChatManagerListener {

    private static final String LOGGER = "ReactXMPP";

    DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    AbstractXMPPConnection connection;
    ChatManager chatManager;

    Map<String, Callback> callbackMap;

    public ReactXMPP(ReactApplicationContext reactContext) {
        super(reactContext);

        callbackMap = new HashMap<>();
    }

    @Override
    public String getName() {
        return "ReactXMPP";
    }

    @ReactMethod
    public void on(final String event, final Callback callback) {
        callbackMap.put(event, callback);
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
    public void connect(final String username, final String password, final String server) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(server)) {
            sendReactEvent(XMPPEventConstants.XMPP_EVENT_LOGIN_ERROR, "Missing server");
            return;
        }

        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username, password)
                .setHost(server)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setServiceName(server);


        connection = new XMPPTCPConnection(configBuilder.build());
        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected(XMPPConnection c) {
                sendReactEvent(XMPPEventConstants.XMPP_EVENT_CONNECT, null);

                chatManager = ChatManager.getInstanceFor(connection);
                chatManager.addChatListener(ReactXMPP.this);
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                sendReactEvent(XMPPEventConstants.XMPP_EVENT_LOGIN, null);
            }

            @Override
            public void connectionClosed() {
                sendReactEvent(XMPPEventConstants.XMPP_EVENT_DISCONNECT, null);
            }

            @Override
            public void connectionClosedOnError(Exception e) {

            }

            @Override
            public void reconnectionSuccessful() {

            }

            @Override
            public void reconnectingIn(int seconds) {

            }

            @Override
            public void reconnectionFailed(Exception e) {

            }
        });

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void[] voids) {

                try {
                    connection.connect().login();
                } catch (SmackException | IOException | XMPPException e) {
                    Log.w(LOGGER, "Can't connect to XMPP: ", e);

                    sendReactEvent(XMPPEventConstants.XMPP_EVENT_LOGIN_ERROR, e.getLocalizedMessage());
                }

                return null;
            }
        }.execute();
    }

    @ReactMethod
    public void disconnect() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    @ReactMethod
    public void message(final String message, final String withJid) {
        final String username = XmppStringUtils.completeJidFrom(
                withJid,
                connection.getServiceName(),
                connection.getConfiguration().getResource()
        );

        Log.d(LOGGER, "Sending message from " + connection.getUser() + " to " + username + "...");

        if (connection == null || !connection.isConnected()) {
            Log.w(LOGGER, "Couldn't send message: no connection");
            sendReactEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE_ERROR, "No connection");
            return;
        }

        if (!connection.isAuthenticated()) {
            Log.w(LOGGER, "Couldn't send message: not authenticated");
            sendReactEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE_ERROR, "Not authenticated");
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void[] voids) {

                try {
                    chatManager.createChat(username, ReactXMPP.this).sendMessage(message);
                } catch (SmackException e) {
                    Log.w(LOGGER, "Couldn't send message", e);
                    sendReactEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE_ERROR, e.getLocalizedMessage());
                }

                return null;
            }
        }.execute();
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        Log.d(LOGGER, "Chat created with " + chat.getParticipant() + "...");

        if (!createdLocally) {
            chat.addMessageListener(ReactXMPP.this);
        }
    }

    @Override
    public void processMessage(final Chat chat, final Message message) {
        Log.d(LOGGER, "Got message from " + message.getFrom() + "...");

        WritableMap map = new WritableNativeMap();
        map.putString(XMPPEventConstants.REACT_EVENT_PARAM_FROM, getUsername(message.getFrom()));
        map.putString(XMPPEventConstants.REACT_EVENT_PARAM_MESSAGE_BODY, message.getBody());
        sendReactEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE, map);
    }

    private String getUsername(String jid) {
        return XmppStringUtils.parseLocalpart(jid);
    }

    private void sendReactEvent(String eventName, @Nullable Object params) {
        eventEmitter = getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        eventEmitter.emit(eventName, params);
    }
}