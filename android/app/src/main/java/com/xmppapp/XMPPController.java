package com.xmppapp;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.xmppapp.lib.XMPPEventConstants;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.util.XmppStringUtils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by jacob on 2/22/16.
 */
public class XMPPController implements ChatMessageListener, ChatManagerListener {
    Context context;
    AbstractXMPPConnection connection;
    ChatManager chatManager;
    XMPPCallbackHandler callbackHandler;

    private static final String LOGGER = "XMPPController";

    private static final HostnameVerifier DUMMY_VERIFIER = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private static TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }
    };

    public interface XMPPCallbackHandler {
        void onEvent(@Nonnull String eventName, @Nullable Object params);
    }

    public XMPPController(Context context) {
        this.context = context;
    }

    public void setCallbackHandler(XMPPCallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    private void sendEvent(@Nonnull String eventName, @Nullable Object params) {
        if (this.callbackHandler != null) {
            this.callbackHandler.onEvent(eventName, params);
        }
    }

//    NOTE: Uncomment this code to use signed SSL certificates
//
//    private boolean setup() {
//        Resources res = context.getResources();
//        String packageName = context.getApplicationContext().getPackageName();
//        int id = res.getIdentifier("raw/keystore", "raw", packageName);
//        InputStream ins = res.openRawResource(id);
//
//        try {
//            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//            ks.load(ins, "android".toCharArray());
//
//            trustManagerFactory =
//                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            trustManagerFactory.init(ks);
//
//            return true;
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        } catch (CertificateException e) {
//            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Resources.NotFoundException e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }

    public void connect(String username, String password, String server, ConnectionConfiguration.SecurityMode securityMode) {

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { DUMMY_TRUST_MANAGER }, new SecureRandom());

            XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(username, password)
                    .setHost(server)
                    .setSecurityMode(securityMode)
                    .setCustomSSLContext(sslContext)
                    .setHostnameVerifier(DUMMY_VERIFIER)
                    .setServiceName(server);

            connection = new XMPPTCPConnection(configBuilder.build());
        } catch (NoSuchAlgorithmException e) {
            sendEvent(XMPPEventConstants.XMPP_EVENT_LOGIN_ERROR, "SSL Error: " + e);
            return;
        } catch (KeyManagementException  e) {
            sendEvent(XMPPEventConstants.XMPP_EVENT_LOGIN_ERROR, "Key Error: " + e);
            return;
        }

        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected(XMPPConnection c) {
                sendEvent(XMPPEventConstants.XMPP_EVENT_CONNECT, null);

                chatManager = ChatManager.getInstanceFor(connection);
                chatManager.addChatListener(XMPPController.this);
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                sendEvent(XMPPEventConstants.XMPP_EVENT_LOGIN, null);
            }

            @Override
            public void connectionClosed() {
                sendEvent(XMPPEventConstants.XMPP_EVENT_DISCONNECT, null);
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

        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                WritableMap map = new WritableNativeMap();
                map.putString(XMPPEventConstants.REACT_EVENT_PARAM_FROM, getUsername(packet.getFrom()));
                map.putString(XMPPEventConstants.REACT_EVENT_PARAM_STANZA, packet.getStanzaId());
                sendEvent(XMPPEventConstants.XMPP_EVENT_PRESENCE, map);
            }
        }, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                return true;
            }
        });

        connection.registerIQRequestHandler(new IQRequestHandler() {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                WritableMap map = new WritableNativeMap();
                map.putString(XMPPEventConstants.REACT_EVENT_PARAM_FROM, getUsername(iqRequest.getFrom()));
                map.putString(XMPPEventConstants.REACT_EVENT_PARAM_IQ, iqRequest.getChildElementXML().toString());
                sendEvent(XMPPEventConstants.XMPP_EVENT_IQ, map);

                return iqRequest;
            }

            @Override
            public Mode getMode() {
                return null;
            }

            @Override
            public IQ.Type getType() {
                return null;
            }

            @Override
            public String getElement() {
                return null;
            }

            @Override
            public String getNamespace() {
                return null;
            }
        });

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void[] voids) {

                try {
                    connection.connect().login();
                } catch (SmackException | IOException | XMPPException e) {
                    Log.w(LOGGER, "Can't connect to XMPP: ", e);

                    sendEvent(XMPPEventConstants.XMPP_EVENT_LOGIN_ERROR, e.getLocalizedMessage());
                }

                return null;
            }
        }.execute();
    }

    public void message(final String message, final String withJid) {
        final String username = XmppStringUtils.completeJidFrom(
                withJid,
                connection.getServiceName(),
                connection.getConfiguration().getResource()
        );

        Log.d(LOGGER, "Sending message from " + connection.getUser() + " to " + username + "...");

        if (connection == null || !connection.isConnected()) {
            Log.w(LOGGER, "Couldn't send message: no connection");
            sendEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE_ERROR, "No connection");
            return;
        }

        if (!connection.isAuthenticated()) {
            Log.w(LOGGER, "Couldn't send message: not authenticated");
            sendEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE_ERROR, "Not authenticated");
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void[] voids) {

                try {
                    chatManager.createChat(username, XMPPController.this).sendMessage(message);
                } catch (SmackException e) {
                    Log.w(LOGGER, "Couldn't send message", e);
                    sendEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE_ERROR, e.getLocalizedMessage());
                }

                return null;
            }
        }.execute();
    }


    public void disconnect() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    public void close() {
        disconnect();
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        Log.d(LOGGER, "Chat created with " + chat.getParticipant() + "...");

        if (!createdLocally) {
            chat.addMessageListener(XMPPController.this);
        }
    }

    @Override
    public void processMessage(final Chat chat, final Message message) {
        Log.d(LOGGER, "Got message from " + message.getFrom() + "...");

        WritableMap map = new WritableNativeMap();
        map.putString(XMPPEventConstants.REACT_EVENT_PARAM_FROM, getUsername(message.getFrom()));
        map.putString(XMPPEventConstants.REACT_EVENT_PARAM_MESSAGE_BODY, message.getBody());
        sendEvent(XMPPEventConstants.XMPP_EVENT_MESSAGE, map);
    }

    private String getUsername(String jid) {
        return XmppStringUtils.parseLocalpart(jid);
    }
}
