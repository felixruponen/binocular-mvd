package cc.arduino.mvd.binocularsdk.webmodels;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.arduino.mvd.binocularsdk.web.IJsonCollectionConvertable;

/**
 * Created by felix on 2015-04-11.
 */
public class FlagCollection implements IJsonCollectionConvertable {

    private final String TAG = FlagCollection.class.getSimpleName();

    private final ICollectionState state;
    JSONObject flags;



    public FlagCollection(ICollectionState state) {
        this.state = state;
    }

    @Override
    public void createCollection(JSONArray jsonArr) {
        // This is not a collection
    }

    @Override
    public void parseResponse(String json) {

        Log.d(TAG, json);

        try {
            flags = new JSONObject(json);


            if(flags.has("status_code")) {
                if(!flags.getString("status_code").equals("200")) {
                    flags = null;
                    return;
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getUrl() {
        return state.getUrl();
    }

    @Override
    public Map<String, String> getDefaultMap() { //Här kan man adda alla JSON key/vals
        return state.getDataMapping();
    }

    @Override
    public String getAction() { //Om det är en POST/GET
        return state.getAction();
    }

    @Override
    public String getCode() {
        return state.getCode();
    }

    public JSONObject getFlags() {
        return flags;
    }

}
