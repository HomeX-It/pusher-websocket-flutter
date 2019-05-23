package com.pusherwebsocket.pusher;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;

import org.json.JSONException;
import org.json.JSONObject;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** PusherPlugin */
public class PusherPlugin implements MethodCallHandler {

  private Pusher pusher;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "pusher");
    channel.setMethodCallHandler(new PusherPlugin());
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("init")) {
      handleInit(call, result);
    } else {
      result.notImplemented();
    }
  }

  private void handleInit(MethodCall call, Result result) {
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

    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
}
