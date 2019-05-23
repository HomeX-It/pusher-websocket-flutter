import 'package:flutter/services.dart';
import 'package:json_annotation/json_annotation.dart';
import 'dart:convert';

part 'pusher.g.dart';

class Pusher {
  Pusher._();

  static const MethodChannel _channel = const MethodChannel('pusher');
  static Map<String, Channel> _subscriptions = Map<String, Channel>();

  static Future init(String appKey, PusherOptions options) async {
    assert(appKey != null);
    assert(options != null);
    final init = jsonEncode(_Init(appKey, options).toJson());
    final String version = await _channel.invokeMethod('init', init);
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

@JsonSerializable()
class _Init {
  String appKey;
  PusherOptions options;

  _Init(this.appKey, this.options);

  factory _Init.fromJson(Map<String, dynamic> json) => _$_InitFromJson(json);

  Map<String, dynamic> toJson() => _$_InitToJson(this);
}

@JsonSerializable()
class PusherOptions {
  String cluster;

  PusherOptions({this.cluster});

  factory PusherOptions.fromJson(Map<String, dynamic> json) =>
      _$PusherOptionsFromJson(json);

  Map<String, dynamic> toJson() => _$PusherOptionsToJson(this);
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
