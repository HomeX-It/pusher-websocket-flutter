package com.pusherwebsocket.pusher;

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

  private Pusher pusher;
  private Map<String, Channel> channels = new HashMap<>();

  private static EventChannel.EventSink eventSinks;
  private static String tag = "FLUTTER-PUSHER";

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "pusher");
    channel.setMethodCallHandler(new PusherPlugin());

    EventChannel eventStream = new EventChannel(registrar.messenger(), "pusherStream");
    eventStream.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object args, final EventChannel.EventSink events) {
        Log.d(tag, "setStreamHandler onListen");
        eventSinks = events;
      }

      @Override
      public void onCancel(Object args) {
        Log.d(tag, "setStreamHandler onCancel");
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
      default:
        result.notImplemented();
        break;
    }
  }

  private void init(MethodCall call, Result result) {
    try {
      JSONObject json = new JSONObject(call.arguments.toString());
      JSONObject options  = json.getJSONObject("options");

      // setup options
      PusherOptions pusherOptions = new PusherOptions();
      if (options.has("cluster")) {
        pusherOptions.setCluster(options.getString("cluster"));
      }

      // create client
      pusher = new Pusher(json.getString("appKey"), pusherOptions);

      result.success(null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void connect(MethodCall call, Result result) {
    pusher.connect(new ConnectionEventListener() {
      @Override
      public void onConnectionStateChange(ConnectionStateChange change) {
        System.out.println("State changed to " + change.getCurrentState() +
                " from " + change.getPreviousState());
      }

      @Override
      public void onError(String message, String code, Exception e) {
        System.out.println("There was a problem connecting!");
      }
    }, ConnectionState.ALL);
    result.success(null);
  }

  private void disconnect(MethodCall call, Result result) {
    pusher.disconnect();
    result.success(null);
  }

  private void subscribe(MethodCall call, Result result) {
    String channelName = call.arguments.toString();
    channels.put(channelName, pusher.subscribe(channelName));
    result.success(null);
  }

  private void unsubscribe(MethodCall call, Result result) {
    String channelName = call.arguments.toString();
    pusher.unsubscribe(call.arguments.toString());
    channels.remove(channelName);
    result.success(null);
  }

  private void bind(MethodCall call, Result result) {
    try {
      JSONObject json = new JSONObject(call.arguments.toString());
      String channelName = json.getString("channelName");
      String eventName = json.getString("eventName");

      Channel channel = channels.get(channelName);

      channel.bind(eventName, new SubscriptionEventListener() {
        @Override
        public void onEvent(String channel, String event, String data) {
          System.out.println("Received event with data: " + data);
        }
      });

      result.success(null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
