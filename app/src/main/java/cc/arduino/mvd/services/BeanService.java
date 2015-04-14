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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;
import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;
import nl.littlerobots.bean.Bean;
import nl.littlerobots.bean.BeanDiscoveryListener;
import nl.littlerobots.bean.BeanListener;
import nl.littlerobots.bean.BeanManager;

import static cc.arduino.mvd.MvdHelper.DEBUG;

/**
 * This is the Bean integration of MVD.
 *
 * @author Andreas Goransson, 2015-03-19
 */
public class BeanService extends Service {

  private static final String TAG = BeanService.class.getSimpleName();

  private Handler handler;

  private long scanTimeout = 1000;

  private int delay = 1000; // Default polling for Bean sensors

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

  private ScheduledFuture scheduledFuture;

  private Map<String, Bean> beans = new HashMap<>();

  private CodePinValue lastCodePinValue = null;

  @Override
  public void onCreate() {
    super.onCreate();

    handler = new Handler(getMainLooper());

    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    IntentFilter filter = new IntentFilter();
    filter.addAction(MvdHelper.ACTION_UP);
    filter.addAction(MvdHelper.ACTION_DOWN);
    filter.addAction(MvdHelper.ACTION_KILL);
    filter.addAction(MvdHelper.ACTION_SCAN);
    filter.addAction(MvdServiceReceiver.ACTION_ADD_BEAN);
    filter.addAction(MvdServiceReceiver.ACTION_REMOVE_BEAN);
    filter.addAction(MvdServiceReceiver.ACTION_LIST_BEANS);

    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

    registerReceiver(broadcastReceiver, filter);

    if (!bluetoothAdapter.isEnabled()) {
      if (DEBUG) {
        Log.d(TAG, "Turning on Bluetooth");
      }
      boolean result = bluetoothAdapter.enable();
      if (DEBUG) {
        Log.d(TAG, "result: " + result);
      }
    } else {
      if (DEBUG) {
        Log.d(TAG, "Bluetooth is enabled");
      }
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {
      scanTimeout = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_TIMEOUT, 3000);

      delay = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_DELAY, 1000);

      startPullRequests(delay);

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

    disconnectAllDevices();

    stopGetRequests();

    if (DEBUG) {
      Log.d(TAG, TAG + " stopped.");
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private void startPullRequests(int delay) {
    Runnable task = new Runnable() {
      @Override
      public void run() {
//        Iterator it = beans.entrySet().iterator();
//        while (it.hasNext()) {
//          Map.Entry pair = (Map.Entry) it.next();
//
//          Bean bean = (Bean) pair.getValue();
//          sendPollMessage(bean);
//        }

        List<Binding> bindings = Binding.listAll(Binding.class);

        for (Binding binding : bindings) {
          Bean bean = beans.get(binding.getMac());

          sendPollMessage(bean, binding);
        }
      }
    };

    scheduledFuture = scheduler.scheduleAtFixedRate(
        task,
        delay,
        delay,
        TimeUnit.MILLISECONDS
    );
  }

  /**
   * This sends a polling notification to the bean, which will tell the bean to send values for all it's sensors.
   *
   * @param bean
   * @param binding
   */
  private void sendPollMessage(Bean bean, Binding binding) {
    String msg = binding.getCode() + "/" + binding.getPin() + "\n";

    bean.sendSerialMessage(msg);
  }

  /**
   * Stop the recurring POLL's
   */
  private void stopGetRequests() {
    scheduledFuture.cancel(true);
  }

  /**
   * Start, or stop, a LE scan. This will print devices as they are found to the ADB log cat.
   *
   * @param scan True if starting the scan, or false if stopping
   */
  private void scanDevices(boolean scan) {
    // Start scanning
    if (scan) {
      Log.d(TAG, "Starting bean scan...");
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Stopping bean scan.");
          BeanManager.getInstance().cancelDiscovery();
        }
      }, scanTimeout);

      BeanManager.getInstance().startDiscovery(beanDiscoveryListener);
    }

