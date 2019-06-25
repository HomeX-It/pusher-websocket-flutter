import Flutter
import UIKit
import PusherSwift

public class SwiftPusherPlugin: NSObject, FlutterPlugin, PusherDelegate {
    
    var pusher: Pusher?
    var isLoggingEnabled: Bool = false;
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
            let initArgs = try jsonDecoder.decode(InitArgs.self, from: json.data(using: .utf8)!)
            
            isLoggingEnabled = initArgs.isLoggingEnabled
            
            let options = PusherClientOptions(
                host: .cluster(initArgs.options.cluster)
            )
            
            pusher = Pusher(
                key: initArgs.appKey,
                options: options
            )
            pusher!.connection.delegate = self
            
            if (isLoggingEnabled) {
                print("Pusher init")
            }
        } catch {
            if (isLoggingEnabled) {
                print("Pusher init error:" + error.localizedDescription)
            }
        }
        result(nil);
    }
    
    public func connect(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            pusherObj.connect();
            if (isLoggingEnabled) {
                print("Pusher connect")
            }
        }
        result(nil);
    }
    
    public func disconnect(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            pusherObj.disconnect();
            if (isLoggingEnabled) {
                print("Pusher disconnect")
            }
        }
        result(nil);
    }
    
    public func subscribe(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            let channelName = call.arguments as! String
            let channel = pusherObj.subscribe(channelName)
            channels[channelName] = channel;
            
            if (isLoggingEnabled) {
                print("Pusher subscribe")
            }
        }
        result(nil);
    }
    
    public func unsubscribe(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if let pusherObj = pusher {
            let channelName = call.arguments as! String
            pusherObj.unsubscribe(channelName)
            channels.removeValue(forKey: "channelName")
            
            if (isLoggingEnabled) {
                print("Pusher unsubscribe")
            }
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
                unbindIfBound(channelName: bindArgs.channelName, eventName: bindArgs.eventName)
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
                                
                                if (self.isLoggingEnabled) {
                                    print("Pusher event: CH:\(bindArgs.channelName) EN:\(bindArgs.eventName) ED:\(jsonString ?? "no data")")
                                }
                            }
                        }
                    } catch {
                        if (self.isLoggingEnabled) {
                            print("Pusher bind error:" + error.localizedDescription)
                        }
                    }
                })
                if (isLoggingEnabled) {
                    print("Pusher bind")
                }
            }
        } catch {
            if (isLoggingEnabled) {
                print("Pusher bind error:" + error.localizedDescription)
            }
        }
        result(nil);
    }
    
    public func unbind(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        do {
            let json = call.arguments as! String
            let jsonDecoder = JSONDecoder()
            let bindArgs = try jsonDecoder.decode(BindArgs.self, from: json.data(using: .utf8)!)
            unbindIfBound(channelName: bindArgs.channelName, eventName: bindArgs.eventName)
        } catch {
            if (isLoggingEnabled) {
                print("Pusher unbind error:" + error.localizedDescription)
            }
        }
        result(nil);
    }
    
    private func unbindIfBound(channelName: String, eventName: String) {
        let channel = channels[channelName]
        if let channelObj = channel {
            let callbackId = bindedEvents[channelName + eventName]
            if let callbackIdObj = callbackId {
                channelObj.unbind(eventName: eventName, callbackId: callbackIdObj)
                bindedEvents.removeValue(forKey: channelName + eventName)
                
                if (isLoggingEnabled) {
                    print("Pusher unbind")
                }
            }
        }
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
            if (isLoggingEnabled) {
                print("Pusher changedConnectionState error:" + error.localizedDescription)
            }
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

struct InitArgs: Codable {
    var appKey: String
    var options: Options
    var isLoggingEnabled: Bool
}

struct Options: Codable {
    var cluster: String
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
