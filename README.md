# Pusher Flutter Client
An unofficial Flutter plugin that wraps [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java) on Android and [pusher-websocket-swift](https://github.com/pusher/pusher-websocket-swift) on iOS.

Get it from [pub](https://pub.dev/packages/pusher_websocket_flutter).
# How to install
* Add to your pubspec.yaml
```
dependencies:
  pusher_websocket_flutter: ^0.2.0
```
* In `/ios/Podfile`, set global platform to at least 9.0
`platform :ios, '9.0'`
### For iOS Objective-C based Flutter apps
It is currently a bit difficult to get some Swift based Flutter plugins working in an Objective-C based Flutter app. See [here for info](https://github.com/flutter/flutter/issues/25676) and [here for a way to fix](https://github.com/fayeed/flutter_freshchat/issues/9#issuecomment-514329934).

This set of steps should work to fix this for your project.
* Add `use_frameworks!` to the end of the Runner section in `/ios/Podfile`
* Set Swift version in your iOS Runner project.
    * Open the project with Xcode.
    * In Runner, File -> New -> File -> Swift File. Name it anything.
    * Xcode will ask you if you wish to create Bridging Header, click yes.
    * Go to Runner `Build Settings` and set `SWIFT_VERSION` to either 4.2 or 5.0
    * Delete the Swift file created in step 2
    * Delete the Bridging Header created in step 3
* `flutter clean`
* In /ios `pod install --repo-update`

If you have trouble, try checking out the example_objc Flutter project.
# How to use
## Pusher.init( ... )
|Parameter      |Type           |Description		 |
|---------------|---------------|--------------------|
|*appKey*       |`String`       |*Required* - The application key is a string which is globally unique to your application. It can be found in the API Access section of your application within the Channels user dashboard.|
|*options*      |`PusherOptions`|*Required* - The options provided to pusher, more information in the *PusherOptions* section.|
|*enableLogging*|`bool`         |*Optional* - Enabling this will activate the logging of important events to the console.|


## Pusher.connect( ... )
|Parameter                |Type                             |Description		|
|-------------------------|---------------------------------|-------------------|
|*onConnectionStateChange*|`Function(ConnectionStateChange)`|*Optional* - Callback when the state of the connection changes (eg. `CONNECTING`, `CONNECTED`, `DISCONNECTED`, ... ).|
|*onError*                |`Function(ConnectionError)`      |*Optional* - Callback when the connection fires an error (eg. `UnauthorizedException`).|

## Pusher.subscribe( ... )
|Parameter    |Type    |Description		   |
|-------------|--------|-------------------|
|*channelName*|`String`|*Required* - provide the channel name to subscribe to (eg. `mychannel`, `private-mychannel` or `presence-mychannel`).|

## Pusher.getSocketId()
Returns the current socket ID, updated after a connection change

## PusherOptions
|Parameter        |Type         |Description		|
|-----------------|-------------|-------------------|
|*auth*			  |`PusherAuth` |*Optional* - A mechanism for authenticating a user's access to a channel at the point of subscription.|
|*cluster*        |`String`     |*Optional* - The identifier of the cluster your application was created in. When not supplied, will connect to the `mt1`(`us-east-1`) cluster.|
|*host*           |`String`     |*Optional* - Provide your own (websocket) host instead of the default `ws.pusherapp.com`|
|*port*           |`int`        |*Optional* - Provide your own (websocket) port instead of the default `443` (when encryption is enabled) or port `80` (when encryption is disabled).|
|*encrypted*      |`bool`       |*Optional* - Tell pusher to only connect over TLS connections to ensure connection traffic is encrypted. This means using `wss://` instead of `ws://`, encryption is enabled by default.|
|*activityTimeout*|`int`        |*Optional* - After this time (in milliseconds) without any messages received from the server, a ping message will be sent to check if the connection is still working. Default value is supplied by the server, low values will result in unnecessary traffic. The default is set to `30000`.|

## PusherAuth
|Parameter  |Type                |Description		 |
|-----------|--------------------|-------------------|
|*endpoint*	|`String`            |*Required* - The endpoint pusher should query to make the post request (eg. https://api.example.com/broadcating/auth).|
|*headers*	|`Map<String,String>`|*Optional* - The headers that should be sent with the POST request to the above endpoint. 2 Different *Content-Types* are supported: `application/x-www-form-urlencoded` & `application/json`. Supplying any of the above types will result into the request body being sent in `form-urlencoded` format or `JSON` format. Defaults to `{'Content-Type': 'application/x-www-form-urlencoded'}`|

  
## Development
This project uses code generation. Run after changing models in the project.
 
`flutter packages pub run build_runner build --delete-conflicting-outputs`
