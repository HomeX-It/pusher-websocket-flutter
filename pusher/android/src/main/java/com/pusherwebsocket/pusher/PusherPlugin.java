package com.pusherwebsocket.pusher;

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

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** PusherPlugin */
public class PusherPlugin implements MethodCallHandler {

  private Pusher pusher;
  private Map<String, Channel> channels = new HashMap<>();

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "pusher");
    channel.setMethodCallHandler(new PusherPlugin());
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("init")) {
      init(call, result);
    } else if (call.method.equals("connect")) {
      connect(call, result);
    } else if (call.method.equals("disconnect")) {
      disconnect(call, result);
    } else if (call.method.equals("subscribe")) {
      subscribe(call, result);
    } else if (call.method.equals("unsubscribe")) {
      unsubscribe(call, result);
    } else {
      result.notImplemented();
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
