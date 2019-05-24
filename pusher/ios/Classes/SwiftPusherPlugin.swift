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
        case "connect":
            connect(call, result: result)
        case "disconnect":
            disconnect(call, result: result)
        case "subscribe":
            subscribe(call, result: result)
        case "unsubscribe":
            unsubscribe(call, result: result)
        case "bind":
            bind(call, result: result)
        case "unbind":
            unbind(call, result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
        
        
        let pusher = Pusher(key: "APP_KEY")
    }
    
    public func setup(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
    
    public func connect(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
    
    public func disconnect(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
    
    public func subscribe(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
    
    public func unsubscribe(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
    
    public func bind(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
    
    public func unbind(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
    }
}
