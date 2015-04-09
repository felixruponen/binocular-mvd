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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.ServiceRoute;
import cc.arduino.mvd.services.BeanService;
import cc.arduino.mvd.services.ElisService;
import cc.arduino.mvd.services.FirebaseService;
import cc.arduino.mvd.services.HttpService;
import cc.arduino.mvd.services.MqttService;
import cc.arduino.mvd.services.XivelyService;

import static cc.arduino.mvd.MvdHelper.DEBUG;
import static cc.arduino.mvd.MvdHelper.EXTRA_BINDING_NAME;
import static cc.arduino.mvd.MvdHelper.EXTRA_CODE;
import static cc.arduino.mvd.MvdHelper.EXTRA_PIN;

/**
 * This receiver starts and stops service connections when requested
 *
 * @author Andreas Goransson, 2015-03-19
 */
public class MvdServiceReceiver extends BroadcastReceiver {

  private static final String TAG = MvdServiceReceiver.class.getSimpleName();

  public static final String ACTION_CREATE_BINDING = "cc.arduino.mvd.services.actions.CREATE_BINDING";
  public static final String ACTION_LIST_BINDINGS = "cc.arduino.mvd.services.actions.LIST_BINDINGS";
  public static final String ACTION_DELETE_BINDING = "cc.arduino.mvd.services.actions.DELETE_BINDING";
  public static final String ACTION_CLEAR_BINDINGS = "cc.arduino.mvd.services.actions.CLEAR_BINDINGS";

  public static final String ACTION_ADD_ROUTE = "cc.arduino.mvd.services.actions.ADD_ROUTE";
  public static final String ACTION_LIST_ROUTES = "cc.arduino.mvd.services.actions.LIST_ROUTES";
  public static final String ACTION_DELETE_ROUTE = "cc.arduino.mvd.services.actions.DELETE_ROUTE";
  public static final String ACTION_CLEAR_ROUTES = "cc.arduino.mvd.services.actions.CLEAR_ROUTES";

  public static final String ACTION_START_SERVICE = "cc.arduino.mvd.services.actions.START_SERVICE";
  public static final String ACTION_STOP_SERVICE = "cc.arduino.mvd.services.actions.STOP_SERVICE";

  public static final String EXTRA_SERVICE_NAME = "cc.arduino.mvd.services.extras.NAME";
  public static final String EXTRA_SERVICE_NAMES = "cc.arduino.mvd.services.extras.NAMES";
  public static final String EXTRA_SERVICE_URL = "cc.arduino.mvd.services.extras.URL";
  public static final String EXTRA_SERVICE_PORT = "cc.arduino.mvd.services.extras.PORT";
  public static final String EXTRA_SERVICE_DELAY = "cc.arduino.mvd.services.extras.DELAY";
  public static final String EXTRA_SERVICE_MAC = "cc.arduino.mvd.services.extras.MAC";
  public static final String EXTRA_SERVICE_TIMEOUT = "cc.arduino.mvd.services.extras.TIMEOUT";
  public static final String EXTRA_SERVICE_API_KEY = "cc.arduino.mvd.services.extras.API_KEY";
  public static final String EXTRA_SERVICE_FEED_ID = "cc.arduino.mvd.services.extras.FEED_ID";

  // Bean specific actions
  public static final String ACTION_ADD_BEAN = "cc.arduino.mvd.services.actions.ADD_BEAN";
  public static final String ACTION_REMOVE_BEAN = "cc.arduino.mvd.services.actions.REMOVE_BEAN";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

//
//    String[] names = null;
//    if (intent.hasExtra(EXTRA_SERVICE_NAMES))
//      names = intent.getStringArrayExtra(EXTRA_SERVICE_NAMES);

