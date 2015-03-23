package cc.arduino.mvd.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;
import cc.arduino.mvd.models.Binding;

/**
 * This is the http integration of MVD.
 *
 * @author Andreas Goransson
 */
public class HttpService extends Service {

  public static final String TAG = HttpService.class.getSimpleName();

  OkHttpClient client = new OkHttpClient();

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

  private ScheduledFuture scheduledFuture;

  private String url;

  private int delay = 5000; // Delay in ms between GET requests

  private boolean started = false;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "Starting HTTP");

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

      delay = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_DELAY, 5000);

      startGetRequests(delay);

      started = true;
    }

    return START_STICKY;
  }

  /**
   * Start the recurring GET's
   *
   * @param delay
   */
  private void startGetRequests(int delay) {
    Runnable task = new Runnable() {
      @Override
      public void run() {
        List<Binding> bindings = Binding.getAllBindings(TAG);
        for (Binding binding : bindings) {
          try {
            get(url + "components/" + binding.code + "/pins/" + binding.pin);
          } catch (IOException e) {
            e.printStackTrace();
          }
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
   * Stop the recurring GET's
   */
  private void stopGetRequests() {
    scheduledFuture.cancel(true);
  }


  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "Killing httpservice");

    unregisterReceiver(broadcastReceiver);

    stopGetRequests();
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
    String target = "BluetoothService";
    MvdHelper.sendDownBroadcast(getApplicationContext(), TAG, target, code, pin, value);
  }

  /**
   * Perform a POST request to the service
   *
   * @param url
   * @param json
   * @return
   * @throws IOException
   */
  private void post(String url, String json) {
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    RequestBody body = RequestBody.create(JSON, json);

    final Request request = new Request.Builder()
        .url(url)
        .post(body)
        .build();

    Call call = client.newCall(request);

    call.enqueue(new Callback() {
      @Override
      public void onFailure(final Request request, final IOException exception) {
        // Meh, don't do anything...
        exception.printStackTrace();
      }

      @Override
      public void onResponse(final Response response) throws IOException {
        // Meh, don't do anything...
        Log.d(TAG, request.toString());
      }
    });
  }

  /**
   * Get something from the service
   *
   * @param url
   * @return
   * @throws IOException
   */
  private void get(String url) throws IOException {
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    final Request request = new Request.Builder()
        .url(url)
        .get()
        .build();

    Call call = client.newCall(request);

    call.enqueue(new Callback() {
      @Override
      public void onFailure(final Request request, final IOException exception) {
        // Meh, don't do anything...
        exception.printStackTrace();
      }

      @Override
      public void onResponse(final Response response) throws IOException {
        // Meh, don't do anything...
        Log.d(TAG, request.toString());

        try {
          String body = new String(response.body().bytes());
          JSONObject json = new JSONObject(body);
          handleKeyValFromHttp(json.getString("code"), json.getString("pin"), json.getString("value"));
        } catch (JSONException e) {
          e.printStackTrace();
        }

      }
    });
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
            try {
              JSONObject json = new JSONObject();
              json.put("value", value);

              post(url + "components/" + code + "/pins/" + pin, json.toString());
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        }

        // Or if we've getting values from the "cloud" services
        else if (action.equals(MvdHelper.ACTION_DOWN)) {
          if (target.equals(TAG)) {
            try {
              JSONObject json = new JSONObject();
              json.put("value", value);

              post(url + "components/" + code + "/pins/" + pin, json.toString());
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        }
      } else {
        // Do nothing, this was me broadcasting
      }

      // If someone told me to kill myself...
      if (action.equals(MvdHelper.ACTION_KILL) && target.equals(TAG)) {
        // TODO: Remvoe listeners

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }

    }
  };

}