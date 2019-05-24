import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pusher/pusher.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Event lastEvent;
  String lastConnectionState;

  @override
  void initState() {
    super.initState();
    initPusher();
  }

  Future<void> initPusher() async {
    try {
      await Pusher.init("APP_KEY", PusherOptions(cluster: "us2"));
      await Pusher.connect(onConnectionStateChange: (x) {
        print("Connection state from ${x.previousState} to ${x.currentState}");
        if (mounted)
          setState(() {
            lastConnectionState = x.currentState;
          });
      }, onError: (x) {
        print("Error: ${x.message}");
      });
      var channel = await Pusher.subscribe("my-channel");
      await channel.bind("my-event", (x) {
        if (mounted)
          setState(() {
            lastEvent = x;
          });
      });
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          appBar: AppBar(
            title: Text('Plugin example app'),
          ),
          body: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      "Connection State: ",
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(lastConnectionState ?? "Unknown"),
                  ],
                ),
                SizedBox(height: 8),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      "Last Event Channel: ",
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(lastEvent?.channel ?? ""),
                  ],
                ),
                SizedBox(height: 8),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      "Last Event Name: ",
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(lastEvent?.event ?? ""),
                  ],
                ),
                SizedBox(height: 8),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      "Last Event Data: ",
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(lastEvent?.data ?? ""),
                  ],
                ),
              ],
            ),
          )),
    );
  }
}
