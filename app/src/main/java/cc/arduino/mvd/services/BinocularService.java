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

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
import cc.arduino.mvd.binocularsdk.web.IJsonCollectionConvertable;
import cc.arduino.mvd.binocularsdk.web.OnDone;
import cc.arduino.mvd.binocularsdk.webmodels.CollectionFactory;
import cc.arduino.mvd.binocularsdk.webmodels.DeviceCollection;
import cc.arduino.mvd.binocularsdk.webmodels.FlagCollection;
import cc.arduino.mvd.binocularsdk.webmodels.ICollectionState;
import cc.arduino.mvd.models.Binding;
import cc.arduino.mvd.models.CodePinValue;
import cc.arduino.mvd.models.ServiceRoute;

import cc.arduino.mvd.binocularsdk.web.Fetcher;

import static cc.arduino.mvd.MvdHelper.DEBUG;

/**
 * This is the http integration of MVD.
 *
 * @author Andreas Goransson, 2015-03-19
 */
public class BinocularService extends Service {

    public static final String TAG = BinocularService.class.getSimpleName();

    OkHttpClient client = new OkHttpClient();

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    private ScheduledFuture scheduledFuture;

    private String url;

    private int delay = 5000; // Delay in ms between GET requests

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

            // Make sure the url ends with a slash
            if (!url.endsWith("/")) {
                url = url + "/";
            }

            //delay = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_DELAY, 5000);
            delay = 2000;

            startGetRequests(delay);

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

        stopGetRequests();

        if (DEBUG) {
            Log.d(TAG, TAG + " stopped.");
        }
    }

    /**
     * Start the recurring GET's
     *
     * @param delay
     */
    private void startGetRequests(int delay) {

        Log.d(TAG, "Starting polling with " + delay + " ms");

        Runnable task = new Runnable() {
            @Override
            public void run() {


//        List<ServiceRoute> routes = ServiceRoute.find(ServiceRoute.class, "service1 = ? OR service2 = ?", TAG);
//        for (ServiceRoute route : routes) {
//          TODO: Find a solution to use service routes in HTTP GET's... probably need to locally store all details...
//          try {
//            get(url, "components/" + route.g)
//          } catch (IOException e) {
//            e.printStackTrace();
//          }
//        }

                //List<Binding> bindings = Binding.find(Binding.class, "service = ?", TAG);
                //for (final Binding binding : bindings) {
                        // TODO: Get device flags from binocular
                        Log.d(TAG, "Running binding");

                        try{


                        final IJsonCollectionConvertable manager = CollectionFactory.build_state("5528e947d7ad015decb3af13", null);

                        if(manager != null) {

                            Fetcher fetcher = new Fetcher();
                            fetcher.setMap(manager.getDefaultMap());
                            fetcher.setCollectionManager(manager);
                            fetcher.setDone(new OnDone() {
                                @Override
                                public void done() {
                                    //Execute next link if chaining is applied.
                                    try {

                                        Log.d(TAG, "Fetched Flags. Got: " + ((FlagCollection) manager).getFlags().length() + " values");

                                        Log.d(TAG, "Recieved: " + ((FlagCollection) manager).getFlags().toString());
                                        JSONObject flags = ((FlagCollection) manager).getFlags();

                                        if (flags != null) {

                                            //String value = flags.getString("is_full").equals("true") ? "1" : "0";

                                            Iterator<String> iterator = flags.keys();

                                            int i = 0;

                                            while (iterator.hasNext()) {
                                                String key = iterator.next();
                                                String value = flags.getString(key);

                                                handleKeyValFromHttp(manager.getCode(), String.valueOf(i), value);
                                                i++;
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                }
                            });

                            fetcher.execute();
                        }

                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                }
            //}
        };

        scheduledFuture = scheduler.scheduleAtFixedRate(
                task,
                //delay,
                10000,
                delay,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop the recurring GET's
     */
    private void stopGetRequests() {
        Log.d(TAG, "Stopping recurring requests");
        scheduledFuture.cancel(true);
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
    private void handleKeyValFromHttp(String code, String pin, String value) {
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
            /*
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
            }*/

            // Find a forwarding where I am included
            List<Binding> bindings = Binding.find(Binding.class, "service = ?", TAG);
            for (Binding binding : bindings) {
                if (DEBUG) {
                    Log.d(TAG, "Found binding for " + binding.getCode() + "/" + binding.getPin() + " to " + binding.getService());
                }

                // Get the target (For bindings this is always the BeanService in the DOWN direction)
                String target = BeanService.class.getSimpleName();

                String mac = binding.getMac();

                Log.d(TAG, "Sending down broadcast with mac: " + mac);

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


            Log.d(TAG, "Recieved intent");

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


                                Log.d(TAG, "TRYING TO UP DATA from: " + codePinValue.getCode());
                                //post(url + "components/" + codePinValue.getCode() + "/pins/" + codePinValue.getPin(), json.toString());


                            lastCodePinValue = codePinValue;
                        }
                    }

                    // Or if we've getting values from other "cloud" services (DOWN direction) we should write to Firebase too
                    else if (action.equals(MvdHelper.ACTION_DOWN)) {
                        // Make sure the value is intended for us
                        Log.d(TAG, "TRYING TO DOWN DATA from: " + codePinValue.getCode());

                        if (target.equals(TAG)) {

                                final String code = codePinValue.getCode();
                                final String value = codePinValue.getValue();

                                //post(url + "components/" + codePinValue.getCode() + "/pins/" + codePinValue.getPin(), json.toString());
                                Thread task = new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try{


                                            Log.i(TAG, "Sending device data: " + value);
                                            Log.i(TAG, "Code for sensor: " + code);
                                            if(value != null) {


                                                final IJsonCollectionConvertable manager = CollectionFactory.build_state(code, value);

                                                if(manager != null) {


                                                    Fetcher fetcher = new Fetcher();
                                                    fetcher.setMap(manager.getDefaultMap());
                                                    fetcher.setCollectionManager(manager);
                                                    fetcher.setDone(new OnDone() {

                                                        @Override
                                                        public void done() {
                                                            //Execute next link if chaining is applied.
                                                            Log.d(TAG, "Pushed data to server");
                                                        }
                                                    });

                                                    fetcher.execute();
                                                }
                                            }
                                        } catch(Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });


                                task.start();


                                lastCodePinValue = codePinValue;


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
                // TODO: remove the listner? (client. ... ?)

                unregisterReceiver(broadcastReceiver);

                stopSelf();
            }

        }
    };

}