    // Otherwise stop scanning
    else {
      Log.d(TAG, "Stopping BEAN scan.");
      BeanManager.getInstance().cancelDiscovery();
    }
  }

  /**
   * Attempt to connect to a Bean
   *
   * @param mac
   */
  private void connectToDevice(String mac) {
    if (DEBUG) {
      Log.d(TAG, "Attempting to connect to " + mac);
    }

    Collection<Bean> scannedBeans = BeanManager.getInstance().getBeans();

    boolean beanFound = false;
    for (Bean bean : scannedBeans) {
      if (bean.getDevice().getAddress().equals(mac)) {
        bean.connect(getApplicationContext(), new MvdBeanListener(bean.getDevice().getAddress()));

        beans.put(mac, bean);

        beanFound = true;
      }
    }

    if (!beanFound) {
      Log.e(TAG, "I couldn't find any bean with address [" + mac + "]");
    }
  }

  /**
   * Attempt to disconnect a device.
   *
   * @param mac
   */
  private void disconnectDevice(String mac) {
    if (DEBUG) {
      Log.d(TAG, "Attempting to disconnect " + mac);
    }

    if (beans.containsKey(mac)) {
      Bean bean = beans.remove(mac);

      if (bean.isConnected()) {
        bean.disconnect();
      } else {
        Log.e(TAG, "The Bean was not connected, but I made sure to remove it from my library anyway.");
      }
    } else {
      Log.e(TAG, "I don't know this bean, did you enter the correct MAC?");
    }
  }

  /**
   * Disconnects all devices.
   */
  private void disconnectAllDevices() {
    Iterator it = beans.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();
      Bean bean = (Bean) pair.getValue();
      bean.disconnect();
      it.remove();
    }
  }

  /**
   * This is used for discovering beans.
   */
  private BeanDiscoveryListener beanDiscoveryListener = new BeanDiscoveryListener() {
    @Override
    public void onBeanDiscovered(Bean bean) {
      Log.d(TAG, bean.getDevice().getName() + " - " + bean.getDevice().getAddress());
    }

    @Override
    public void onDiscoveryComplete() {
      if (DEBUG) {
        Log.d(TAG, "Bean scan complete!");
      }
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
          Log.d(TAG, codePinValue.toString());
        }

        if (!sender.equals(TAG)) {
          // BeanService ALWAYS requires other services to pass the MAC too!
          if (!intent.hasExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC)) {
            Log.e(TAG, "The BeanService requires the MAC to be set!");

            return;
          }

          // Get the mac!
          String mac = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC);


          // If we're getting values from the Bean service (UP direction)
          if (action.equals(MvdHelper.ACTION_UP)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              sendToBean(mac, codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());

              lastCodePinValue = codePinValue;
            }
          }


          // Or if we've getting values from other "cloud" services (DOWN direction) we should write to Bean too
          else if (action.equals(MvdHelper.ACTION_DOWN)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              sendToBean(mac, codePinValue.getCode(), codePinValue.getPin(), codePinValue.getValue());

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

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }

      // I was asked to start scanning for LE units
      if (action.equals(MvdHelper.ACTION_SCAN)) {
        scanDevices(true);
      }

      // BT state changed
      if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        Log.d(TAG, "Bluetooth adapter state change");
      }

      if (action.equals(MvdServiceReceiver.ACTION_ADD_BEAN)) {

        if (!intent.hasExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC)) {
          Log.e(TAG, "Need to give a MAC address to add a new Bean.");
        } else {
          String mac = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC);

          if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            Log.e(TAG, "This is not a valid MAC address.");
          } else {
            // Connect to device
            connectToDevice(mac);
          }
        }
      }

      if (action.equals(MvdServiceReceiver.ACTION_REMOVE_BEAN)) {
        if (!intent.hasExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC)) {
          Log.e(TAG, "I need a MAC, otherwise I won't know which Bean to remove.");
        } else {
          String mac = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC);

          if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            Log.e(TAG, "This is not a valid MAC address.");
          } else {
            // Make sure the MAC is known
            if (!beans.containsKey(mac)) {
              Log.e(TAG, "I don't know this MAC, please try again.");
            } else {
              disconnectDevice(mac);
            }
          }
        }
      }

      if (action.equals(MvdServiceReceiver.ACTION_LIST_BEANS)) {
        Log.d(TAG, "Listing connected beans:");
        printAllConnectedBeans();
        Log.d(TAG, "Done listing connected beans.");
      }
    }
  };


  /**
   * This will send a value to the correct bean.
   * <p/>
   *
   * @param code
   * @param pin
   * @param value
   */
  private void sendToBean(String mac, String code, String pin, String value) {
    String message = code + "/" + pin + "/" + value + "\n"; // Always send newline!

    if (DEBUG) {
      Log.d(TAG, message);
    }


    if (beans.containsKey(mac)) {

        Bean b = beans.get(mac);
        try{
            b.sendSerialMessage(message);
        } catch(Exception e) {
            e.printStackTrace();

            // TODO: remove bindings, remove beans, scan, add beans

            scanDevices(true);

            try {
                disconnectDevice(mac);
            }catch(Exception e1) {
                e1.printStackTrace();
            }

            connectToDevice(mac);
        }

    } else {
      if (DEBUG) {
        Log.d(TAG, "Bean with address [" + mac + "] was not found.");
      }
    }
  }

  /**
   * This will handle the key-val from Bean's. Basically it determines if I should send a DOWN
   * broadcast or not depending on the last value I wrote UP.
   *
   * @param codePinValue
   */
  private void handleKeyValFromBean(String mac, CodePinValue codePinValue) {



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
        Log.d(TAG, "I got the following from Bean:");
        Log.d(TAG, codePinValue.toString());
      }

      // This was not me editing, I'll go ahead and broadcast the value "down" to other services
      // (The value is from "me", but it was edited by someone else in Firebase so I'll mask it as "me"
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
        MvdHelper.sendBeanDownBroadcast(getApplicationContext(), source, target, mac, codePinValue);
      }

      // Find a forwarding where I am included
      List<Binding> bindings = Binding.find(Binding.class, "service = ?", TAG);
      for (Binding binding : bindings) {
        if (DEBUG) {
          Log.d(TAG, "Found binding for " + binding.getCode() + "/" + binding.getPin() + " to " + binding.getService());
        }

        // Get the target (For bindings this is always the BeanService in the DOWN direction)
        String target = BeanService.class.getSimpleName();

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
   * MVD implementation for the BeanListener. We need this because we want to be able of attaching
   * new listeners to all new beans. We could of course reuse the same listener for all beans...meh
   */
  private class MvdBeanListener implements BeanListener {

    private String mac;

    private MvdBeanListener(String mac) {
      this.mac = mac;
    }

    @Override
    public void onConnected() {
      Log.d(TAG, "Connected to a bean!");
    }

    @Override
    public void onConnectionFailed() {
      Log.e(TAG, "Connection failed!");
    }

    @Override
    public void onDisconnected() {
      if (DEBUG) {
        Log.d(TAG, "Disconnected from a bean!");
      }
    }

    @Override
    public void onSerialMessageReceived(byte[] bytes) {
      String msg = new String(bytes);

      if (DEBUG) {
        Log.d(TAG, "read [" + msg + "] from Bean with address [" + mac + "]");
      }

      CodePinValue codePinValue = parseBeanMessage(msg);


      if (codePinValue != null) {
        handleKeyValFromBean(mac, codePinValue);
      }

    }

    @Override
    public void onScratchValueChanged(int i, byte[] bytes) {
      // TODO: What is a "scratch" value?
    }
  }

  private CodePinValue parseBeanMessage(String msg) {
    String[] parts = msg.split("/");

    if(parts.length == 3) {
    //if (parts == null || parts.length < 3) {
      //parts = msg.split(":");

      //if (parts == null || parts.length >= 3) {
        return new CodePinValue(parts[0], parts[1], parts[2]);
      //}
    }

    Log.d(TAG, "Could not parse message");

    return null;
  }


  private void printAllConnectedBeans() {
    Iterator it = beans.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();

      String mac = (String) pair.getKey();
      Bean bean = (Bean) pair.getValue();

      Log.d(TAG, mac + " - " + bean.getDevice().getName());
    }
  }
}
