# MVD - Android

## Libraries
The MVD Android client uses the following libraries:

* [Android-Websockets by Eric Butler](https://github.com/codebutler/android-websockets)
* [OkHttp by Square](http://square.github.io/okhttp/)
* [Firebase by Firebase](https://www.firebase.com/docs/android/quickstart.html)
* [Sugar ORM by Satya Narayan](http://satyan.github.io/sugar/)
* [MQTT Client by fusesource](https://github.com/fusesource/mqtt-client)
* [Beanlib by Littlerobots](https://bitbucket.org/littlerobots/beanlib)

## Requirements

To work with this application you need the Android Developer Tools installed. You can find them [here](https://developer.android.com/sdk/index.html). Scroll down to "Other Download Options" and select the **SDK Tools** for your system.

**Note: You may need to perform other installation options to get the development tools setup properly on your computer**

**Note: You do not need to install the Android Studio Development environment unless you intend on altering the MVD Android component setup.**

## Getting the MVD to show up in ADB

### Windows

1. Copy the file "android_winusb.inf" in C:\PATH\TO\ANDRIOD\android-sdk\extras\google\usb_driver, overwriting the original one. 
2. Follow the instructions described [here](http://www.makeuseof.com/tag/how-can-i-install-hardware-with-unsigned-drivers-in-windows-8/) to install unsigned driver in Windows 8.
3. Navigate to C:\PATH\TO\ANDRIOD\android-sdk\extras\google\usb_driver.
4. Right-click on "android_winusb.inf" and click "Install".
5. Copy the file "adb_usb.ini" into C:\PATH\TO\USER\\.android, overwriting the original one as well.
6. Launch cmd and navigate to C:\PATH\TO\ANDROID\android-sdk\platform-tools\ .
7. Run `adb devices`


### Linux

1. Run ​`echo 0x0a5c >> ~/.android/adb_usb.ini`
2. Run `adb kill-server`
3. Run `adb start-server`
4. Run `adb devices`


### Mac


1. Run ​`echo 0x0a5c >> ~/.android/adb_usb.ini`
2. Run `adb kill-server`
3. Run `adb start-server`
4. Run `adb devices`


## Installing

If you've only got one ADB device on your system:

```
adb -d install mvd-v.0.1.0.apk
```

If you've got more than one ADB device on your system:

```
adb -s MC000090 install mvd-v.0.1.0.apk
```

## General description

The MVD can be described, in a simple way, as a network proxy. It can connect to multiple services and pass information between them. The following diagram simplifies the position of the MVD in the network.

<pre>(Arduino Bean) -- <b>[MVD]</b> -- (Cloud Service)</pre>

## Services

The connection/s that goes through the MVD are controlled by a set of services, each with its own specific usecase.

The following cloud services are included:

| Service | MVD service name | Type | Description |
|---|---|---|---|---|
| Firebase | FirebaseService | Firebase | Firebase is an increasingly popular SaaS for building real-time applications on mobile and web platforms. Read more [here](https://www.firebase.com/) |
| HTTP | HttpService | HTTP | This service allows for simple connections to REST-like web API:s. There's an example [Laravel](http://laravel.com/) implementation [here](https://github.com/agoransson/mvd-forge-demo) |
| MQTT | MqttService | MQTT | MQTT is a lightweight messaging protocol originally developed for use by remote sensors. It's most notable implementation to day is the Facebook messenger. Read more [here](http://en.wikipedia.org/wiki/MQTT) |
| Xively | XivelyService | MQTT | Xively is an Internet of Things Platform-as-a-Service (PaaS). Read more [here](https://xively.com/) |
| Elis | ElisService | Websocket | This project focuses on exploring the potential of mobile services on mobile devices (e.g. smart phones and tablets) to promote energy efficiency in existing buildings. Read more [here](http://elis.mah.se/) |

There is also a specific Bean service, used to connect to the Arduino compatible "Beans".

| Service | MVD service name | Type | Description |
|---|---|---|---|---|
| Bean | BeanService | Bluetooth | ... |

*Note that from the perspective of the MVD the Bean service is no different from any other service, it acts in an identical way.*

## Configuring the MVD

To set the different configuration options for your MVD you would use the `SET_CONFIGURATION` action and pass in the values you wish to set.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.SET_CONFIGURATION
<...configuration extras...>
```

### Debugging 

The MVD will also, if instructed, print debug messages to the logcat. Use the following commands to enable and disable debug messaging. To persist the debug setting pass the optional `PERSIST` extra.

#### Enable debugging

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.ENABLE_DEBUG
--ez cc.arduino.mvd.helper.extras.PERSIST true
```

#### Disable debugging

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DISABLE_DEBUG
--ez cc.arduino.mvd.helper.extras.PERSIST true
```

### Setting the MVD identifier

The identifier is used by services to distinguish this particular MVD from other MVD's. This should always be set.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.SET_CONFIGURATION
--es cc.arduino.mvd.helper.extras.ID "mvd-team-1"
```

**TODO: Add more config options?**

## Controlling the MVD

The Services are controlled using broadcast intents with a predefined set of extra values that you can pass to the broadcast.

Currently you can perform the following actions using broadcasts:

### Starting and Stopping the MVD app

#### Start

```
adb shell am start -n cc.arduino.mvd/cc.arduino.mvd.MainActivity
```

#### Stop

```
adb shell am force-stop cc.arduino.mvd
```

### Starting a service
Generally speaking a service is started with the `START_SERVICE` action. All services are started with as `STICKY` services.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "<MVD Service name>"
```

Each service may require extra values, see each service description for details on starting that service.

### Stopping a service

To stop a service you send the `STOP_SERVICE` action.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "<MVD Service name>"
```

### Sending values to services

Values can be passed through the MVD either in the `UP` direction or the `DOWN` direction. UP are connections towards a service while DOWN are from a service.

Each service will listen for both `UP` (*someone else wrote to me*) and `DOWN` (*I wrote to someone else*) broadcasts. Depending on the `TARGET` and `SENDER` values the Services will react either by:

1. Ignoring the value when the service was not mentioned as either `TARGET` or `SENDER`.
2. Assuming the role as the `SENDER` and not reacting to the broadcast, merely noticing that itself sent the broadcast.
3. Assuming the role as the `TARGET` and reading the value and forwarding it to the service it is connected to (f.ex. MQTT, Xively, or Bean).

#### UP

<pre>[MVD] --<b>UP</b>--> (Service)</pre>

*This command is for internal use only. It can also be used to debug connections - verifying that values are being sent to the target service from the MVD.*

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "<MVD Service name>" 
--es cc.arduino.mvd.helper.extras.SENDER "<MVD Service name>" 
--es cc.arduino.mvd.helper.extras.CODE "<code>" 
--es cc.arduino.mvd.helper.extras.PIN "<pin>" 
--es cc.arduino.mvd.helper.extras.VALUE "<value>"
```

#### DOWN

<pre>[MVD] <--<b>DOWN</b>-- (Service)</pre>

*This command is for internal use only. It can also be used to debug connections - verifying that the service is reacting properly to the sent broadcast.*

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN 
--es cc.arduino.mvd.helper.extras.TARGET "<MVD Service name>" 
--es cc.arduino.mvd.helper.extras.SENDER "<MVD Service name>" 
--es cc.arduino.mvd.helper.extras.CODE "<code>" 
--es cc.arduino.mvd.helper.extras.PIN "<pin>" 
--es cc.arduino.mvd.helper.extras.VALUE "<value>"
```

### Sensor Binding

The main use case for MVD is sensor binding from Bean to cloud services. These bindings are set up using broadcasts.

#### Creating a binding

To create a binding you would pass the bean mac address, the component code and the pin it is connected to. You also need to add a descriptive name to the binding.

```
db shell am broadcast -a cc.arduino.mvd.services.actions.CREATE_BINDING
--es cc.arduino.mvd.helper.extras.BINDING_NAME "<name for this binding>"
--es cc.arduino.mvd.services.extras.MAC "<Bean MAC>"
--es cc.arduino.mvd.services.extras.NAME "<MVD Service name>"
--es cc.arduino.mvd.helper.extras.CODE "<code>"
--es cc.arduino.mvd.helper.extras.PIN "<pin>"
```

#### List all bindings

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.LIST_BINDINGS
```

#### Delete a binding

There are two ways to delete a binding, either by selecting the service to remove bindings for:

```
db shell am broadcast -a cc.arduino.mvd.services.actions.DELETE_BINDING
--es cc.arduino.mvd.services.extras.NAME "<MVD Service name>"
```

Or by declaring the combination of the mac, component code and pin number.

```
db shell am broadcast -a cc.arduino.mvd.services.actions.DELETE_BINDING
--es cc.arduino.mvd.services.extras.MAC "<Bean MAC>"
--es cc.arduino.mvd.helper.extras.CODE "<code>"
--es cc.arduino.mvd.helper.extras.PIN "<pin>"
```

#### Clear all bindings

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.CLEAR_BINDINGS
```

### Service Routing

> Last tested: 2015-04-10

MVD is also built on the premise that services can be connected, when two services are connected all information from either of the two services are passed to the other.

**Note: Because of the nature of HTTP it's impossible to route `DOWN`updates from HttpService to other services. However, you can route other services updates to the HttpService.**

#### Add Service Route

To create a service route use the `ADD_ROUTE` action and pass two service names using the `NAMES` extra.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.ADD_ROUTE
--esa cc.arduino.mvd.services.extras.NAMES "<MVD Service name1>, <MVD Service name2>"
```

#### List Service Routes

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.LIST_ROUTES
```

#### Delete Service Route

To delete a service route use the `DELETE_ROUTE` action and pass two service names using the `NAMES` extra.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.DELETE_ROUTE
--esa cc.arduino.mvd.services.extras.NAMES "<MVD Service name1>, <MVD Service name2>"
```

#### Clear all Service Routes

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.CLEAR_ROUTES
```

# Service Broadcast Details

Each service may require special values when sending a broadcast, here you'll find all the details, including examples, for interacting with each of the services.

## BeanService (Bluetooth LE)

### Starting the service

> Last tested: 2015-04-09

To start the service you would send a simple `START_SERVICE` broadcast. Because of limitations in the BT stack we cannot rely on standard GATT connection characteristics, instead we need to poll the connected Bean's at an interval - you should pass a `DELAY` extra when starting the service.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "BeanService"
--ei cc.arduino.mvd.services.extras.DELAY 1000
```

### Stopping the service

> Last tested: 2015-04-09

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE
--ex cc.arduino.mvd.services.extras.NAME "BeanService"
```

### Initiating an LE scan

> Last tested: 2015-04-09

To start an LE scan issue you send `SCAN` broadcast and include the `TIMEOUT` which decided for how many milliseconds you want to perform the scan.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.SCAN
--ei cc.arduino.mvd.services.extras.TIMEOUT 5000
```

The result will be posted to Logcat under tag "BeanService".

### Adding a new Bean to the MVD

> Last tested: 2015-04-09

To add a new Bean (Arduino) to the MVD library you would use the `ADD_BEAN` action, and pass the `MAC` as an extra. The Bean will now which services (Sensors and Actuators) it supports and the MVD will act accordingly.

*Note: Before you can add any Bean you actually need to make a [scan](#initiating_an_le_scan), otherwise the Bean service will not have any knowledge of the available Beans nearby.*

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.ADD_BEAN
--es cc.arduino.mvd.services.extras.MAC "D0:39:72:D3:4A:C6"
```

### Removing a Bean from the MVD

To remove a Bean you use the `REMOVE_BEAN` action and pass the same `MAC` address.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.REMOVE_BEAN
--es cc.arduino.mvd.services.extras.MAC "D0:39:72:D3:4A:C6"
```

### Sending a simple value (testing UP connection)

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "BeanService" 
--es cc.arduino.mvd.helper.extras.SENDER "mvd" 
--es cc.arduino.mvd.services.extras.MAC "00:11:22:AA:BB:CC"
--es cc.arduino.mvd.helper.extras.CODE "T" 
--es cc.arduino.mvd.helper.extras.PIN "2" 
--es cc.arduino.mvd.helper.extras.VALUE "audio.wav"
```

*Note: We're telling the system that the `SENDER` is `mvd` this is just for debugging. Making sure that we're not using any of the other service names will create a clean, orphaned, call.*

### Sending a simple value (testing DOWN connection)

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN 
--es cc.arduino.mvd.helper.extras.TARGET "mvd" 
--es cc.arduino.mvd.helper.extras.SENDER "BeanService"
--es cc.arduino.mvd.services.extras.MAC "00:11:22:AA:BB:CC"
--es cc.arduino.mvd.helper.extras.CODE "L" 
--es cc.arduino.mvd.helper.extras.PIN "10" 
--es cc.arduino.mvd.helper.extras.VALUE "100"
```

*Note: We're telling the system that the `TARGET` is `mvd` this is just for debugging. Making sure that we're not using any of the other service names will create a clean, orphaned, call.*

## Firebase

### Starting the service

> Last tested: 2015-04-09

To start the service you would send a simple `START_SERVICE` broadcast which includes a value `META_URL` pointing to the Firebase URL that is of interest. The participants can freely sign up for their own Firebase instances at [https://www.firebase.com/](https://www.firebase.com/).

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "FirebaseService" 
--es cc.arduino.mvd.services.extras.URL "https://mvdtest.firebaseio.com/"
```

### Stopping the service

> Last tested: 2015-04-09

Stopping the service is done through a simple `STOP_SERVICE` intent.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "FirebaseService"
```

### Sending a simple value (testing UP connection)

> Last tested: 2015-04-09

MVD allows to send values to Firebase after the service is set up through broadcasts. This can be useful for debugging. The action is `UP`.

The intent requires 5 values to be set. The first is `TARGET` and it's the service that should handle the intent (**FirebaseService** in this case), the second is `SENDER` which can be any name (**mvd** will be good for debugging). The last three values are connected to the sensor/actuator that we're mocking: `CODE`, `PIN`, and `VALUE`.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "FirebaseService" 
--es cc.arduino.mvd.helper.extras.SENDER "mvd" 
--es cc.arduino.mvd.helper.extras.CODE "L" 
--es cc.arduino.mvd.helper.extras.PIN "7" 
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

*Note: We're telling the system that the `SENDER` is `mvd` this is just for debugging. Making sure that we're not using any of the other service names will create a clean call.*

### Sending a simple value (testing DOWN connection)

> Last tested: 2015-04-09

The intent requires 5 values to be set. The first is `TARGET` and it's the service that should handle the intent (**mvd** will be good for debugging
), the second is `SENDER` which should be the service where the value originated from (**FirebaseService** in this case). The last three values are connected to the sensor/actuator that we're mocking: `CODE`, `PIN`, and `VALUE`.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN 
--es cc.arduino.mvd.helper.extras.TARGET "mvd" 
--es cc.arduino.mvd.helper.extras.SENDER "FirebaseService" 
--es cc.arduino.mvd.helper.extras.CODE "L" 
--es cc.arduino.mvd.helper.extras.PIN "7" 
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

*Note: We're telling the system that the `TARGET` is `mvd` this is just for debugging. Making sure that we're not using any of the other service names will create a clean call.*

### Connecting Firebase to Bean through MVD

**TODO**

## HTTP

### Starting the service

> Last tested: 2015-04-08

To start the service you would send a simple `START_SERVICE` broadcast including the `URL` and `DELAY` extras. The `DELAY` defines the polling interval that the MVD will use for the lifetime of the service. Default delay is 5000 milliseconds.

There is an example implementation for a HTTP REST-like API at [mvd-forge-demo](https://github.com/agoransson/mvd-forge-demo).

*In `DOWN` direction the HttpService will only react to registered bindings, service forwarding FROM the HttpService is currently not available.*

**Note: The service can be a bit sensitive to url formatting, make sure to specify the whole `"http://.../"`, otherwise the MVD app may become unresponsive**

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "HttpService" 
--es cc.arduino.mvd.services.extras.URL "http://188.226.207.177/"
--ei cc.arduino.mvd.services.extras.DELAY 2000
```

### Stopping the service

> Last tested: 2015-04-08

Stopping the service is done through a simple `STOP_SERVICE` intent.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE
--es cc.arduino.mvd.services.extras.NAME "HttpService"
```

### Sending a simple value (testing UP connection)

> Last tested: 2015-04-08

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "HttpService" 
--es cc.arduino.mvd.helper.extras.SENDER "mvd"
--es cc.arduino.mvd.helper.extras.CODE "L"
--es cc.arduino.mvd.helper.extras.PIN "7"
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

### Sending a simple value (testing DOWN connection)

> Last tested: 2015-04-08

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN 
--es cc.arduino.mvd.helper.extras.TARGET "mvd" 
--es cc.arduino.mvd.helper.extras.SENDER "MqttService"
--es cc.arduino.mvd.helper.extras.CODE "L"
--es cc.arduino.mvd.helper.extras.PIN "7"
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

### Connecting Http to Bean through MVD

**TODO**

## MQTT

### Starting the service

> Last tested: 2015-04-08

To start the service you would send a simple `START_SERVICE` broadcast which includes a value `URL` pointing to the MQTT Broker address that is of interest. There is an open broker at `iot.eclise.org`, on port `1883`, that is available for use.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "MqttService" 
--es cc.arduino.mvd.services.extras.URL "iot.eclipse.org" 
--ei cc.arduino.mvd.services.extras.PORT 1883
```

### Stopping the service

> Last tested: 2015-04-08

Stopping the service is done through a simple `STOP_SERVICE` intent.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE
--es cc.arduino.mvd.services.extras.NAME "MqttService"
```

### Sending a simple value (testing UP connection)

> Last tested: 2015-04-08

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "MqttService" 
--es cc.arduino.mvd.helper.extras.SENDER "mvd"
--es cc.arduino.mvd.helper.extras.CODE "L"
--es cc.arduino.mvd.helper.extras.PIN "7"
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

### Sending a simple value (testing DOWN connection)

> Last tested: 2015-04-08

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN 
--es cc.arduino.mvd.helper.extras.TARGET "mvd" 
--es cc.arduino.mvd.helper.extras.SENDER "MqttService"
--es cc.arduino.mvd.helper.extras.CODE "L"
--es cc.arduino.mvd.helper.extras.PIN "7"
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

### Connecting MQTT to Bean through MVD

**TODO**

## Xively

### Starting the service

> Last tested: 2015-04-08

To start the service you would send a simple `START_SERVICE` broadcast which includes a value `API_KEY` which is your Xively API Key (found on your product page) and the `FEED_ID` for your specific feed.

Only one Xively feed can be connected at a time to the MVD.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "XivelyService" 
--es cc.arduino.mvd.services.extras.API_KEY "E90BxBbVsWxBZyiCepmoT65vswM1UDktpVpkoBBl3k2Vn8BY" 
--es cc.arduino.mvd.services.extras.FEED_ID "1822884493"
```

### Stopping the service

> Last tested: 2015-04-08

Stopping the service is done through a simple `STOP_SERVICE` intent.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE
--es cc.arduino.mvd.services.extras.NAME "XivelyService"
```

### Sending a simple value (testing UP connection)

> Last tested: 2015-04-08

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "XivelyService" 
--es cc.arduino.mvd.helper.extras.SENDER "mvd"
--es cc.arduino.mvd.helper.extras.CODE "L"
--es cc.arduino.mvd.helper.extras.PIN "7"
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

### Sending a simple value (testing DOWN connection)

> Last tested: 2015-04-08

The `DOWN` connection will send a value from the XivelyService to another service. If no valid `TARGET` service is given this will be an orphaned broadcast - no one will recieve the value.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN
--es cc.arduino.mvd.helper.extras.TARGET "mvd"
--es cc.arduino.mvd.helper.extras.SENDER "XivelyService"
--es cc.arduino.mvd.helper.extras.CODE "T"
--es cc.arduino.mvd.helper.extras.PIN "12"
--es cc.arduino.mvd.helper.extras.VALUE "55"
```

### Connecting Xively to Bean through MVD

**TODO**

## Elis

### Starting the service

> Last tested: 2015-04-09

To start the service you would send a simple `START_SERVICE` broadcast which includes a value `URL` which is your Elis URL and the `PORT`. The `URL` should be formatted as `ws://<hostname>`. Default port is 11414.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE 
--es cc.arduino.mvd.services.extras.NAME "ElisService" 
--es cc.arduino.mvd.services.extras.URL "ws://echo.websocket.org" 
--ei cc.arduino.mvd.services.extras.PORT 80
```

### Stopping the service

> Last tested: 2015-04-09

Stopping the service is done through a simple `STOP_SERVICE` intent.

```
adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE
--es cc.arduino.mvd.services.extras.NAME "ElisService"
```

### Sending a simple value (testing UP connection)

> Last tested: 2015-04-09

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP 
--es cc.arduino.mvd.helper.extras.TARGET "ElisService" 
--es cc.arduino.mvd.helper.extras.SENDER "mvd"
--es cc.arduino.mvd.helper.extras.CODE "L"
--es cc.arduino.mvd.helper.extras.PIN "7"
--es cc.arduino.mvd.helper.extras.VALUE "124"
```

adb shell am broadcast -a cc.arduino.mvd.helper.actions.UP --es cc.arduino.mvd.helper.extras.TARGET "ElisService" --es cc.arduino.mvd.helper.extras.SENDER "mvd" --es cc.arduino.mvd.helper.extras.CODE "L" --es cc.arduino.mvd.helper.extras.PIN "7" --es cc.arduino.mvd.helper.extras.VALUE "124"

### Sending a simple value (testing DOWN connection)

> Last tested: 2015-04-08

The `DOWN` connection will send a value from the XivelyService to another service. If no valid `TARGET` service is given this will be an orphaned broadcast - no one will recieve the value.

```
adb shell am broadcast -a cc.arduino.mvd.helper.actions.DOWN
--es cc.arduino.mvd.helper.extras.TARGET "mvd"
--es cc.arduino.mvd.helper.extras.SENDER "XivelyService"
--es cc.arduino.mvd.helper.extras.CODE "T"
--es cc.arduino.mvd.helper.extras.PIN "12"
--es cc.arduino.mvd.helper.extras.VALUE "55"
```

### Connecting Elis to Bean through MVD

**TODO**
