#import "PusherPlugin.h"
#import <pusher/pusher-Swift.h>

@implementation PusherPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPusherPlugin registerWithRegistrar:registrar];
}
@end