    // A service was requested to start
    if (action.equals(ACTION_START_SERVICE)) {
      String name = intent.getStringExtra(EXTRA_SERVICE_NAME);

      if (!MvdHelper.isValidService(name)) {
        Log.e(TAG, "Invalid service name: " + name);
        return;
      }

      // Start Firebase
      if (name.equals(FirebaseService.class.getSimpleName())) {
        String url = intent.getStringExtra(EXTRA_SERVICE_URL);

        if (DEBUG) {
          Log.d(TAG, "Starting FIREBASE service");
        }
        Intent firebase = new Intent(context, FirebaseService.class);
        firebase.putExtra(EXTRA_SERVICE_URL, url);
        context.startService(firebase);
      }

      // Start XIVELY
      else if (name.equals(XivelyService.class.getSimpleName())) {
        String apiKey = intent.getStringExtra(EXTRA_SERVICE_API_KEY);
        String feedId = intent.getStringExtra(EXTRA_SERVICE_FEED_ID);

        if (DEBUG) {
          Log.d(TAG, "Starting XIVELY service");
        }
        Intent xively = new Intent(context, XivelyService.class);
        xively.putExtra(EXTRA_SERVICE_API_KEY, apiKey);
        xively.putExtra(EXTRA_SERVICE_FEED_ID, feedId);
        context.startService(xively);
      }

      // Start ELIS
      else if (name.equals(ElisService.class.getSimpleName())) {
        String url = intent.getStringExtra(EXTRA_SERVICE_URL);
        int port = intent.getIntExtra(EXTRA_SERVICE_PORT, 11414);

        if (DEBUG) {
          Log.d(TAG, "Starting ELIS service");
        }

        Intent elis = new Intent(context, ElisService.class);
        elis.putExtra(EXTRA_SERVICE_URL, url);
        elis.putExtra(EXTRA_SERVICE_PORT, port);
        context.startService(elis);
      }

      // Start HTTP
      else if (name.equals(HttpService.class.getSimpleName())) {
        String url = intent.getStringExtra(EXTRA_SERVICE_URL);
        int delay = intent.getIntExtra(EXTRA_SERVICE_DELAY, 5000);

        if (DEBUG) {
          Log.d(TAG, "Starting HTTP service");
        }
        Intent http = new Intent(context, HttpService.class);
        http.putExtra(EXTRA_SERVICE_URL, url);
        http.putExtra(EXTRA_SERVICE_DELAY, delay);
        context.startService(http);
      }

      // Start MQTT
      else if (name.equals(MqttService.class.getSimpleName())) {
        String url = intent.getStringExtra(EXTRA_SERVICE_URL);
        int port = intent.getIntExtra(EXTRA_SERVICE_PORT, 1883);

        if (DEBUG) {
          Log.d(TAG, "Starting MQTT service");
        }
        Intent mqtt = new Intent(context, MqttService.class);
        mqtt.putExtra(EXTRA_SERVICE_URL, url);
        mqtt.putExtra(EXTRA_SERVICE_PORT, port);
        context.startService(mqtt);
      }

      // Start Bean
      else if (name.equals(BeanService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Starting BEAN service");
        }
        Intent bean = new Intent(context, BeanService.class);
        context.startService(bean);
      }
    }

    // A service was requested to stop
    else if (action.equals(ACTION_STOP_SERVICE)) {
      String name = intent.getStringExtra(EXTRA_SERVICE_NAME);

      if (!MvdHelper.isValidService(name)) {
        Log.e(TAG, "Invalid service name: " + name);
        return;
      }

      // Stop Firebase
      if (name.equals(FirebaseService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Stopping FIREBASE service");
        }
        Intent firebase = new Intent(context, FirebaseService.class);
        context.stopService(firebase);
      }

      // Stop XIVELY
      else if (name.equals(XivelyService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Stopping XIVELY service");
        }
        Intent xively = new Intent(context, XivelyService.class);
        context.stopService(xively);
      }

      // Stop ELIS
      else if (name.equals(ElisService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Stopping ELIS service");
        }
        Intent elis = new Intent(context, ElisService.class);
        context.stopService(elis);
      }

      // Stop HTTP
      else if (name.equals(HttpService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Stopping HTTP service");
        }
        Intent http = new Intent(context, HttpService.class);
        context.stopService(http);
      }

      // Stop MQTT
      else if (name.equals(MqttService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Stopping MQTT service");
        }
        Intent mqtt = new Intent(context, MqttService.class);
        context.stopService(mqtt);
      }

      // Stop BEAN
      else if (name.equals(BeanService.class.getSimpleName())) {
        if (DEBUG) {
          Log.d(TAG, "Stopping BEAN service");
        }
        Intent bean = new Intent(context, BeanService.class);
        context.stopService(bean);
      }
    }

