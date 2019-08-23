# Pusher Flutter Client  
An unofficial Flutter plugin that wraps [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java) on Android and [pusher-websocket-swift](https://github.com/pusher/pusher-websocket-swift) on iOS.

## Pusher.init( ... )
|Parameter      |Type           |Description		 |
|---------------|---------------|--------------------|
|*appKey*       |`String`       |*Required* - The application key is a string which is globally unique to your application. It can be found in the API Access section of your application within the Channels user dashboard.|
|*options*      |`PusherOptions`|*Required* - The options provided to pusher, more information in the *PusherOptions* section.|
|*enableLogging*|`bool`         |*Optional* - Enabling this will activate the logging of important events to the console.|
|||

## Pusher.connect( ... )
|Parameter                |Type                             |Description		|
|-------------------------|---------------------------------|-------------------|
|*onConnectionStateChange*|`Function(ConnectionStateChange)`|*Optional* - Callback when the state of the connection changes (eg. `CONNECTING`, `CONNECTED`, `DISCONNECTED`, ... ).|
|*onError*                |`Function(ConnectionError)`      |*Optional* - Callback when the connection fires an error (eg. `UnauthorizedException`).|
|||

## Pusher.subscribe( ... )
|Parameter    |Type    |Description		   |
|-------------|--------|-------------------|
|*channelName*|`String`|*Required* - provide the channel name to subscribe to (eg. `mychannel`, `private-mychannel` or `presence-mychannel`).|
|||

## PusherOptions
|Parameter        |Type         |Description		|
|-----------------|-------------|-------------------|
|*auth*			  |`PusherAuth` |*Optional* - A mechanism for authenticating a user's access to a channel at the point of subscription.|
|*cluster*        |`String`     |*Optional* - The identifier of the cluster your application was created in. When not supplied, will connect to the `mt1`(`us-east-1`) cluster.|
|*host*           |`String`     |*Optional* - Provide your own (websocket) host instead of the default `ws.pusherapp.com`|
|*port*           |`int`        |*Optional* - Provide your own (websocket) port instead of the default `443` (when encryption is enabled) or port `80` (when encryption is disabled).|
|*encrypted*      |`bool`       |*Optional* - Tell pusher to only connect over TLS connections to ensure connection traffic is encrypted. This means using `wss://` instead of `ws://`, encryption is enabled by default.|
|*activityTimeout*|`int`        |*Optional* - After this time (in milliseconds) without any messages received from the server, a ping message will be sent to check if the connection is still working. Default value is supplied by the server, low values will result in unnecessary traffic. The default is set to `30000`.|
|||

## PusherAuth
|Parameter  |Type                |Description		 |
|-----------|--------------------|-------------------|
|*endpoint*	|`String`            |*Required* - The endpoint pusher should query to make the post request (eg. https://api.example.com/broadcating/auth).|
|*headers*	|`Map<String,String>`|*Optional* - The headers that should be sent with the POST request to the above endpoint. 2 Different *Content-Types* are supported: `application/x-www-form-urlencoded` & `application/json`. Supplying any of the above types will result into the request body being sent in `form-urlencoded` format or `JSON` format. Defaults to `{'Content-Type': 'application/x-www-form-urlencoded'}`|
|||

  
## Development  
Generate the models and the factories: `flutter packages pub run build_runner build --delete-conflicting-outputs`