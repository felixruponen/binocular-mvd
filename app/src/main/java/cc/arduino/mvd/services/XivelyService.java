/*
 * Copyright 2015 Arduino Verkstad AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.arduino.mvd.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;
import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;

import static cc.arduino.mvd.MvdHelper.DEBUG;

/**
 * This is the XIVELY integration of MVD. It uses MQTT to read and write values from a Xively
 * feed. To make this service work the user need to pass the api key and the feed id. When the
 * service is running new values will be posted to channels within the feed, identified by the code
 * and the pin together in a string (code+pin, for example L+13).
 * <p/>
 * Only one Xively feed can be connected at a time.
 *
 * @author Andreas Goransson, 2015-03-19
 */
public class XivelyService extends Service {

  public static final String TAG = XivelyService.class.getSimpleName();

  private String host = "api.xively.com";

  private int port = 1883;

  private MQTT mqtt;

  private String apiKey;

  private String feedId;

  //  private BlockingConnection connection;
  private CallbackConnection connection;

  private CodePinValue lastCodePinValue = null;

  @Override
  public void onCreate() {
    super.onCreate();

    IntentFilter filter = new IntentFilter();
    filter.addAction(MvdHelper.ACTION_UP);
    filter.addAction(MvdHelper.ACTION_DOWN);
    filter.addAction(MvdHelper.ACTION_KILL);

    registerReceiver(broadcastReceiver, filter);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {

      apiKey = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_API_KEY);

      feedId = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_FEED_ID);

      mqtt = new MQTT();

      try {
        mqtt.setHost(host, port);

        mqtt.setUserName(apiKey);

        connection = mqtt.callbackConnection();

        connection.listener(mqttListener);

        connection.connect(mqttCallback);

      } catch (URISyntaxException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (DEBUG) {
        Log.d(TAG, TAG + " started.");
      }
    }

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    unregisterReceiver(broadcastReceiver);

    // To disconnect..
    connection.disconnect(new Callback<Void>() {
      public void onSuccess(Void v) {
        // called once the connection is disconnected.
      }

      public void onFailure(Throwable value) {
        // Disconnects never fail.
      }
    });

