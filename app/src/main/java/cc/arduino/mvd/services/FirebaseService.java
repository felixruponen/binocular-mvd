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

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.List;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;
import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;

import static cc.arduino.mvd.MvdHelper.DEBUG;

/**
 * This is the Firebase integration of MVD.
 *
 * @author Andreas Goransson, 2015-03-13
 */
public class FirebaseService extends Service {

  public static final String TAG = FirebaseService.class.getSimpleName();

  private String url;

  private Firebase firebase;

  private boolean started = false;

  private CodePinValue lastCodePinValue;

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

      Firebase.setAndroidContext(getApplicationContext());

      firebase = new Firebase(url);

      firebase.addChildEventListener(childListener);

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

    firebase.removeEventListener(childListener);

    firebase = null;

    if (DEBUG) {
      Log.d(TAG, TAG + " stopped.");
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private ChildEventListener childListener = new ChildEventListener() {
    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
      String code = dataSnapshot.getKey();
      Iterable<DataSnapshot> pinValues = dataSnapshot.getChildren();
      for (DataSnapshot pinValue : pinValues) {
        String pin = pinValue.getKey();
        String value = pinValue.getValue().toString();

        handleKeyValFromFirebase(code, pin, value);
      }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
      String code = dataSnapshot.getKey();
      Iterable<DataSnapshot> pinValues = dataSnapshot.getChildren();
      for (DataSnapshot pinValue : pinValues) {
        String pin = pinValue.getKey();
        String value = pinValue.getValue().toString();

        handleKeyValFromFirebase(code, pin, value);
      }
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(FirebaseError firebaseError) {

    }
  };

  /**
   * This will handle the key-val from Firebase. Basically it determines if I should send a DOWN
   * broadcast or not depending on the last value I wrote UP.
   *
   * @param code
   * @param pin
   * @param value
   */
  private void handleKeyValFromFirebase(String code, String pin, String value) {
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
        Log.d(TAG, "I got the following from Firebase:");
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
              firebase.child(codePinValue.getCode()).child(codePinValue.getPin()).setValue(codePinValue.getValue());

              lastCodePinValue = codePinValue;
            }
          }

          // Or if we've getting values from other "cloud" services (DOWN direction) we should write to Firebase too
          else if (action.equals(MvdHelper.ACTION_DOWN)) {
            // Make sure the value is intended for us
            if (target.equals(TAG)) {
              firebase.child(codePinValue.getCode()).child(codePinValue.getPin()).setValue(codePinValue.getValue());

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
        firebase.removeEventListener(childListener);

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }

    }
  };

}