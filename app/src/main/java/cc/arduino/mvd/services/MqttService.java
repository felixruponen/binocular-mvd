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

import java.net.URISyntaxException;
import java.util.List;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;
import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;

import static cc.arduino.mvd.MvdHelper.DEBUG;

/**
 * This is the MQTT integration of MVD. It uses unsecured MQTT to read and write values to a selected
 * MQTT broker. It will publish a value as a string (in byte[]) to the topic pattern:
 * <p/>
 * Topic: component/code/pin
 * Example: "component/L/13", or "component/T/12"
 * <p/>
 * It will automatically subscribe to all component topics: "component/#"
 * <p/>
 * Recommended test broker is "iot.eclipse.org" on port 1883
 *
 * @author Andreas Goransson, 2015-03-26
 */
public class MqttService extends Service {

  public static final String TAG = MqttService.class.getSimpleName();

  private String host;

  private int port;

  private MQTT mqtt;

  private boolean started = false;

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
    if (!started) {
      host = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_URL);
      port = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_PORT, 1883);

      mqtt = new MQTT();

      try {
        mqtt.setHost(host, port);

        mqtt.setClientId(MvdHelper.getMvdId(getApplicationContext()));

        connection = mqtt.callbackConnection();

        connection.listener(mqttListener);

        connection.connect(mqttCallback);

        started = true;

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
  private void handleKeyValFromMqtt(String code, String pin, String value) {
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

  private void publish(String code, String pin, String value) {
    final String topic = "component/" + code + "/" + pin;

//    try {
//      connection.publish(topic, value.getBytes(), QoS.AT_MOST_ONCE, false);
//
//      lastChangedCode = code;
//      lastChangedPin = pin;
//      lastChangedValue = value;
//    } catch (Exception e) {
//      e.printStackTrace();
//    }

    // Send a message to a topic
    connection.publish(topic, value.getBytes(), QoS.AT_MOST_ONCE, false, new Callback<Void>() {
      public void onSuccess(Void v) {
        // the pubish operation completed successfully.
        if (DEBUG) {
          Log.d(TAG, "Successfully published message to: " + topic);
        }
      }

      public void onFailure(Throwable value) {
//          connection.close(null); // publish failed.
        if (DEBUG) {
          Log.e(TAG, "Failed published message");
        }
      }
    });
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
      String s = new String(utf8Buffer.getData());
      Log.d(TAG, "found: " + s);
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

      // Subscribe to a topic
      Topic topic = new Topic("component/#", QoS.AT_MOST_ONCE);
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
          Log.d(TAG, "TARGET: " + target);
          Log.d(TAG, "SENDER: " + sender);
          Log.d(TAG, codePinValue.toString());
        }

        if (!sender.equals(TAG)) {
          // If we're getting values from the Bean service (UP direction)
          if (action.equals(MvdHelper.ACTION_UP)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              publish(codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());

              lastCodePinValue = codePinValue;
            }
          }

          // Or if we've getting values from other "cloud" services (DOWN direction) we should write to Firebase too
          else if (action.equals(MvdHelper.ACTION_DOWN)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              publish(codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());

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
        try {
          connection.disconnect(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
              if (DEBUG) {
                Log.d(TAG, "Disconnected from MQTT broker.");
              }
            }

            @Override
            public void onFailure(Throwable throwable) {
              if (DEBUG) {
                Log.d(TAG, "Failed to disconnect from MQTT broker.");
              }
            }
          });

          connection.kill(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
              if (DEBUG) {
                Log.d(TAG, "Killed MQTT service.");
              }
            }

            @Override
            public void onFailure(Throwable throwable) {
              if (DEBUG) {
                Log.d(TAG, "Failed to kill MQTT service.");
              }
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        }

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }

    }
  };

}