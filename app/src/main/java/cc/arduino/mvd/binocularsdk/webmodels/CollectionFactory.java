package cc.arduino.mvd.binocularsdk.webmodels;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import cc.arduino.mvd.binocularsdk.web.IJsonCollectionConvertable;

/**
 * Created by felix on 2015-04-11.
 */
public class CollectionFactory {


    private static final String TAG = CollectionFactory.class.getSimpleName();

    public static IJsonCollectionConvertable build_state(final String id, final String value) {

        IJsonCollectionConvertable state = null;

        // LED
        if(id.equals("5528e947d7ad015decb3af13")) {
            state = new FlagCollection(new ICollectionState() {


                @Override
                public String getCode() {
                    return "R";
                }

                @Override
                public String getUrl() {
                        return "http://api.binocular.se/v1/devices/" + id + "/flags?api_key=55285bd1018d767753bf0045";
                }

                @Override
                public Map<String, String> getDataMapping() {
                    return new HashMap<String, String>();
                }

                @Override
                public String getAction() {
                    return "GET";
                }
            });
        } else if(id.equals("552867f3e80dab6754b41131")) {
            state = new FlagCollection(new ICollectionState() {


                @Override
                public String getCode() {
                    return "T";
                }


                @Override
                public String getUrl() {
                    return "http://api.binocular.se/v1/devices/" + id + "/flags?api_key=55285bd1018d767753bf0045";
                }

                @Override
                public Map<String, String> getDataMapping() {

                    Map<String, String> map = new HashMap<>();
                    map.put("is_full", value);

                    return map;
                }

                @Override
                public String getAction() {
                    return "POST";
                }
            });
        } else if(id.equals("552929cf306eeede84662bc8")) {
            state = new FlagCollection(new ICollectionState() {


                @Override
                public String getCode() {
                    return "T";
                }

                @Override
                public String getUrl() {
                    return "http://api.binocular.se/v1/devices/" + id + "/data?api_key=55285bd1018d767753bf0045";
                }

                @Override
                public Map<String, String> getDataMapping() {


                    if(value != null) {
                        Log.d(TAG, "The value is : " + value);

                        Map<String, String> map = new HashMap<>();

                        String[] values = value.split(";");

                        map.put("temperature", values[0]);
                        map.put("humidity", values[1]);

                        return map;
                    } else {
                        return null;
                    }

                }

                @Override
                public String getAction() {
                    return "POST";
                }
            });
        } else {
            Log.d(TAG, "No valid binocular id was found");
        }

        return state;
    }



}
