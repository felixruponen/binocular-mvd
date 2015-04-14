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

package cc.arduino.mvd;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;
import cc.arduino.mvd.services.BeanService;
import cc.arduino.mvd.services.BinocularService;
import cc.arduino.mvd.services.ElisService;
import cc.arduino.mvd.services.FirebaseService;
import cc.arduino.mvd.services.HttpService;
import cc.arduino.mvd.services.MqttService;
import cc.arduino.mvd.services.XivelyService;

/**
 * Created by ksango on 19/03/15.
 */
public class MvdHelper {

  private static final String TAG = MvdHelper.class.getSimpleName();

  private static final String PREFS = "mvd_prefs";

  public static boolean DEBUG = false;

  public static final String ACTION_UP = "cc.arduino.mvd.helper.actions.UP";
  public static final String ACTION_DOWN = "cc.arduino.mvd.helper.actions.DOWN";
  public static final String ACTION_KILL = "cc.arduino.mvd.helper.actions.KILL";
  public static final String ACTION_SCAN = "cc.arduino.mvd.helper.actions.SCAN";


  public static final String EXTRA_TARGET = "cc.arduino.mvd.helper.extras.TARGET";
  public static final String EXTRA_SENDER = "cc.arduino.mvd.helper.extras.SENDER";

  public static final String EXTRA_BINDING_NAME = "cc.arduino.mvd.helper.extras.BINDING_NAME";
  public static final String EXTRA_CODE = "cc.arduino.mvd.helper.extras.CODE";
  public static final String EXTRA_PIN = "cc.arduino.mvd.helper.extras.PIN";
  public static final String EXTRA_VALUE = "cc.arduino.mvd.helper.extras.VALUE";

  public static final String EXTRA_CODE_PIN_VALUE = "cc.arduino.mvd.helper.extras.CODE_PIN_VALUE";

  // Configuration intent
  public static final String ACTION_ENABLE_DEBUG = "cc.arduino.mvd.helper.actions.ENABLE_DEBUG";
  public static final String ACTION_DISABLE_DEBUG = "cc.arduino.mvd.helper.actions.DISABLE_DEBUG";

  public static final String ACTION_SET_CONFIGURATION = "cc.arduino.mvd.helper.actions.SET_CONFIGURATION";
  public static final String ACTION_GET_CONFIGURATION = "cc.arduino.mvd.helper.actions.GET_CONFIGURATION";

  public static final String EXTRA_PERSIST = "cc.arduino.mvd.helper.extras.PERSIST";
  public static final String EXTRA_ID = "cc.arduino.mvd.helper.extras.ID";

