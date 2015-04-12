package cc.arduino.mvd.binocularsdk.webmodels;

import cc.arduino.mvd.binocularsdk.web.IJsonConvertable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abc123 on 2015-04-06.
 */
public class Device implements IJsonConvertable{
    String _id;
    String id;
    String last_heartbeat;
    String flags;
    String devicetype;

    @Override
    public void setVariables(JSONObject obj) {
        try {
            _id = obj.getString("_id");
            id = obj.getString("id");
            last_heartbeat = obj.getString("last_heartbeat");
            flags = obj.getString("flags");
            devicetype = obj.getString("devicetype");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
