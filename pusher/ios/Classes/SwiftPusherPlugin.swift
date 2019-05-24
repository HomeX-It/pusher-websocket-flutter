import Flutter
import UIKit
import PusherSwift

public class SwiftPusherPlugin: NSObject, FlutterPlugin, PusherDelegate {
    
    var pusher: Pusher?
    var channels = [String:PusherChannel]()
    var bindedEvents = [String:String]()
    var eventChannel: FlutterEventChannel?
    
    public static var eventSink: FlutterEventSink?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "pusher", binaryMessenger: registrar.messenger())
        let instance = SwiftPusherPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let eventChannel = FlutterEventChannel(name: "pusherStream", binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(StreamHandler())
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
    }
    
    public func setup(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        do {
            let json = call.arguments as! String
            let jsonDecoder = JSONDecoder()
            let setupArgs = try jsonDecoder.decode(SetupArgs.self, from: json.data(using: .utf8)!)
            
            let options = PusherClientOptions(
                host: .cluster(setupArgs.options.cluster)
            )
            
            pusher = Pusher(
                key: setupArgs.appKey,
                options: options
            )
            pusher!.connection.delegate = self
        } catch {
            print(error)
            
        }
        result(nil);
    }
    
    public func connect(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            pusherObj.connect();
        }
        result(nil);
    }
    
    public func disconnect(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            pusherObj.disconnect();
        }
        result(nil);
    }
    
    public func subscribe(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            let channelName = call.arguments as! String
            let channel = pusherObj.subscribe(channelName)
            channels[channelName] = channel;
        }
        result(nil);
    }
    
    public func unsubscribe(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            let channelName = call.arguments as! String
            pusherObj.unsubscribe(channelName)
            channels.removeValue(forKey: "channelName")
        }
        result(nil);
    }
    
    public func bind(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        do {
            let json = call.arguments as! String
            let jsonDecoder = JSONDecoder()
            let bindArgs = try jsonDecoder.decode(BindArgs.self, from: json.data(using: .utf8)!)
            
            let channel = channels[bindArgs.channelName]
            if let channelObj = channel {
                bindedEvents[bindArgs.channelName + bindArgs.eventName] = channelObj.bind(eventName: bindArgs.eventName, callback: { data in
                    do {
                        if let dataObj = data as? [String : AnyObject] {
                            let pushJsonData = try! JSONSerialization.data(withJSONObject: dataObj)
                            let pushJsonString = NSString(data: pushJsonData, encoding: String.Encoding.utf8.rawValue)
                            let event = Event(channel: bindArgs.channelName, event: bindArgs.eventName, data: pushJsonString! as String)
                            let message = PusherEventStreamMessage(event: event, connectionStateChange:  nil)
                            let jsonEncoder = JSONEncoder()
                            let jsonData = try jsonEncoder.encode(message)
                            let jsonString = String(data: jsonData, encoding: .utf8)
                            if let eventSinkObj = SwiftPusherPlugin.eventSink {
                                eventSinkObj(jsonString)
                            }
                        }
                    } catch {
                        print(error)
                    }
                })
            }
            
        } catch {
            print(error)
        }
        result(nil);
    }
    
    public func unbind(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        do {
            let json = call.arguments as! String
            let jsonDecoder = JSONDecoder()
            let bindArgs = try jsonDecoder.decode(BindArgs.self, from: json.data(using: .utf8)!)
            
            let channel = channels[bindArgs.channelName]
            if let channelObj = channel {
                let callbackId = bindedEvents[bindArgs.channelName + bindArgs.eventName]
                if let callbackIdObj = callbackId {
                    channelObj.unbind(eventName: bindArgs.channelName, callbackId: callbackIdObj)
                    bindedEvents.removeValue(forKey: bindArgs.channelName + bindArgs.eventName)
                }
            }
            
        } catch {
            print(error)
        }
        result(nil);
    }
    
    public func changedConnectionState(from old: ConnectionState, to new: ConnectionState) {
        do {
            let stateChange = ConnectionStateChange(currentState: new.stringValue(), previousState: old.stringValue())
            let message = PusherEventStreamMessage(event: nil, connectionStateChange: stateChange)
            let jsonEncoder = JSONEncoder()
            let jsonData = try jsonEncoder.encode(message)
            let jsonString = String(data: jsonData, encoding: .utf8)
            if let eventSinkObj = SwiftPusherPlugin.eventSink {
                eventSinkObj(jsonString)
            }
        } catch {
            print(error)
        }
    }
}

class StreamHandler: NSObject, FlutterStreamHandler {
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        SwiftPusherPlugin.eventSink = events
        return nil;
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        return nil;
    }
}

struct SetupArgs: Codable {
    var appKey : String
    var options : Options
}

struct Options: Codable {
    var cluster : String
}

struct PusherEventStreamMessage: Codable {
    var event: Event?
    var connectionStateChange: ConnectionStateChange?
}

struct ConnectionStateChange: Codable {
    var currentState: String
    var previousState: String
}

struct Event: Codable {
    var channel: String
    var event: String
    var data: String
}

struct BindArgs: Codable {
    var channelName: String
    var eventName: String
}
