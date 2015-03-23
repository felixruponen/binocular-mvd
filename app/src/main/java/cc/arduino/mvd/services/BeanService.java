package cc.arduino.mvd.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import com.firebase.client.Firebase;

import java.util.UUID;

import cc.arduino.mvd.MvdHelper;
import cc.arduino.mvd.MvdServiceReceiver;

/**
 * This is the Bean (Bluetooth LE) integration of MVD.
 *
 * @author Andreas Goransson
 */
public class BeanService extends Service {

  private static final String TAG = BeanService.class.getSimpleName();

  public static final String ACTION_GATT_CONNECTED = "cc.arduino.mvd.bean.actions.GATT_CONNECTED";
  public static final String ACTION_GATT_DISCONNECTED = "cc.arduino.mvd.bean.actions.GATT_DISCONNECTED";
  public static final String ACTION_GATT_SERVICES_DISCOVERED = "cc.arduino.mvd.bean.actions.GATT_SERVICES_DISCOVERED";
  public static final String ACTION_GATT_DATA_AVAILABLE = "cc.arduino.mvd.bean.actions.DATA_AVAILABLE";
  public static final String EXTRA_DATA = "cc.arduino.mvd.bean.extras.DATA";

  public static final int STATE_DISCONNECTED = 1;
  public static final int STATE_CONNECTING = 2;
  public static final int STATE_CONNECTED = 3;

  private BluetoothManager bluetoothManager;

  private BluetoothAdapter bluetoothAdapter;

  private int connectionState = STATE_DISCONNECTED;

  private Handler handler;

  private long scanTimeout = 3000;

  private String mac = null;

  private UUID BEAN_UUID = UUID.fromString("a495ff10-c5b1-4b44-b512-1370f02d74de");

  @Override
  public void onCreate() {
    super.onCreate();

    handler = new Handler(getMainLooper());

    bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

    bluetoothAdapter = bluetoothManager.getAdapter();

    IntentFilter filter = new IntentFilter();
    filter.addAction(MvdHelper.ACTION_UP);
    filter.addAction(MvdHelper.ACTION_DOWN);
    filter.addAction(MvdHelper.ACTION_KILL);
    filter.addAction(MvdHelper.ACTION_SCAN);

    registerReceiver(broadcastReceiver, filter);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "onStartCommmand1");
    if (intent != null) {
      mac = intent.getStringExtra(MvdServiceReceiver.EXTRA_SERVICE_MAC);
      scanTimeout = intent.getIntExtra(MvdServiceReceiver.EXTRA_SERVICE_TIMEOUT, 3000);

      // TODO: Start something?
      Log.d(TAG, "onStartCommmand2");
    }

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    unregisterReceiver(broadcastReceiver);

    // TODO: Kill bluetooth connections
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Start, or stop, a LE scan. This will print devices as they are found to the ADB log cat.
   *
   * @param scan True if starting the scan, or false if stopping
   */
  private void scanDevices(boolean scan) {
    // Start scanning
    if (scan) {
      Log.d(TAG, "Starting LE scan...");
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Stopping LE scan.");
          // MVD is not API21, use deprecated method
          bluetoothAdapter.stopLeScan(leScanCallback);
        }
      }, scanTimeout);

      // MVD is not API21, use deprecated method
      bluetoothAdapter.startLeScan(leScanCallback);
    }

    // Otherwise stop scanning
    else {
      Log.d(TAG, "Stopping LE scan.");
      // MVD is not API21, so we need to use this method.
      bluetoothAdapter.stopLeScan(leScanCallback);
    }
  }

  /**
   * Used by the LE scan.
   */
  private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
      String name = device.getName();
      String address = device.getAddress();
      ParcelUuid[] uuids = device.getUuids();

      StringBuilder sb = new StringBuilder();
      sb.append("Found new LE device ").append("(rssi: ").append(rssi).append(")").append("\n");
      sb.append(name).append(", ").append(address).append("\n");
      sb.append("Services (uuid's):").append("\n");
      if (uuids != null) {
        for (int i = 0; i < uuids.length; i++) {
          String uuid = uuids[i].getUuid().toString();
          sb.append(uuid);
        }
      } else {
        sb.append("...");
      }

      Log.d(TAG, sb.toString());
      Log.d(TAG, "");
    }
  };

  /**
   * Attempt to connect to a BluetoothDevice
   */
  private void connectToDevice() {
    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);

    device.connectGatt(this, false, bluetoothGattCallback);
  }

  /**
   * This is used when connecting to a GATT device.
   */
  private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      String intentAction;

      // If I connected
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        intentAction = ACTION_GATT_CONNECTED;
        connectionState = STATE_CONNECTED;
        broadcastUpdate(intentAction);
      }

      // If I disconnected
      else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        intentAction = ACTION_GATT_DISCONNECTED;
        connectionState = STATE_DISCONNECTED;
        broadcastUpdate(intentAction);
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(ACTION_GATT_DATA_AVAILABLE, characteristic);
      }
    }
  };

  /**
   * Send a broadcast in response to GATT events
   *
   * @param action The action that happened
   */
  private void broadcastUpdate(final String action) {
    final Intent intent = new Intent(action);
    sendBroadcast(intent);
  }

  /**
   * Send a broadcast in response to received GATT characteristics
   *
   * @param action         The action that happened
   * @param characteristic The values
   */
  private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
    // This is special handling for the Heart Rate Measurement profile. Data
    // parsing is carried out as per profile specifications.
    if (characteristic.getUuid().equals(BEAN_UUID)) {
      // TODO: Handle data...

      // We're sending an "UP" event to other services to listen for
      Intent intent = new Intent(MvdHelper.ACTION_UP);

      String target = "mvd_blah";
      String code = "asd";
      String pin = "345";
      String value = "234567";

      intent.putExtra(MvdHelper.EXTRA_SENDER, TAG);
      intent.putExtra(MvdHelper.EXTRA_TARGET, target);
      intent.putExtra(MvdHelper.EXTRA_CODE, code);
      intent.putExtra(MvdHelper.EXTRA_PIN, pin);
      intent.putExtra(MvdHelper.EXTRA_VALUE, value);

      sendBroadcast(intent);
    } else {
      // Otherwise, it's not a Bean so ignore it...
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

      // If we're getting values from another service
      if (action.equals(MvdHelper.ACTION_UP) && !sender.equals(TAG)) {
        // TODO
      }

      // Or if we've getting values from the "cloud" services
      else if (action.equals(MvdHelper.ACTION_DOWN) && !sender.equals(TAG)) {
        // TODO
      }

      // If someone told me to kill myself...
      if (action.equals(MvdHelper.ACTION_KILL) && target.equals(TAG)) {
        // TODO: Kill all bluetooth connections

        unregisterReceiver(broadcastReceiver);

        stopSelf();
      }

      // I was asked to start scanning for LE units
      if (action.equals(MvdHelper.ACTION_SCAN)) {
        scanDevices(true);
      }

    }
  };

}
