package cc.arduino.mvd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.services.BeanService;
import cc.arduino.mvd.services.FirebaseService;
import cc.arduino.mvd.services.HttpService;

/**
 * This receiver starts and stops service connections when requested
 */
public class MvdServiceReceiver extends BroadcastReceiver {

  private static final String TAG = MvdServiceReceiver.class.getSimpleName();

  public static final String ACTION_CREATE_BINDING = "cc.arduino.mvd.services.actions.CREATE_BINDING";
  public static final String ACTION_DELETE_BINDING = "cc.arduino.mvd.services.actions.DELETE_BINDING";

  public static final String ACTION_START_SERVICE = "cc.arduino.mvd.services.actions.START_SERVICE";
  public static final String ACTION_STOP_SERVICE = "cc.arduino.mvd.services.actions.STOP_SERVICE";

  public static final String EXTRA_SERVICE_NAME = "cc.arduino.mvd.services.extras.NAME";
  public static final String EXTRA_SERVICE_URL = "cc.arduino.mvd.services.extras.URL";
  public static final String EXTRA_SERVICE_DELAY = "cc.arduino.mvd.services.extras.DELAY";
  public static final String EXTRA_SERVICE_MAC = "cc.arduino.mvd.services.extras.MAC";
  public static final String EXTRA_SERVICE_TIMEOUT = "cc.arduino.mvd.services.extras.TIMEOUT";

  public static final String EXTRA_COMPONENT_NAME = "cc.arduino.mvd.services.extras.COMPONENT_NAME";
  public static final String EXTRA_CODE = "cc.arduino.mvd.services.extras.CODE";
  public static final String EXTRA_PIN = "cc.arduino.mvd.services.extras.PIN";

  public static final String FIREBASE = "FIREBASE";
  public static final String XIVELY = "XIVELY";
  public static final String ELIS = "ELIS";
  public static final String HTTP = "HTTP";
  public static final String BEAN = "BEAN";

  public MvdServiceReceiver() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    String name = intent.getStringExtra(EXTRA_SERVICE_NAME);

    String url = null;
    if (intent.hasExtra(EXTRA_SERVICE_URL))
      url = intent.getStringExtra(EXTRA_SERVICE_URL);

    int delay = 0;
    if (intent.hasExtra(EXTRA_SERVICE_DELAY))
      delay = intent.getIntExtra(EXTRA_SERVICE_DELAY, 5000);

    // A service was requested to start
    if (action.equals(ACTION_START_SERVICE)) {
      // Start Firebase
      if (name.equals(FIREBASE)) {
        Log.d(TAG, "Starting FIREBASE service");
        Intent firebase = new Intent(context, FirebaseService.class);
        firebase.putExtra(EXTRA_SERVICE_URL, url);
        context.startService(firebase);
      }

      // Start XIVELY
      else if (name.equals(XIVELY)) {

      }

      // Start ELIS
      else if (name.equals(ELIS)) {

      }

      // Start HTTP
      else if (name.equals(HTTP)) {
        Log.d(TAG, "Starting HTTP service");
        Intent http = new Intent(context, HttpService.class);
        http.putExtra(EXTRA_SERVICE_URL, url);
        http.putExtra(EXTRA_SERVICE_DELAY, delay);
        context.startService(http);
      }

      // Start Bean
      else if (name.equals(BEAN)) {
        Log.d(TAG, "Starting BEAN service");
        Intent bean = new Intent(context, BeanService.class);
        context.startService(bean);
      }
    }

    // A service was requested to stop
    else if (action.equals(ACTION_STOP_SERVICE)) {
      // Stop Firebase
      if (name.equals(FIREBASE)) {
        Log.d(TAG, "Stopping FIREBASE service");
        Intent firebase = new Intent(context, FirebaseService.class);
        context.stopService(firebase);
      }

      // Stop XIVELY
      else if (name.equals(XIVELY)) {
        // TODO
      }

      // Stop ELIS
      else if (name.equals(ELIS)) {
        // TODO
      }

      // Stop HTTP
      else if (name.equals(HTTP)) {
        Log.d(TAG, "Stopping HTTP service");
        Intent http = new Intent(context, HttpService.class);
        context.stopService(http);
      }

      // Stop BEAN
      else if (name.equals(BEAN)) {
        Log.d(TAG, "Stopping BEAN service");
        Intent bean = new Intent(context, BeanService.class);
        context.stopService(bean);
      }
    }

    // Create a binding
    else if (action.equals(ACTION_CREATE_BINDING)) {
      String service = intent.getStringExtra(EXTRA_SERVICE_NAME);
      String component = intent.getStringExtra(EXTRA_COMPONENT_NAME);
      String code = intent.getStringExtra(EXTRA_CODE);
      String pin = intent.getStringExtra(EXTRA_PIN);

      Binding binding = new Binding();
      binding.service = service;
      binding.component = component;
      binding.code = code;
      binding.pin = pin;
      binding.save();

      // 3. Create record in database
    }

    // Delete a binding
    else if (action.equals(ACTION_DELETE_BINDING)) {
      // TODO
    }

  }

}
