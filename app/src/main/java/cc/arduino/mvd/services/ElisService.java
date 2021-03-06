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

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;
import cc.arduino.mvd.libs.WebSocketClient;
import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;

import static cc.arduino.mvd.MvdHelper.DEBUG;

/**
 * This is the Elis integration of MVD. This is using WebSockets to send and receive messages to and
 * from the Elis "service".
 *
 * @author Andreas Goransson, 2015-04-08
 */
public class ElisService extends Service {

  public static final String TAG = ElisService.class.getSimpleName();

  private String url = null;

  private int port = 11414;

  private WebSocketClient webSocketClient;

  private boolean started = false;

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

      url = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_URL);

      port = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_PORT, 11414);

      List<BasicNameValuePair> extraHeaders = new ArrayList<>();

      URI uri = URI.create(url + ":" + port);

      webSocketClient = new WebSocketClient(uri, webSocketListener, extraHeaders);

      webSocketClient.connect();

      started = true;

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

    webSocketClient.disconnect();

    if (DEBUG) {
      Log.d(TAG, TAG + " stopped.");
    }
  }


  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private WebSocketClient.Listener webSocketListener = new WebSocketClient.Listener() {
    @Override
    public void onConnect() {
      if (DEBUG) {
        Log.d(TAG, "Connected to " + url + ":" + port);
      }

      try {
        registerBindings();
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onMessage(String message) {
      parseElisMessage(message);
    }

    @Override
    public void onMessage(byte[] data) {
      String message = new String(data);
      parseElisMessage(message);
    }

    @Override
    public void onDisconnect(int code, String reason) {
      if (DEBUG) {
        Log.d(TAG, "Disconnected from " + url + ":" + port);
      }
    }

    @Override
    public void onError(Exception error) {
      if (DEBUG) {
        Log.e(TAG, error.getMessage());
      }
    }
  };

  /**
   * Handle incoming messages, these should be in one of two formats.
   * <p/>
   * GET: code:pin
   * or
   * SET: code:pin:value
   *
   * @param message
   */
  private void parseElisMessage(String message) {
    String[] parts = message.split(":");
    if (parts != null) {
      if (parts.length == 2) {
        // GET... do nothing, I'll just keep publishing values just like other services
      } else if (parts.length == 3) {
        // SET... pass the value down to connected services
        String code = parts[0];
        String pin = parts[1];
        String val = parts[2];

        handleKeyValFromElis(code, pin, val);
      }
    }
  }

  /**
   * This just sends all bindings for the Elis service as a register message to Elis
   */
  private void registerBindings() throws JSONException {
    JSONArray register = new JSONArray();
    List<Binding> bindings = Binding.find(Binding.class, "service = ?", TAG);
    for (Binding binding : bindings) {
      JSONObject jsonBinding = new JSONObject();
      jsonBinding.put("name", binding.getName());
      jsonBinding.put("code", binding.getCode());
      jsonBinding.put("pin", binding.getPin());
      register.put(jsonBinding);
    }

    JSONObject message = new JSONObject();
    message.put("id", MvdHelper.getMvdId(getApplicationContext()));
    message.put("register", register);

    webSocketClient.send(message.toString());
  }

  /**
   * This will handle the key-val from Firebase. Basically it determines if I should send a DOWN
   * broadcast or not depending on the last value I wrote UP.
   *
   * @param code
   * @param pin
   * @param value
   */
  private void handleKeyValFromElis(String code, String pin, String value) {
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
        Log.d(TAG, "I got the following from Elis:");
        Log.d(TAG, codePinValue.toString());
      }

      // This was not me editing, I'll go ahead and broadcast the value "down" to other services
      // (The value is from "me", but it was edited by someone else in Elis so I'll mask it as "me"
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

      try {
        sendElisResponseValueSet(code, pin, value);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    // I wrote these changes, I shouldn't care about informing anyone else
    else {
      // Do nothing
    }
  }

  /**
   * Send a Elis response to a SET request.
   *
   * @param code
   * @param pin
   * @param value
   */
  private void sendElisResponseValueSet(String code, String pin, String value) throws JSONException {
    JSONObject response = new JSONObject();
    response.put("code", code);
    response.put("pin", pin);
    response.put("value", value);

    JSONObject message = new JSONObject();
    message.put("id", MvdHelper.getMvdId(getApplicationContext()));
    message.put("response", response);

    webSocketClient.send(message.toString());
  }

  /**
   * Send a Elis response to a GET request.
   *
   * @param code
   * @param pin
   * @param value
   */
  private void sendElisResponseValueGet(String code, String pin, String value) throws JSONException {
    JSONObject response = new JSONObject();
    response.put("code", code);
    response.put("pin", pin);
    response.put("value", value);

    JSONObject message = new JSONObject();
    message.put("id", MvdHelper.getMvdId(getApplicationContext()));
    message.put("response", response);

    webSocketClient.send(message.toString());
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
              try {
                sendElisResponseValueGet(codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());
              } catch (JSONException e) {
                e.printStackTrace();
              }

              lastCodePinValue = codePinValue;
            }
          }

          // Or if we've getting values from other "cloud" services (DOWN direction) we should write to Firebase too
          else if (action.equals(MvdHelper.ACTION_DOWN)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              try {
                sendElisResponseValueGet(codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());
              } catch (JSONException e) {
                e.printStackTrace();
              }

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
        webSocketClient.disconnect();

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }


    }
  };

}