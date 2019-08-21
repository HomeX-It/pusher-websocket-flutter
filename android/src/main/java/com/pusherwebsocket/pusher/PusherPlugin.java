package com.pusherwebsocket.pusher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

import org.json.JSONObject;

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
    static String TAG = "PusherPlugin";

    private static Pusher pusher;
    private static Map<String, Channel> channels = new HashMap<>();
    private static EventChannelListener eventListener;
    private static PrivateChannelChannelListener eventListenerPrivate;
    private static PresenceChannelEventListener eventListenerPresence;

    static EventChannel.EventSink eventSinks;
    static boolean isLoggingEnabled = false;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "pusher");
        EventChannel eventStream = new EventChannel(registrar.messenger(), "pusherStream");
        eventListener = new EventChannelListener();
        eventListenerPrivate = new PrivateChannelChannelListener();
        eventListenerPresence = new PresenceChannelChannelListener();

        channel.setMethodCallHandler(new PusherPlugin());
        eventStream.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object args, final EventChannel.EventSink events) {
                eventSinks = events;
            }

            @Override
            public void onCancel(Object args) {
                Log.d(TAG, String.format("onCancel args: [%s]", args.toString()));
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
            JSONObject json = new JSONObject(call.arguments.toString());
            JSONObject options = json.getJSONObject("options");

            if (json.has("isLoggingEnabled")) {
                isLoggingEnabled = json.getBoolean("isLoggingEnabled");
            }

            // setup options
            PusherOptions pusherOptions = new PusherOptions();

            if (options.has("authEndpoint") && options.has("authHeaders")) {
                HttpAuthorizer authorizer = new HttpAuthorizer(options.getString("authEndpoint"), new JsonEncodedConnectionFactory());
                HashMap<String, String> headers = new Gson().fromJson(options.get("authHeaders").toString(), HashMap.class);
                authorizer.setHeaders(headers);

                pusherOptions.setAuthorizer(authorizer);
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
            pusherOptions.setMaxReconnectGapInSeconds(5);

            // defaults to encrypted connection on port 443
            int port = options.has("port") ? options.getInt("port") : 443;
            boolean encrypted = !options.has("encrypted") || options.getBoolean("encrypted");

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

    private void connect(MethodCall call, Result result) {
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(final ConnectionStateChange change) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject eventStreamMessageJson = new JSONObject();
                            JSONObject connectionStateChangeJson = new JSONObject();
                            connectionStateChangeJson.put("currentState", change.getCurrentState().toString());
                            connectionStateChangeJson.put("previousState", change.getPreviousState().toString());
                            eventStreamMessageJson.put("connectionStateChange", connectionStateChangeJson);
                            eventSinks.success(eventStreamMessageJson.toString());
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
                            String exMessage = null;
                            if (ex != null)
                                exMessage = ex.getMessage();

                            JSONObject eventStreamMessageJson = new JSONObject();
                            JSONObject connectionErrorJson = new JSONObject();
                            connectionErrorJson.put("message", message);
                            connectionErrorJson.put("code", code);
                            connectionErrorJson.put("exception", exMessage);
                            eventStreamMessageJson.put("connectionError", connectionErrorJson);
                            eventSinks.success(eventStreamMessageJson.toString());
                        } catch (Exception e) {
                            for (Map.Entry<String, Channel> entry : channels.entrySet()) {
                                String name = entry.getKey();
                                pusher.unsubscribe(name);
                                channels.remove(name);
                            }
                            if (isLoggingEnabled) {
                                Log.d(TAG, "onError error: " + e.getMessage());
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
        String channelName = call.arguments.toString();
        if (channels.containsKey(channelName)) {
            pusher.unsubscribe(channelName);
            channels.remove(channelName);
            if (isLoggingEnabled) {
                Log.d(TAG, String.format("Was already subscribed, unsubscribing: [%s]", channelName));
            }
        }
        String channelType = channelName.split("-")[0];
        Channel channel;

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
        String channelName = call.arguments.toString();
        pusher.unsubscribe(call.arguments.toString());
        channels.remove(channelName);
        if (isLoggingEnabled) {
            Log.d(TAG, String.format("unsubscribe ([%s])", channelName));
        }
        result.success(null);
    }

    private void bind(MethodCall call, Result result) {
        try {
            JSONObject json = new JSONObject(call.arguments.toString());
            String channelName = json.getString("channelName");
            String channelType = channelName.split("-")[0];
            String eventName = json.getString("eventName");

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
                Log.d(TAG, String.format("bind ([%s])", eventName));
            }
            result.success(null);
        } catch (Exception e) {
            if (isLoggingEnabled) {
                Log.d(TAG, "bind error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void unbind(MethodCall call, Result result) {
        try {
            JSONObject json = new JSONObject(call.arguments.toString());
            String channelName = json.getString("channelName");
            String channelType = channelName.split("-")[0];
            String eventName = json.getString("eventName");

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
                Log.d(TAG, String.format("unbind ([%s])", eventName));
            }
            result.success(null);
        } catch (Exception e) {
            if (isLoggingEnabled) {
                Log.d(TAG, "unbind error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static PusherEvent toPusherEvent(String channel, String event){
        Map<String, Object> eventData = new HashMap<>();

        eventData.put("channel", channel);
        eventData.put("event", event);
        eventData.put("data", "");

        return new PusherEvent(eventData);
    }

    public static PusherEvent toPusherEvent(String channel, String event, String userId){
        Map<String, Object> eventData = new HashMap<>();

            eventData.put("user_id", userId);
            eventData.put("channel", channel);
            eventData.put("event", event);
            eventData.put("data", "");


        return new PusherEvent(eventData);
    }
}


class JsonEncodedConnectionFactory extends ConnectionFactory {

    /**
     * Create a Form URL-encoded factory
     */
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
        HashMap<String, String> map = new HashMap<>();
        payload.add("channel_name", new JsonPrimitive(getChannelName()));
        payload.add("socket_id", new JsonPrimitive(getSocketId()));

        return payload.toString();
    }
}

class EventChannelListener implements ChannelEventListener {
    static final String SUBSCRIPTION_SUCCESS_EVENT = "pusher_internal:subscription_succeeded";
    static final String MEMBER_ADDED_EVENT = "pusher_internal:member_added";
    static final String MEMBER_REMOVED_EVENT = "pusher_internal:member_removed";

    @Override
    public void onEvent(final PusherEvent pusherEvent) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject eventStreamMessageJson = new JSONObject();
                    JSONObject eventJson = new JSONObject();
                    eventJson.put("channel", pusherEvent.getChannelName());
                    eventJson.put("event", pusherEvent.getEventName());
                    eventJson.put("data", pusherEvent.getData());
                    eventStreamMessageJson.put("event", eventJson);
                    String eventStreamMessageJsonString = eventStreamMessageJson.toString();

                    PusherPlugin.eventSinks.success(eventStreamMessageJsonString);

                    if (PusherPlugin.isLoggingEnabled) {
                        Log.d(PusherPlugin.TAG, "Pusher event: CH:" + pusherEvent.getChannelName() + " EN:" + pusherEvent.getEventName() + " ED:" + eventStreamMessageJsonString);
                    }
                } catch (Exception e) {
                   onError(e);
                }
            }
        });
    }

    void onError(Exception e){
        PusherPlugin.eventSinks.error("Pusher event error", e.getMessage(), e);

        if (PusherPlugin.isLoggingEnabled) {
            Log.d(PusherPlugin.TAG, "Pusher event error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onSubscriptionSucceeded(String channelName) {
        this.onEvent(PusherPlugin.toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT));
    }
}

class PresenceChannelChannelListener extends EventChannelListener implements PresenceChannelEventListener {

    @Override
    public void onSubscriptionSucceeded(String channelName) {
        this.onEvent(PusherPlugin.toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT));
    }

    @Override
    public void onAuthenticationFailure(String message, Exception e) {
        // TODO
    }

    @Override
    public void onUsersInformationReceived(String channelName, Set<User> users) {
        this.onEvent(PusherPlugin.toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT));
    }

    @Override
    public void userSubscribed(String channelName, User user) {
        this.onEvent(PusherPlugin.toPusherEvent(channelName, MEMBER_ADDED_EVENT));
    }

    @Override
    public void userUnsubscribed(String channelName, User user) {
        this.onEvent(PusherPlugin.toPusherEvent(channelName, MEMBER_REMOVED_EVENT));
    }
}

class PrivateChannelChannelListener extends EventChannelListener implements PrivateChannelEventListener {

    @Override
    public void onSubscriptionSucceeded(String channelName) {
        this.onEvent(PusherPlugin.toPusherEvent(channelName, SUBSCRIPTION_SUCCESS_EVENT));
    }

    @Override
    public void onAuthenticationFailure(String message, Exception e) {
       // TODO
    }

}
