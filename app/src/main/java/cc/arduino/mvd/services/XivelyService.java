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

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;

/**
 * This is the Firebase integration of MVD.
 *
 * @author Andreas Goransson
 */
public class XivelyService extends Service {

  public static final String TAG = XivelyService.class.getSimpleName();

  private String url;

  private Firebase firebase;

  private String lastChangedCode = null;
  private String lastChangedPin = null;
  private String lastChangedValue = null;

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

    Log.d(TAG, "onStartCommmand1");
    if (intent != null) {
      url = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_URL);

      Firebase.setAndroidContext(getApplicationContext());

      firebase = new Firebase(url);

      firebase.addChildEventListener(childListener);
      Log.d(TAG, "onStartCommmand2");
    }

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    unregisterReceiver(broadcastReceiver);

    firebase.removeEventListener(childListener);

    firebase = null;
  }

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private ChildEventListener childListener = new ChildEventListener() {
    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
      String code = dataSnapshot.getKey();
      Object pin = dataSnapshot.getValue();

//      handleKeyValFromFirebase(key, val);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
      String code = dataSnapshot.getKey();
      Object pin = dataSnapshot.getValue();

//      handleKeyValFromFirebase(key, val) ;
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
    if (lastChangedCode != null && lastChangedPin != null && lastChangedValue != null) {
      if (!lastChangedCode.equals(code) && !lastChangedPin.equals(pin) && !lastChangedValue.equals(value)) {
        // This was not me, I'll go ahead and broadcast the value "down" to other services
        String target = "BluetoothService";
        MvdHelper.sendDownBroadcast(getApplicationContext(), TAG, target, code, pin, value);
      } else {
        // Do nothing, I just wrote this value myself!
      }
    } else {
      // Ignore the first read!
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

      String code = intent.getStringExtra(MvdHelper.EXTRA_CODE);
      String pin = intent.getStringExtra(MvdHelper.EXTRA_PIN);
      String value = intent.getStringExtra(MvdHelper.EXTRA_VALUE);

      Log.d(TAG, "TARGET: " + target);
      Log.d(TAG, "SENDER: " + sender);
      Log.d(TAG, "code: " + code);
      Log.d(TAG, "pin: " + pin);
      Log.d(TAG, "value: " + value);

      if (!sender.equals(TAG)) {
        // If we're getting values from another service
        if (action.equals(MvdHelper.ACTION_UP)) {
          if (target.equals(TAG)) {
            firebase.child(code).child(pin).setValue(value);
            lastChangedCode = code;
            lastChangedPin = pin;
            lastChangedValue = value;
          }
        }

        // Or if we've getting values from the "cloud" services
        else if (action.equals(MvdHelper.ACTION_DOWN)) {
          if (target.equals(TAG)) {
            firebase.child(code).child(pin).setValue(value);
            lastChangedCode = code;
            lastChangedPin = pin;
            lastChangedValue = value;
          }
        }
      } else {
        // Do nothing, this was me broadcasting
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