package com.pusherwebsocket.pusher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** PusherPlugin */
public class PusherPlugin implements MethodCallHandler {
  static String TAG = "PusherPlugin";

  private static Pusher pusher;
  private static Map<String, Channel> channels = new HashMap<>();
  private static EventListener eventListener;

  static EventChannel.EventSink eventSinks;
  static boolean isLoggingEnabled = false;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "pusher");
    EventChannel eventStream = new EventChannel(registrar.messenger(), "pusherStream");
    eventListener = new EventListener();

    channel.setMethodCallHandler(new PusherPlugin());
    eventStream.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object args, final EventChannel.EventSink events) {
        eventSinks = events;
      }

      @Override
      public void onCancel(Object args) {
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
      }
    }

    try {
      JSONObject json = new JSONObject(call.arguments.toString());
      JSONObject options  = json.getJSONObject("options");

      if (json.has("isLoggingEnabled")) {
        isLoggingEnabled = json.getBoolean("isLoggingEnabled");
      }

      // setup options
      PusherOptions pusherOptions = new PusherOptions();
      if (options.has("cluster")) {
        pusherOptions.setCluster(options.getString("cluster"));
      }

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
    channels.put(channelName, pusher.subscribe(channelName));
    if (isLoggingEnabled) {
      Log.d(TAG, "subscribe");
    }
    result.success(null);
  }

  private void unsubscribe(MethodCall call, Result result) {
    String channelName = call.arguments.toString();
    pusher.unsubscribe(call.arguments.toString());
    channels.remove(channelName);
    if (isLoggingEnabled) {
      Log.d(TAG, "unsubscribe");
    }
    result.success(null);
  }

  private void bind(MethodCall call, Result result) {
    try {
      JSONObject json = new JSONObject(call.arguments.toString());
      String channelName = json.getString("channelName");
      String eventName = json.getString("eventName");

      Channel channel = channels.get(channelName);
      channel.bind(eventName, eventListener);

      if (isLoggingEnabled) {
        Log.d(TAG, "bind");
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
      String eventName = json.getString("eventName");

      Channel channel = channels.get(channelName);
      channel.unbind(eventName, eventListener);

      if (isLoggingEnabled) {
        Log.d(TAG, "bind");
      }
      result.success(null);
    } catch (Exception e) {
      if (isLoggingEnabled) {
        Log.d(TAG, "unbind error: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }
}

class EventListener implements SubscriptionEventListener {

  @Override
  public void onEvent(final String channelName, final String eventName, final String data) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        try {
          JSONObject eventStreamMessageJson = new JSONObject();
          JSONObject eventJson = new JSONObject();
          eventJson.put("channel", channelName);
          eventJson.put("event", eventName);
          eventJson.put("data", data);
          eventStreamMessageJson.put("event", eventJson);
          String eventStreamMessageJsonString = eventStreamMessageJson.toString();
          PusherPlugin.eventSinks.success(eventStreamMessageJsonString);
          if (PusherPlugin.isLoggingEnabled) {
            Log.d(PusherPlugin.TAG, "Pusher event: CH:" + channelName + " EN:" + eventName + " ED:" + eventStreamMessageJsonString);
          }
        } catch (Exception e) {
          if (PusherPlugin.isLoggingEnabled) {
            Log.d(PusherPlugin.TAG, "onEvent error: " + e.getMessage());
            e.printStackTrace();
          }
        }
      }
    });
  }
}


