import Flutter
import UIKit
import PusherSwift

public class SwiftPusherPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "pusher", binaryMessenger: registrar.messenger())
        let instance = SwiftPusherPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "init":
            setup(call, result: result)
        default:
            return
        }
        
        
        let pusher = Pusher(key: "APP_KEY")
    }
    
    public func setup(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
}
