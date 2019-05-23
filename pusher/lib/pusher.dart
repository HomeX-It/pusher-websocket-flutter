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
    final initArgs = jsonEncode(_InitArgs(appKey, options).toJson());
    await _channel.invokeMethod('init', initArgs);
  }

  /// Connect the client to pusher
  static Future connect(
      {Function(ConnectionStateChange) onConnectionStateChange,
      Function(ConnectionError) onError}) async {
    await _channel.invokeMethod('connect');
  }

  /// Disconnect the client from pusher
  static Future disconnect() async {
    await _channel.invokeMethod('disconnect');
  }

  /// Subscribe to a channel
  /// Use the returned [Channel] to bind events
  static Future<Channel> subscribe(String channelName) async {
    await _channel.invokeMethod('subscribe', channelName);
    return Channel(name: channelName);
  }

  /// Subscribe to a channel
  /// Use the returned [Channel] to bind events
  static Future unsubscribe(String channelName) async {
    await _channel.invokeMethod('unsubscribe', channelName);
  }

  static Future _bind(String channelName, String eventName,
      {Function(Event) onEvent}) async {
    final bindArgs = jsonEncode(
        _BindArgs(channelName: channelName, eventName: eventName).toJson());
    await _channel.invokeMethod('bind', bindArgs);
  }
}

@JsonSerializable()
class _InitArgs {
  String appKey;
  PusherOptions options;

  _InitArgs(this.appKey, this.options);

  factory _InitArgs.fromJson(Map<String, dynamic> json) =>
      _$_InitArgsFromJson(json);

  Map<String, dynamic> toJson() => _$_InitArgsToJson(this);
}

@JsonSerializable()
class _BindArgs {
  String channelName;
  String eventName;

  _BindArgs({this.channelName, this.eventName});

  factory _BindArgs.fromJson(Map<String, dynamic> json) =>
      _$_BindArgsFromJson(json);

  Map<String, dynamic> toJson() => _$_BindArgsToJson(this);
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
  String name;

  Channel({this.name});

  Future bind(String eventName, Function(Event) onEvent) async {
    await Pusher._bind(name, eventName);
  }

  void unbind(String eventName) {}
}