    if (DEBUG) {
      Log.d(TAG, TAG + " stopped.");
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * This will handle the key-val from Firebase. Basically it determines if I should send a DOWN
   * broadcast or not depending on the last value I wrote UP.
   *
   * @param code
   * @param pin
   * @param value
   */
  private void handleKeyValFromXively(String code, String pin, String value) {
    CodePinValue codePinValue = new CodePinValue(code, pin, value);

    // First read, just store the last values.
    if (lastCodePinValue == null) {
      if (DEBUG) {
        Log.d(TAG, "First read.");
        Log.d(TAG, codePinValue.toString());
      }

      lastCodePinValue = codePinValue;
    }

    // Someone else changed the values in the cloud, I should react to it!
    else if (!lastCodePinValue.equals(codePinValue)) {
      if (DEBUG) {
        Log.d(TAG, "I got the following from Xively:");
        Log.d(TAG, codePinValue.toString());
      }

      // This was not me editing, I'll go ahead and broadcast the value "down" to other services
      // (The value is from "me", but it was edited by someone else in Xively so I'll mask it as "me"
      String source = TAG;

      // Find a forwarding where I am included
      List<ServiceRoute> routes = ServiceRoute.find(ServiceRoute.class, "service1 = ? OR service2 = ?", TAG, TAG);
      for (ServiceRoute route : routes) {
        if (DEBUG) {
          Log.d(TAG, "Found route! " + route.getService1() + "-" + route.getService2());
        }

        // Get the target
        String target = MvdHelper.getServiceTarget(route, TAG);

        // Send the broadcast
        MvdHelper.sendDownBroadcast(getApplicationContext(), source, target, codePinValue);
      }

      // Find a forwarding where I am included
      List<Binding> bindings = Binding.find(Binding.class, "service = ?", TAG);
      for (Binding binding : bindings) {
        if (DEBUG) {
          Log.d(TAG, "Found binding for " + binding.getCode() + "/" + binding.getPin() + " to " + binding.getService());
        }

        // Get the target (For bindings this is always the BeanService in the DOWN direction)
        String target = BeanService.class.getSimpleName();

        String mac = binding.getMac();

        // Send the broadcast
        MvdHelper.sendBeanDownBroadcast(getApplicationContext(), source, target, mac, codePinValue);
      }
    }

    // I wrote these changes, I shouldn't care about informing anyone else
    else {
      // Do nothing
    }
  }

  /**
   * This is how other components of the app communicate with me
   */
  public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      String action = intent.getAction();

      String target = intent.getStringExtra(MvdHelper.EXTRA_TARGET);
      String sender = intent.getStringExtra(MvdHelper.EXTRA_SENDER);

      CodePinValue codePinValue = null;

      // If we're getting the primitives...
      if (intent.hasExtra(MvdHelper.EXTRA_CODE) &&
          intent.hasExtra(MvdHelper.EXTRA_PIN) &&
          intent.hasExtra(MvdHelper.EXTRA_VALUE)) {
        String code = intent.getStringExtra(MvdHelper.EXTRA_CODE);
        String pin = intent.getStringExtra(MvdHelper.EXTRA_PIN);
        String value = intent.getStringExtra(MvdHelper.EXTRA_VALUE);

        codePinValue = new CodePinValue(code, pin, value);
      }

      // ... or if we're getting the serializable
      else if (intent.hasExtra(MvdHelper.EXTRA_CODE_PIN_VALUE)) {
        codePinValue = (CodePinValue) intent.getSerializableExtra(MvdHelper.EXTRA_CODE_PIN_VALUE);
      }

      // Make sure we've got a valid key-value set.
      if (codePinValue != null) {

        if (DEBUG) {
          Log.d(TAG, codePinValue.toString());
        }

        if (!sender.equals(TAG)) {
          // If we're getting values from the Bean service (UP direction)
          if (action.equals(MvdHelper.ACTION_UP)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              publish(codePinValue);

              lastCodePinValue = codePinValue;
            }
          }

          // Or if we've getting values from other "cloud" services (DOWN direction) we should write to Firebase too
          else if (action.equals(MvdHelper.ACTION_DOWN)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              publish(codePinValue);

              lastCodePinValue = codePinValue;
            }
          }
        } else {
          // Do nothing, this was myself broadcasting values
          if (DEBUG) {
            Log.d(TAG, "I just forwarded a value to another service (" + target + ")");
          }
        }
      }

      // If someone told me to kill myself...
      if (action.equals(MvdHelper.ACTION_KILL) && target.equals(TAG)) {
//        firebase.removeEventListener(childListener)
        // TODO: remove the listner? (connection.disconnect()?)

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }

    }
  };

  private void publish(CodePinValue codePinValue) {
    publish(codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());
  }

  private void publish(String code, String pin, String value) {
    String topic = "/v2/feeds/" + feedId + ".json";

    JSONObject payload = null;
    try {
      payload = createPayload(code, pin, value);

      // Send a message to a topic
      connection.publish(topic, payload.toString().getBytes(), QoS.AT_MOST_ONCE, false, new Callback<Void>() {
        public void onSuccess(Void v) {
          // the pubish operation completed successfully.
        }

        public void onFailure(Throwable value) {
//          connection.close(null); // publish failed.
        }
      });
    } catch (JSONException e) {
      e.printStackTrace();
    }

  }

  private Listener mqttListener = new Listener() {
    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onPublish(UTF8Buffer utf8Buffer, Buffer buffer, Runnable ack) {
      // You can now process a received message from a topic.

      // TODO
      String json = new String(utf8Buffer.getData());

      try {
        parsePayload(json);
      } catch (JSONException e) {
        e.printStackTrace();
      }

      // Once process execute the ack runnable.
      ack.run();
    }

    @Override
    public void onFailure(Throwable throwable) {

    }
  };

  private Callback<Void> mqttCallback = new Callback<Void>() {
    @Override
    public void onSuccess(Void aVoid) {
      String topicString = "/v2/feeds/" + feedId + ".json";

      // Subscribe to a topic
      Topic topic = new Topic(topicString, QoS.AT_MOST_ONCE);
      Topic[] topics = new Topic[]{topic};

      connection.subscribe(topics, new Callback<byte[]>() {
        public void onSuccess(byte[] qoses) {
          // The result of the subscribe request.
        }

        public void onFailure(Throwable value) {
//          connection.close(); // subscribe failed.
        }
      });
    }

    @Override
    public void onFailure(Throwable value) {
//      result.failure(value); // If we could not connect to the server.
    }
  };

  private JSONObject createPayload(String code, String pin, String value) throws JSONException {
    JSONObject root = new JSONObject();

    Date d = Calendar.getInstance(Locale.getDefault()).getTime();

    JSONObject datapoint = new JSONObject();
    datapoint.put("at", d.toString());
    datapoint.put("value", value);

    JSONArray datapoints = new JSONArray();
    datapoints.put(datapoint);

    JSONObject datastream = new JSONObject();
    datastream.put("id", code + "+" + pin);
    datastream.put("datapoints", datapoints);

    JSONArray datastreams = new JSONArray();
    datastreams.put(datastream);

    root.put("version", "1.0.0");
    root.put("datastreams", datastreams);

    return root;
  }

  private String[] parsePayload(String json) throws JSONException {
    int start = json.indexOf("{");
    String jsonFixed = json.substring(start);
    JSONObject root = new JSONObject(jsonFixed);

    JSONArray datastreams = root.getJSONArray("datastreams");

    for (int i = 0; i < datastreams.length(); i++) {
      JSONObject datastream = datastreams.getJSONObject(i);
      String id = datastream.getString("id");
      final String re = Pattern.quote("+");
      String[] codepin = id.split(re);
      String code = codepin[0];
      String pin = codepin[1];
      String value = datastream.getString("current_value");

      handleKeyValFromXively(code, pin, value);
    }

    return new String[]{};
  }

}