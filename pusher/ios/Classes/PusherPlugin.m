#import "PusherPlugin.h"
#import <pusher_websocket_flutter/pusher_websocket_flutter-Swift.h>

@implementation PusherPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPusherPlugin registerWithRegistrar:registrar];
}
@end
