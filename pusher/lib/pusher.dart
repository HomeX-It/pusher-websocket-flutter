import 'dart:async';

import 'package:flutter/services.dart';

class Pusher {
  static const MethodChannel _channel =
      const MethodChannel('pusher');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