    // Create a binding
    else if (action.equals(ACTION_CREATE_BINDING)) {
      String service = intent.getStringExtra(EXTRA_SERVICE_NAME);

      if (MvdHelper.isValidService(service)) {
        String bindingName = intent.getStringExtra(EXTRA_BINDING_NAME);
        String code = intent.getStringExtra(EXTRA_CODE);
        String pin = intent.getStringExtra(EXTRA_PIN);

        Binding binding = new Binding();
        binding.name = bindingName;
        binding.service = service;
        binding.code = code;
        binding.pin = pin;
        binding.save();
      }
    }

    // List all bindings
    else if (action.equals(ACTION_LIST_BINDINGS)) {
      List<Binding> bindings = Binding.listAll(Binding.class);

      Log.d(TAG, "Listing bindings:");
      for (Binding binding : bindings) {
        Log.d(TAG, binding.toString());
      }
      Log.d(TAG, "(found a total of " + bindings.size() + " bindings)");
    }

    // Delete a binding
    else if (action.equals(ACTION_DELETE_BINDING)) {
      String service = null;
      if (intent.hasExtra(EXTRA_SERVICE_NAME))
        service = intent.getStringExtra(EXTRA_SERVICE_NAME);

      String mac = null;
      if (intent.hasExtra(EXTRA_SERVICE_MAC))
        mac = intent.getStringExtra(EXTRA_SERVICE_MAC);

      String code = null;
      if (intent.hasExtra(EXTRA_CODE))
        code = intent.getStringExtra(EXTRA_CODE);

      String pin = null;
      if (intent.hasExtra(EXTRA_PIN))
        pin = intent.getStringExtra(EXTRA_PIN);

      // If we decided to kill bindings to a service...
      if (service != null) {
        List<Binding> bindings = Binding.find(Binding.class, "service = ?", service);

        int size = bindings.size();

        for (Binding binding : bindings) {
          binding.delete();
        }

        if (DEBUG) {
          Log.d(TAG, "Deleted " + size + " bindings.");
        }
      }

      // Or, if we selected a CODE and a PIN...
      else if (mac != null && code != null && pin != null) {
        List<Binding> bindings = Binding.find(Binding.class, "mac = ? AND code = ? AND pin = ?", mac, code, pin);

        int size = bindings.size();

        for (Binding binding : bindings) {
          binding.delete();
        }

        if (DEBUG) {
          Log.d(TAG, "Deleted " + size + " bindings.");
        }
      }
    }

    // Clear all bindings
    else if (action.equals(ACTION_CLEAR_BINDINGS)) {
      Binding.deleteAll(Binding.class);

      Log.d(TAG, "Deleted all bindings.");
    }

