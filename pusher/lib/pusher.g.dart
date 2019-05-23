// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pusher.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_InitArgs _$_InitArgsFromJson(Map<String, dynamic> json) {
  return _InitArgs(
      json['appKey'] as String,
      json['options'] == null
          ? null
          : PusherOptions.fromJson(json['options'] as Map<String, dynamic>));
}

Map<String, dynamic> _$_InitArgsToJson(_InitArgs instance) =>
    <String, dynamic>{'appKey': instance.appKey, 'options': instance.options};

_BindArgs _$_BindArgsFromJson(Map<String, dynamic> json) {
  return _BindArgs(
      channelName: json['channelName'] as String,
      eventName: json['eventName'] as String);
}

Map<String, dynamic> _$_BindArgsToJson(_BindArgs instance) => <String, dynamic>{
      'channelName': instance.channelName,
      'eventName': instance.eventName
    };

PusherOptions _$PusherOptionsFromJson(Map<String, dynamic> json) {
  return PusherOptions(cluster: json['cluster'] as String);
}

Map<String, dynamic> _$PusherOptionsToJson(PusherOptions instance) =>
    <String, dynamic>{'cluster': instance.cluster};
