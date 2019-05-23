// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pusher.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_Init _$_InitFromJson(Map<String, dynamic> json) {
  return _Init(
      json['appKey'] as String,
      json['options'] == null
          ? null
          : PusherOptions.fromJson(json['options'] as Map<String, dynamic>));
}

Map<String, dynamic> _$_InitToJson(_Init instance) =>
    <String, dynamic>{'appKey': instance.appKey, 'options': instance.options};

PusherOptions _$PusherOptionsFromJson(Map<String, dynamic> json) {
  return PusherOptions(cluster: json['cluster'] as String);
}

Map<String, dynamic> _$PusherOptionsToJson(PusherOptions instance) =>
    <String, dynamic>{'cluster': instance.cluster};