    // Add a service route
    else if (action.equals(ACTION_ADD_ROUTE)) {
      String[] services = intent.getStringArrayExtra(EXTRA_SERVICE_NAMES);

      // Not enough services passed in the array
      if (services.length != 2) {
        Log.e(TAG, "Need exactly two (2) service to create a forwarding");
      }

      // Go ahead and attempt the forwarding
      else {
        String service1 = services[0].trim();
        String service2 = services[1].trim();

        boolean service1Valid = MvdHelper.isValidService(service1);
        boolean service2Valid = MvdHelper.isValidService(service2);

        // Invalid names, print error
        if (!service1Valid || !service2Valid) {
          Log.e(TAG, "At least one service name was not valid. " + service1 + " (" + (service1Valid ? "valid" : "invalid") + "), " + service2 + " (" + (service2Valid ? "valid" : "invalid") + ")");
        }

        // Valid names, go ahead and attempt the forwarding
        else {
          // Try to find the service route
          List<ServiceRoute> result = ServiceRoute.find(ServiceRoute.class, "service1 = ? and service2 = ?", service1, service2);
          if (result.size() <= 0) {
            // No forwarding was found, attempt the reverse loopup
            result = ServiceRoute.find(ServiceRoute.class, "service1 = ? and service2 = ?", service2, service1);
            if (result.size() <= 0) {
              if (DEBUG) {
                Log.d(TAG, "Created service forwarding between " + service1 + " and " + service2);
              }

              // Not found, create the forwarding
              ServiceRoute serviceRoute = new ServiceRoute(service1, service2);
              serviceRoute.save();
            } else {
              if (DEBUG) {
                Log.e(TAG, "Service forwarding was already detected between " + service1 + " and " + service2 + ", skipping.");
              }
            }

          } else {
            // Service forwarding was found, skip adding it.
            Log.e(TAG, "Service forwarding was already detected between " + service1 + " and " + service2 + ", skipping.");
          }
        }
      }
    }

    // List all service routes
    else if (action.equals(ACTION_LIST_ROUTES)) {
      List<ServiceRoute> routes = ServiceRoute.listAll(ServiceRoute.class);

      Log.d(TAG, "Listing service routes:");
      for (ServiceRoute route : routes) {
        Log.d(TAG, route.toString());
      }
      Log.d(TAG, "(found a total of " + routes.size() + " routes)");
    }

    // Delete a service route
    else if (action.equals(ACTION_DELETE_ROUTE)) {
      String[] services = intent.getStringArrayExtra(EXTRA_SERVICE_NAMES);

      // Not enough services passed in the array
      if (services.length != 2) {
        Log.e(TAG, "Need exactly two (2) services to delete a service route");
      }

      // Go ahead and attempt the forwarding
      else {
        String service1 = services[0].trim();
        String service2 = services[1].trim();

        // Try to find the service route
        List<ServiceRoute> routes = ServiceRoute.find(ServiceRoute.class, "service1 = ? and service2 = ?", service1, service2);

        routes.addAll(ServiceRoute.find(ServiceRoute.class, "service1 = ? and service2 = ?", service2, service1));

        int size = routes.size();

        for (ServiceRoute route : routes) {
          route.delete();
        }

        if (DEBUG) {
          Log.d(TAG, "Deleted " + size + " routes.");
        }
      }
    }

    // Clear all Service routes
    else if (action.equals(ACTION_CLEAR_ROUTES)) {
      ServiceRoute.deleteAll(ServiceRoute.class);

      Log.d(TAG, "Deleted all service routes.");
    }

    // Enable debugging
    else if (action.equals(MvdHelper.ACTION_ENABLE_DEBUG)) {
      Log.d(TAG, "Enabling debug");

      boolean persist = intent.getBooleanExtra(MvdHelper.EXTRA_PERSIST, false);
      MvdHelper.enableDebug(context.getApplicationContext(), persist);
    }

    // Disable debugging
    else if (action.equals(MvdHelper.ACTION_DISABLE_DEBUG)) {
      Log.d(TAG, "Disabling debug");

      boolean persist = intent.getBooleanExtra(MvdHelper.EXTRA_PERSIST, false);
      MvdHelper.disableDebug(context.getApplicationContext(), persist);
    }

    // Set the configuration
    else if (action.equals(MvdHelper.ACTION_SET_CONFIGURATION)) {
      if (intent.hasExtra(MvdHelper.EXTRA_ID)) {

      }
    }

    // Print the configuration
    else if (action.equals(MvdHelper.ACTION_GET_CONFIGURATION)) {

    }
  }

}