  /**
   * Sends a DOWN broadcast to a Bean, the Bean always requires the MAC to be set!
   *
   * @param ctx
   * @param sender
   * @param target
   * @param mac
   * @param codePinValue
   */
  public static void sendBeanDownBroadcast(
      Context ctx,
      String sender,
      String target,
      String mac,
      CodePinValue codePinValue
  ) {
    if (DEBUG) {
      Log.d(TAG, "sendDownBroadcast [" + sender + "] -> [" + target + "]");
      Log.d(TAG, codePinValue.toString());
    }

    Intent down = new Intent(ACTION_DOWN);
    down.putExtra(EXTRA_TARGET, target);
    down.putExtra(EXTRA_SENDER, sender);
    down.putExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC, mac);
    down.putExtra(EXTRA_CODE_PIN_VALUE, codePinValue);
    ctx.sendBroadcast(down);
  }

  /**
   * Sends a DOWN broadcast.
   *
   * @param ctx
   * @param sender
   * @param target
   * @param codePinValue
   */
  public static void sendDownBroadcast(
      Context ctx,
      String sender,
      String target,
      CodePinValue codePinValue
  ) {
    if (DEBUG) {
      Log.d(TAG, "sendDownBroadcast [" + sender + "] -> [" + target + "]");
      Log.d(TAG, codePinValue.toString());
    }

    Intent down = new Intent(ACTION_DOWN);
    down.putExtra(EXTRA_TARGET, target);
    down.putExtra(EXTRA_SENDER, sender);
    down.putExtra(EXTRA_CODE_PIN_VALUE, codePinValue);
    ctx.sendBroadcast(down);
  }

  /**
   * Sends a UP broadcast, specifically for Bean. It includes the MAC.
   *
   * @param ctx
   * @param sender
   * @param target
   * @param codePinValue
   */
  public static void sendBeanUpBroadcast(
      Context ctx,
      String sender,
      String target,
      String mac,
      CodePinValue codePinValue
  ) {
    if (DEBUG) {
      Log.d(TAG, "sendUpBroadcast [" + sender + "] -> [" + target + "]");
      Log.d(TAG, codePinValue.toString());
    }

    Intent up = new Intent(ACTION_UP);
    up.putExtra(EXTRA_TARGET, target);
    up.putExtra(EXTRA_SENDER, sender);
    up.putExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC, mac);
    up.putExtra(EXTRA_CODE_PIN_VALUE, codePinValue);
    ctx.sendBroadcast(up);
  }

  /**
   * Sends a UP broadcast.
   *
   * @param ctx
   * @param sender
   * @param target
   * @param codePinValue
   */
  public static void sendUpBroadcast(
      Context ctx,
      String sender,
      String target,
      CodePinValue codePinValue
  ) {
    if (DEBUG) {
      Log.d(TAG, "sendUpBroadcast [" + sender + "] -> [" + target + "]");
      Log.d(TAG, codePinValue.toString());
    }

    Intent up = new Intent(ACTION_UP);
    up.putExtra(EXTRA_TARGET, target);
    up.putExtra(EXTRA_SENDER, sender);
    up.putExtra(EXTRA_CODE_PIN_VALUE, codePinValue);
    ctx.sendBroadcast(up);
  }

  /**
   * Send the DOWN broadcast
   *
   * @param ctx
   * @param sender
   * @param target
   * @param code
   * @param pin
   * @param value
   */
  @Deprecated
  public static void sendDownBroadcast(
      Context ctx,
      String sender,
      String target,
      String code,
      String pin,
      String value
  ) {
    Intent down = new Intent(ACTION_DOWN);
    down.putExtra(EXTRA_TARGET, target);
    down.putExtra(EXTRA_SENDER, sender);
    down.putExtra(EXTRA_CODE, code);
    down.putExtra(EXTRA_PIN, pin);
    down.putExtra(EXTRA_VALUE, value);
    ctx.sendBroadcast(down);
  }

  /**
   * @param ctx
   * @param serviceClass
   * @return
   */
  public static boolean isServiceRunning(Context ctx, Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param name
   * @return
   */
  public Class getServiceClass(String name) {
    if (name.equals(FirebaseService.class.getSimpleName())) {
      return FirebaseService.class;
    } else if (name.equals(HttpService.class.getSimpleName())) {
      return HttpService.class;
    } else if (name.equals(MqttService.class.getSimpleName())) {
      return MqttService.class;
    } else if (name.equals(ElisService.class.getSimpleName())) {
      return ElisService.class;
    }

    return null;
  }

  /**
   * Determine if the supplied service name is valid
   *
   * @param service The service name
   * @return true if valid
   */
  public static boolean isValidService(String service) {
    boolean valid = false;

    if(service == null) {
        return false;
    }

    // BEAN
    if (service.equals(BeanService.class.getSimpleName())) {
      return true;
    }

    // Elis
    else if (service.equals(ElisService.class.getSimpleName())) {
      return true;
    }

    // Firebase
    else if (service.equals(FirebaseService.class.getSimpleName())) {
      return true;
    }

    // Http
    else if (service.equals(HttpService.class.getSimpleName())) {
      return true;
    }

    // Binocular
    else if (service.equals(BinocularService.class.getSimpleName())) {
        return true;
    }

    // MQTT
    else if (service.equals(MqttService.class.getSimpleName())) {
      return true;
    }

    // Xively
    else if (service.equals(XivelyService.class.getSimpleName())) {
      return true;
    }

    return valid;
  }

  /**
   * This will get the service target based on a context (Service name).
   *
   * @param route   The route
   * @param context The service name where we're getting the target from
   * @return The "other" service name
   */
  public static String getServiceTarget(ServiceRoute route, String context) {
    String target = route.getService1();

    if (route.getService1().equals(context))
      target = route.getService2();
    else if (route.getService2().equals(context))
      target = route.getService1();

    // Just in case, trim the of white spaces
    return target.trim();
  }

  /**
   * Set the MVD id.
   *
   * @param context The application context
   * @param id      The id
   */
  public static void setMvdId(Context context, String id) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("id", id);
    editor.apply();
  }

  /**
   * Get the MVD id. Default is "mvd".
   *
   * @param context The application context
   * @return The id
   */
  public static String getMvdId(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    return prefs.getString("id", "mvd");
  }

  /**
   * Enable the debugging.
   *
   * @param context The application context
   * @param persist If the debug should be persisted to next restart.
   */
  public static void enableDebug(Context context, boolean persist) {
    DEBUG = true;
    if (persist) {
      SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putBoolean("debug", true);
      editor.apply();
    }
  }

  /**
   * Disable the debugging.
   *
   * @param context The application context
   * @param persist If the debug should be "unpersisted".
   */
  public static void disableDebug(Context context, boolean persist) {
    DEBUG = false;
    if (persist) {
      SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      editor.remove("debug");
      editor.apply();
    }
  }

  /**
   * Attempt to load the debugging variable from from persistent storage. Defaults to "false".
   *
   * @param context The application context
   */
  public static void loadDebug(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    DEBUG = prefs.getBoolean("debug", false);
  }

}
