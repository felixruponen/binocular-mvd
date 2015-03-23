package cc.arduino.mvd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cc.arduino.mvd.services.FirebaseService;
import cc.arduino.mvd.services.HttpService;


public class MainActivity extends ActionBarActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  TextView firebase_in, http_in;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    setupFirebaseTest();

    setupHttpTest();
  }

  private void setupFirebaseTest() {
    firebase_in = (TextView) findViewById(R.id.firebase_in);
    Button firebase_out = (Button) findViewById(R.id.firebase_out);
    firebase_out.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent firebase = new Intent(MvdHelper.ACTION_UP);
        firebase.putExtra(MvdHelper.EXTRA_SENDER, "mvd");
        firebase.putExtra(MvdHelper.EXTRA_TARGET, FirebaseService.class.getSimpleName());
        firebase.putExtra(MvdHelper.EXTRA_CODE, "L");
        firebase.putExtra(MvdHelper.EXTRA_PIN, "13");
        firebase.putExtra(MvdHelper.EXTRA_VALUE, String.valueOf(((int) (Math.random() * 13))));
        sendBroadcast(firebase);
      }
    });
    Button firebase_start = (Button) findViewById(R.id.firebase_start);
    firebase_start.setOnClickListener(new View.OnClickListener() {
      boolean toggle = false;

      @Override
      public void onClick(View v) {
        toggle = !toggle;
        if (toggle) {
          Intent start = new Intent(MvdServiceReceiver.ACTION_START_SERVICE);
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_NAME, MvdServiceReceiver.FIREBASE);
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_URL, "https://mvdtest.firebaseio.com/");
          sendBroadcast(start);
        } else {
          Intent start = new Intent(MvdServiceReceiver.ACTION_STOP_SERVICE);
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_NAME, MvdServiceReceiver.FIREBASE);
          sendBroadcast(start);
        }
      }
    });
  }

  private void setupHttpTest() {
    http_in = (TextView) findViewById(R.id.http_in);
    Button http_out = (Button) findViewById(R.id.http_out);
    http_out.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent firebase = new Intent(MvdHelper.ACTION_UP);
        firebase.putExtra(MvdHelper.EXTRA_SENDER, "mvd");
        firebase.putExtra(MvdHelper.EXTRA_TARGET, HttpService.class.getSimpleName());
        firebase.putExtra(MvdHelper.EXTRA_CODE, "L");
        firebase.putExtra(MvdHelper.EXTRA_PIN, "13");
        firebase.putExtra(MvdHelper.EXTRA_VALUE, String.valueOf(((int) (Math.random() * 13))));
        sendBroadcast(firebase);
      }
    });
    Button http_start = (Button) findViewById(R.id.http_start);
    http_start.setOnClickListener(new View.OnClickListener() {
      boolean toggle = false;

      @Override
      public void onClick(View v) {
        toggle = true; //!toggle;
        if (toggle) {
          Intent start = new Intent(MvdServiceReceiver.ACTION_START_SERVICE);
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_NAME, MvdServiceReceiver.HTTP);
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_URL, "http://188.226.207.177/");
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_DELAY, 5000);
          sendBroadcast(start);
        } else {
          Intent start = new Intent(MvdServiceReceiver.ACTION_STOP_SERVICE);
          start.putExtra(MvdServiceReceiver.EXTRA_SERVICE_NAME, MvdServiceReceiver.HTTP);
          sendBroadcast(start);
        }
      }
    });
  }

}
