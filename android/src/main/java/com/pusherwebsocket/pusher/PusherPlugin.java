package com.pusherwebsocket.pusher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.PresenceChannelEventListener;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.User;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.ConnectionFactory;
import com.pusher.client.util.HttpAuthorizer;
import com.pusher.client.util.UrlEncodedConnectionFactory;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * PusherPlugin
 */
public class PusherPlugin implements MethodCallHandler {
    private static Pusher pusher;
    private static Map<String, Channel> channels = new HashMap<>();
    private static EventChannelListener eventListener;
    private static PrivateChannelChannelListener eventListenerPrivate;
    private static PresenceChannelEventListener eventListenerPresence;

    static String TAG = "PusherPlugin";
    static EventChannel.EventSink eventSink;
    static boolean isLoggingEnabled = false;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "pusher");
        final EventChannel eventStream = new EventChannel(registrar.messenger(), "pusherStream");

        eventListener = new EventChannelListener();
        eventListenerPrivate = new PrivateChannelChannelListener();
        eventListenerPresence = new PresenceChannelChannelListener();

        channel.setMethodCallHandler(new PusherPlugin());
        eventStream.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object args, final EventChannel.EventSink eventSink) {
                PusherPlugin.eventSink = eventSink;
            }

            @Override
            public void onCancel(Object args) {
                Log.d(TAG, String.format("onCancel args: %s", args != null ? args.toString() : "null"));
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "init":
                init(call, result);
                break;
            case "connect":
                connect(call, result);
                break;
            case "disconnect":
                disconnect(call, result);
                break;
            case "subscribe":
                subscribe(call, result);
                break;
            case "unsubscribe":
                unsubscribe(call, result);
                break;
            case "bind":
                bind(call, result);
                break;
            case "unbind":
                unbind(call, result);
                break;
            case "getSocketId":
                getSocketId(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void init(MethodCall call, Result result) {
        if (pusher != null) {
            for (Map.Entry<String, Channel> entry : channels.entrySet()) {
                String name = entry.getKey();
                pusher.unsubscribe(name);
                channels.remove(name);
            }
        }

        try {
            final JSONObject json = new JSONObject(call.arguments.toString());
            final JSONObject options = json.getJSONObject("options");

            if (json.has("isLoggingEnabled")) {
                isLoggingEnabled = json.getBoolean("isLoggingEnabled");
            }

            // setup options
            final PusherOptions pusherOptions = new PusherOptions();

            if (options.has("auth")) {
                final JSONObject auth = options.getJSONObject("auth");
                final String endpoint = auth.getString("endpoint");
                final Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                final Map<String, String> headers = new Gson().fromJson(auth.get("headers").toString(), mapType);

                pusherOptions.setAuthorizer(getAuthorizer(endpoint, headers));
            }

            if (options.has("activityTimeout")) {
                pusherOptions.setActivityTimeout(options.getInt("activityTimeout"));
            }
            if (options.has("cluster")) {
                pusherOptions.setCluster(options.getString("cluster"));
            }
            if (options.has("host")) {
                pusherOptions.setHost(options.getString("host"));
            }

            // defaults to encrypted connection on port 443
            final int port = options.has("port") ? options.getInt("port") : 443;
            final boolean encrypted = !options.has("encrypted") || options.getBoolean("encrypted");

            if (encrypted) {
                pusherOptions.setWssPort(port);
            } else {
                pusherOptions.setWsPort(port);
            }
            pusherOptions.setEncrypted(encrypted);


            // create client
            pusher = new Pusher(json.getString("appKey"), pusherOptions);

            if (isLoggingEnabled) {
                Log.d(TAG, "init");
            }
            result.success(null);
        } catch (Exception e) {
            if (isLoggingEnabled) {
                Log.d(TAG, "init error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static HttpAuthorizer getAuthorizer(String endpoint, Map<String, String> headers) {
        final ConnectionFactory connection = headers.containsValue("application/json") ?
                new JsonEncodedConnectionFactory() :
                new UrlEncodedConnectionFactory();

        final HttpAuthorizer authorizer = new HttpAuthorizer(endpoint, connection);
        authorizer.setHeaders(headers);

        return authorizer;

    }

    private void connect(MethodCall call, Result result) {
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(final ConnectionStateChange change) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final JSONObject eventStreamMessageJson = new JSONObject();
                            final JSONObject connectionStateChangeJson = new JSONObject();
                            connectionStateChangeJson.put("currentState", change.getCurrentState().toString());
                            connectionStateChangeJson.put("previousState", change.getPreviousState().toString());
                            eventStreamMessageJson.put("connectionStateChange", connectionStateChangeJson);
                            eventSink.success(eventStreamMessageJson.toString());
                        } catch (Exception e) {
                            if (isLoggingEnabled) {
                                Log.d(TAG, "onConnectionStateChange error: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(final String message, final String code, final Exception ex) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String exMessage = ex != null ? ex.getMessage() : null;
                            final JSONObject eventStreamMessageJson = new JSONObject();
                            final JSONObject connectionErrorJson = new JSONObject();

                            connectionErrorJson.put("message", message);
                            connectionErrorJson.put("code", code);
                            connectionErrorJson.put("exception", exMessage);
                            eventStreamMessageJson.put("connectionError", connectionErrorJson);

                            eventSink.success(eventStreamMessageJson.toString());

                        } catch (Exception e) {
                            if (isLoggingEnabled) {
                                Log.d(TAG, "onError exception: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

        }, ConnectionState.ALL);

        if (isLoggingEnabled) {
            Log.d(TAG, "connect");
        }
        result.success(null);
    }

    private void disconnect(MethodCall call, Result result) {
        pusher.disconnect();
        if (isLoggingEnabled) {
            Log.d(TAG, "disconnect");
        }
        result.success(null);
    }

    private void subscribe(MethodCall call, Result result) {
        final String channelName = call.arguments.toString();
        final String channelType = channelName.split("-")[0];
        Channel channel = channels.get(channelName);

        if (channel != null && channel.isSubscribed()) {
            if (isLoggingEnabled) {
                Log.d(TAG, "Already subscribed, ignoring ...");
            }
            result.success(null);
            return;
        }

        switch (channelType) {
            case "private":
                channel = pusher.subscribePrivate(channelName, eventListenerPrivate);
                if (isLoggingEnabled) {
                    Log.d(TAG, "subscribe (private)");
                }
                break;
            case "presence":
                channel = pusher.subscribePresence(channelName, eventListenerPresence);
                if (isLoggingEnabled) {
                    Log.d(TAG, "subscribe (presence)");
                }
                break;
            default:
                channel = pusher.subscribe(channelName, eventListener);

                if (isLoggingEnabled) {
                    Log.d(TAG, "subscribe");
                }
                break;
        }

        channels.put(channelName, channel);
        result.success(null);
    }

    private void unsubscribe(MethodCall call, Result result) {
        final String channelName = call.arguments.toString();
        pusher.unsubscribe(call.arguments.toString());
        channels.remove(channelName);

        if (isLoggingEnabled) {
            Log.d(TAG, String.format("unsubscribe (%s)", channelName));
        }
        result.success(null);
    }

    private void bind(MethodCall call, Result result) {
        try {
            final JSONObject json = new JSONObject(call.arguments.toString());
            final String channelName = json.getString("channelName");
            final String channelType = channelName.split("-")[0];
            final String eventName = json.getString("eventName");

            Channel channel = channels.get(channelName);

            switch (channelType) {
                case "private":
                    channel.bind(eventName, eventListenerPrivate);
                    break;
                case "presence":
                    channel.bind(eventName, eventListenerPresence);
                    break;
                default:
                    channel.bind(eventName, eventListener);
                    break;
            }

            if (isLoggingEnabled) {
                Log.d(TAG, String.format("bind (%s)", eventName));
            }
            result.success(null);
        } catch (Exception e) {
            if (isLoggingEnabled) {
                Log.d(TAG, String.format("bind exception: %s", e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    private void unbind(MethodCall call, Result result) {
        try {
            final JSONObject json = new JSONObject(call.arguments.toString());
            final String channelName = json.getString("channelName");
            final String channelType = channelName.split("-")[0];
            final String eventName = json.getString("eventName");

            Channel channel = channels.get(channelName);
            switch (channelType) {
                case "private":
                    channel.unbind(eventName, eventListenerPrivate);
                    break;
                case "presence":
                    channel.unbind(eventName, eventListenerPresence);
                    break;
                default:
                    channel.unbind(eventName, eventListener);
                    break;
            }

            if (isLoggingEnabled) {
                Log.d(TAG, String.format("unbind (%s)", eventName));
            }
            result.success(null);
        } catch (Exception e) {
            if (isLoggingEnabled) {
                Log.d(TAG, String.format("unbind exception: %s", e.getMessage()));
                e.printStackTrace();
            }
        }
    }
    
    private void getSocketId(MethodCall call, MethodChannel.Result result) {
        result.success(pusher.getConnection().getSocketId());
    }
}


class JsonEncodedConnectionFactory extends ConnectionFactory {

    JsonEncodedConnectionFactory() {
    }

    @Override
    public String getCharset() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    public String getBody() {
        JsonObject payload = new JsonObject();
        payload.add("channel_name", new JsonPrimitive(getChannelName()));
        payload.add("socket_id", new JsonPrimitive(getSocketId()));

        return payload.toString();
    }
}

class EventChannelListener implements ChannelEventListener {
    static final String SUBSCRIPTION_SUCCESS_EVENT = "pusher:subscription_succeeded";
    static final String MEMBER_ADDED_EVENT = "pusher:member_added";
    static final String MEMBER_REMOVED_EVENT = "pusher:member_removed";

    static PusherEvent toPusherEvent(String channel, String event, String userId, String data) {
        final Map<String, Object> eventData = new HashMap<>();

        eventData.put("channel", channel);
        eventData.put("event", event);
        eventData.put("data", data != null ? data : "");
        if (userId != null) {
            eventData.put("user_id", userId);
        }

        return new PusherEvent(eventData);
    }

    @Override
    public void onEvent(final PusherEvent pusherEvent) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject eventStreamMessageJson = new JSONObject();
                    final JSONObject eventJson = new JSONObject();
                    final String channel = pusherEvent.getChannelName();
                    final String event = pusherEvent.getEventName();
                    final String data = pusherEvent.getData();

                    eventJson.put("channel", channel);
                    eventJson.put("event", event);
                    eventJson.put("data", data);
                    eventStreamMessageJson.put("event", eventJson);

                    PusherPlugin.eventSink.success(eventStreamMessageJson.toString());

                    if (PusherPlugin.isLoggingEnabled) {
                        Log.d(PusherPlugin.TAG, String.format("onEvent: \nCHANNEL: %s \nEVENT: %s \nDATA: %s", channel, event, data));
                    }
                } catch (Exception e) {
                    onError(e);
                }
            }
        });

    }

    void onError(final Exception e) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject eventStreamMessageJson = new JSONObject();
                    JSONObject connectionErrorJson = new JSONObject();
                    connectionErrorJson.put("message", e.getMessage());
                    connectionErrorJson.put("code", "Channel error");
                    connectionErrorJson.put("exception", e);
                    eventStreamMessageJson.put("connectionError", connectionErrorJson);

                    PusherPlugin.eventSink.success(eventStreamMessageJson.toString());

                    if (PusherPlugin.isLoggingEnabled) {
                        Log.d(PusherPlugin.TAG, "onError : " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (Exception ex) {
                    if (PusherPlugin.isLoggingEnabled) {
                        Log.d(PusherPlugin.TAG, "onError exception: " + e.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onSubscriptionSucceeded(String channelName) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, null));
    }
}

class PresenceChannelChannelListener extends EventChannelListener implements PresenceChannelEventListener {

    @Override
    public void onSubscriptionSucceeded(String channelName) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, null));
    }

    @Override
    public void onAuthenticationFailure(String message, Exception e) {
        onError(e);
    }

    @Override
    public void onUsersInformationReceived(String channelName, Set<User> users) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, users.toString()));
    }

    @Override
    public void userSubscribed(String channelName, User user) {
        this.onEvent(toPusherEvent(channelName, MEMBER_ADDED_EVENT, user.getId(), null));
    }

    @Override
    public void userUnsubscribed(String channelName, User user) {
        this.onEvent(toPusherEvent(channelName, MEMBER_REMOVED_EVENT, user.getId(), null));
    }
}

class PrivateChannelChannelListener extends EventChannelListener implements PrivateChannelEventListener {

    @Override
    public void onSubscriptionSucceeded(String channelName) {
        this.onEvent(toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT, null, null));
    }

    @Override
    public void onAuthenticationFailure(String message, Exception e) {
        onError(e);
    }

}
