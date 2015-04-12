package cc.arduino.mvd.binocularsdk.webmodels;

import cc.arduino.mvd.binocularsdk.web.IJsonCollectionConvertable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by abc123 on 2015-04-06.
 */
public class DeviceCollection implements IJsonCollectionConvertable {

    private List<Device> devices;
    private ICollectionState stateMachine;

    public DeviceCollection(ICollectionState stateMachine){
        devices = new ArrayList<>();
        this.stateMachine = stateMachine;
    }

    @Override
    public void createCollection(JSONArray jsonArr) {

        for(int i = 0; i < jsonArr.length(); i++){
            Device device = new Device();

            try {
                device.setVariables(jsonArr.getJSONObject(i));
            } catch (JSONException e) {

                device = null;
                e.printStackTrace();
            }

            if(device != null){
                devices.add(device);
            }
        }
    }

    @Override
    public String getUrl() {
        return stateMachine.getUrl();
    }

    @Override
    public Map<String, String> getDefaultMap() { //Här kan man adda alla JSON key/vals
        return stateMachine.getDataMapping();
    }

    @Override
    public String getAction() { //Om det är en POST/GET
        return stateMachine.getAction();
    }

    @Override
    public void parseResponse(String json) {

        try{

            JSONObject object = new JSONObject(json);

            if(object.has("status_code")) {
                if(!object.getString("status_code").equals("200")) {
                    return;
                }
            }



            if(object.has("items")) {
                JSONArray array = object.getJSONArray("items");

                for (int i = 0; i < array.length(); i++) {
                    Device device = new Device();
                    device.setVariables(array.getJSONObject(i));
                    devices.add(device);
                }
            }

        }catch(JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getCode() {
        return stateMachine.getCode();
    }


    public List<Device> getDevices(){
        return devices;
    }
}
