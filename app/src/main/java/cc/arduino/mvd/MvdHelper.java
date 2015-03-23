package cc.arduino.mvd;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import cc.arduino.mvd.services.ElisService;
import cc.arduino.mvd.services.FirebaseService;
import cc.arduino.mvd.services.HttpService;
import cc.arduino.mvd.services.XivelyService;

/**
 * Created by ksango on 19/03/15.
 */
public class MvdHelper {

  public static final String ACTION_UP = "cc.arduino.mvd.helper.actions.UP";
  public static final String ACTION_DOWN = "cc.arduino.mvd.helper.actions.DOWN";
  public static final String ACTION_KILL = "cc.arduino.mvd.helper.actions.KILL";
  public static final String ACTION_SCAN = "cc.arduino.mvd.helper.actions.SCAN";

  public static final String EXTRA_TARGET = "cc.arduino.mvd.helper.extras.TARGET";
  public static final String EXTRA_SENDER = "cc.arduino.mvd.helper.extras.SENDER";

  public static final String EXTRA_CODE = "cc.arduino.mvd.helper.extras.CODE";
  public static final String EXTRA_PIN = "cc.arduino.mvd.helper.extras.PIN";
  public static final String EXTRA_VALUE = "cc.arduino.mvd.helper.extras.VALUE";

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

  public static boolean isServiceRunning(Context ctx, Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  public Class getServiceClass(String name) {
    if (name.equals(FirebaseService.class.getSimpleName())) {
      return FirebaseService.class;
    } else if (name.equals(HttpService.class.getSimpleName())) {
      return HttpService.class;
    } else if (name.equals(XivelyService.class.getSimpleName())) {
      return XivelyService.class;
    } else if (name.equals(ElisService.class.getSimpleName())) {
      return ElisService.class;
    }

    return null;
  }

}
