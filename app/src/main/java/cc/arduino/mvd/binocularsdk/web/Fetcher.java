package cc.arduino.mvd.binocularsdk.web;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by abc123 on 2015-04-06.
 */
public class Fetcher {
        HttpResponse response;
        IJsonCollectionConvertable collectionManager;
        private Map<String, String> map;
        OnDone done;

        private static final String TAG = Fetcher.class.getSimpleName();

        private HttpPost getPostHeader (String uri, JSONObject obj){
            HttpPost post = new HttpPost(uri);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json;charset=UTF-8");
            post.setHeader("Authorization", "Basic NTUyODViZDEwMThkNzY3NzUzYmYwMDQ1OmFzZGZhc2Rm");

            try {
                if(obj != null){
                    post.setEntity(new StringEntity(obj.toString(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            return post;
        }

        private HttpGet getGetHeader (String uri){
            HttpGet get = new HttpGet(uri);
            get.setHeader("Accept", "application/json");
            get.setHeader("Content-type", "application/json;charset=UTF-8");
            get.setHeader("Authorization", "Basic NTUyODViZDEwMThkNzY3NzUzYmYwMDQ1OmFzZGZhc2Rm");

            return get;
        }

        private void executeRequest(HttpUriRequest post){
            try {
                response = new DefaultHttpClient().execute(post);
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private BufferedReader getReader(){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalStateException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            return reader;
        }

        private String getJson(BufferedReader reader){

            String json = null;
            try {
                json = reader.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return json;
        }

        private HttpPost getPost(){
            String uri = collectionManager.getUrl();
            HttpPost post = null;
            if(map != null){
                JSONObject obj= new JSONObject(map);
                post = getPostHeader(uri, obj);
            }else{
                post = getPostHeader(uri, null);
            }
            return post;
        }

        private HttpGet getGet(){
            String uri = collectionManager.getUrl();
            HttpGet get = getGetHeader(uri);
            return get;
        }


        public void getAllWithPost(){

            executeRequest(getPost());

            BufferedReader reader = getReader();

            String json = getJson(reader);

            collectionManager.parseResponse(json);

            done.done();
        }

        public void getAllWithGet(){

            executeRequest(getGet());

            BufferedReader reader = getReader();

            String json = getJson(reader);

            collectionManager.parseResponse(json);

            done.done();
        }



        public void execute() {
            String action = collectionManager.getAction();

            if(action.equals("POST")){
                if(collectionManager.getDefaultMap() != null) {
                    getAllWithPost();
                } else {
                    Log.d(TAG, "Default map missing");
                }

            }else if(action.equals("GET")){
                getAllWithGet();
            }
        }


        public IJsonCollectionConvertable getCollectionManager() {
            return collectionManager;
        }


        public void setCollectionManager(IJsonCollectionConvertable collectionManager) {
            this.collectionManager = collectionManager;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        public OnDone getDone() {
            return done;
        }

        public void setDone(OnDone done) {
            this.done = done;
        }

}
