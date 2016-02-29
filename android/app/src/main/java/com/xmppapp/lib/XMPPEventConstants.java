/**
 * Created by Jacob Ferrero on 2/16/16.
 */

package com.xmppapp.lib;

/**
 * Package globals, some of which are exposed to React.
 */
public class XMPPEventConstants {

    // Configuration
    public static final String COMPONENT_MANE = "XMPPApp";

    // React events
    public static final String REACT_EVENT_NEW_CHAT = "react.xmpp.newChat";
    public static final String REACT_EVENT_PARAM_MESSAGE_BODY = "message";
    public static final String REACT_EVENT_PARAM_FROM = "from";

    // XMPP events
    public static final String XMPP_EVENT_CONNECT = "xmppConnect";
    public static final String XMPP_EVENT_DISCONNECT = "xmppDisconnect";
    public static final String XMPP_EVENT_LOGIN_ERROR = "xmppLoginError";
    public static final String XMPP_EVENT_LOGIN = "xmppLogin";
    public static final String XMPP_EVENT_MESSAGE = "xmppMessage";
    public static final String XMPP_EVENT_MESSAGE_ERROR = "xmppMessageError";
}
