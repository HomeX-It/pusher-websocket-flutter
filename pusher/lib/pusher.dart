import 'package:flutter/services.dart';

class Pusher {
  Pusher._();

  static const MethodChannel _channel = const MethodChannel('pusher');
  static Map<String, Channel> _subscriptions = Map<String, Channel>();

  /// Your pusher.com app key
  static String appKey;

  /// Pusher options. Example: the cluster to connect to
  static PusherOptions options;

  //todo remove
  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Connect the client to pusher
  static Future connect(
      {Function(ConnectionStateChange) onConnectionStateChange,
      Function(ConnectionError) onError}) async {}

  /// Disconnect the client from pusher
  static Future disconnect() async {}

  /// Subscribe to a channel
  /// Use the returned [Channel] to bind events
  static Future<Channel> subscribe(String channelName) async {}
}

class PusherOptions {
  String cluster;
}

class ConnectionStateChange {}

class ConnectionError {}

class Event {
  String channel;
  String event;
  String data;
}

class Channel {
  void bind(String eventName, Function(Event) onEvent) {}
  void unbind(String eventName) {}
}
