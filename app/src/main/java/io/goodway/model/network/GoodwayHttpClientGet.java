package io.goodway.model.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import io.goodway.model.Uber;
import io.goodway.model.UberProduct;
import io.goodway.model.callback.FinishCallback;
import io.goodway.navitia_android.Action;
import io.goodway.navitia_android.ErrorAction;

/**
 * Created by antoine on 10/25/15.
 */


public class GoodwayHttpClientGet<T> extends AsyncTask<AbstractMap.SimpleEntry<String, String>, T, Integer>{

    private Context c;
    private Action<T> action;
    private ErrorAction error;
    private FinishCallback finish;
    private ProcessJson<T> processJson;
    private String url;

    private GoodwayHttpClientGet(Context c, ProcessJson<T> processJson, Action<T> action, ErrorAction error, final String URL){
        this.c = c;
        this.action = action;
        this.error = error;
        this.processJson = processJson;
        this.url = URL;
    }

    public static AsyncTask getUberEstimate(Context c, Action<List<Uber>> action, ErrorAction error, double start_latitude, double start_longitude, double end_latitude, double end_longitude) {
        return new GoodwayHttpClientGet<>(c, new ProcessJson<List<Uber>>() {
            @Override
            public List<Uber> processJson(JSONObject jsonObject) throws JSONException {
                ArrayList<Uber> ret = new ArrayList<Uber>();
                JSONArray jsonArray = jsonObject.getJSONArray("prices");
                int length = jsonArray.length();
                for (int i = 0; i < length; i++) {
                    JSONObject innerJsonObject = jsonArray.getJSONObject(i);
                    String localized_display_name = innerJsonObject.optString("localized_display_name");
                    int high_estimate = innerJsonObject.optInt("high_estimate");
                    int minimum = innerJsonObject.optInt("minimum");
                    int duration = innerJsonObject.optInt("duration");
                    String estimate = innerJsonObject.optString("estimate");
                    double distance = innerJsonObject.optInt("distance");
                    String display_name = innerJsonObject.optString("display_name");
                    String product_id = innerJsonObject.optString("product_id");
                    int low_estimate = innerJsonObject.optInt("low_estimate");
                    int surge_multiplier = innerJsonObject.optInt("surge_multiplier");
                    String currency_code = innerJsonObject.optString("currency_code");
                    ret.add(new Uber(localized_display_name, high_estimate, minimum, duration, estimate,
                            distance, display_name, product_id, low_estimate, surge_multiplier, currency_code));
                }
                return ret;
            }
        }, action, error, "http://developer.goodway.io/api/v1/uber/estimate?").execute(
                new AbstractMap.SimpleEntry<String, String>("start_latitude", Double.toString(start_latitude)),
                new AbstractMap.SimpleEntry<String, String>("start_longitude", Double.toString(start_longitude)),
                new AbstractMap.SimpleEntry<String, String>("end_latitude", Double.toString(end_latitude)),
                new AbstractMap.SimpleEntry<String, String>("end_longitude", Double.toString(end_longitude)));
    }
    public static AsyncTask getUberTimeEstimate(Context c, Action<Integer> action, ErrorAction error, double start_latitude, double start_longitude, String productId) {
        return new GoodwayHttpClientGet<>(c, new ProcessJson<Integer>() {
            @Override
            public Integer processJson(JSONObject jsonObject) throws JSONException {
                JSONArray jsonArray = jsonObject.getJSONArray("times");
                int length = jsonArray.length();
                if(length==1) {
                    try {
                        return jsonArray.getJSONObject(0).optInt("estimate");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return -1;
            }
        }, action, error, "http://developer.goodway.io/api/v1/uber/estimate_time?").execute(
                new AbstractMap.SimpleEntry<String, String>("start_latitude", Double.toString(start_latitude)),
                new AbstractMap.SimpleEntry<String, String>("start_longitude", Double.toString(start_longitude)),
                new AbstractMap.SimpleEntry<String, String>("product_id", productId));
    }
    public static AsyncTask uberRequestEstimate(Context c, Action<UberProduct> action, ErrorAction error, double start_latitude, double start_longitude, double end_latitude, double end_longitude, final Uber uber) {
        return new GoodwayHttpClientGet<>(c, new ProcessJson<UberProduct>() {
            @Override
            public UberProduct processJson(JSONObject jsonObject) throws JSONException {
                String surge_confirmation_href=null, surge_confirmation_id=null, display=null, currency_code=null, distance_unit=null;
                int high_estimate=-1, minimum=-1, low_estimate=-1, duration_estimate=-1, pickup_estimate;
                double surge_multiplier=-1, distance_estimate=-1;
                JSONArray prices = jsonObject.getJSONArray("prices");
                int pricesLength = prices.length();
                for (int i = 0; i < pricesLength; i++) {
                    JSONObject innerJsonObject = prices.getJSONObject(i);
                    surge_confirmation_href = innerJsonObject.optString("surge_confirmation_href");
                    high_estimate = innerJsonObject.optInt("high_estimate");
                    surge_confirmation_id = innerJsonObject.optString("surge_confirmation_id");
                    minimum = innerJsonObject.optInt("minimum");
                    low_estimate = innerJsonObject.optInt("low_estimate");
                    surge_multiplier = innerJsonObject.optDouble("surge_multiplier");
                    display = innerJsonObject.optString("display");
                    currency_code = innerJsonObject.optString("currency_code");
                }
                JSONArray trip = jsonObject.getJSONArray("trip");
                int tripLength = trip.length();
                for (int i = 0; i < tripLength; i++) {
                    JSONObject innerJsonObject = trip.getJSONObject(i);
                    distance_unit = innerJsonObject.optString("distance_unit");
                    duration_estimate = innerJsonObject.optInt("duration_estimate");
                    distance_estimate = innerJsonObject.optDouble("distance_estimate");
                }
                pickup_estimate = jsonObject.optInt("pickup_estimate");
                return new UberProduct(uber, surge_confirmation_href, surge_confirmation_id, display, currency_code, distance_unit, low_estimate, high_estimate, minimum, duration_estimate,
                        surge_multiplier, distance_estimate, pickup_estimate);
            }
        }, action, error, "http://developer.goodway.io/api/v1/uber/request_estimate?").execute(
                new AbstractMap.SimpleEntry<String, String>("start_latitude", Double.toString(start_latitude)),
                new AbstractMap.SimpleEntry<String, String>("start_longitude", Double.toString(start_longitude)),
                new AbstractMap.SimpleEntry<String, String>("product_id", uber.getProduct_id()),
                new AbstractMap.SimpleEntry<String, String>("end_latitude", Double.toString(end_latitude)),
                new AbstractMap.SimpleEntry<String, String>("end_longitude", Double.toString(end_longitude)));
    }
    public static AsyncTask uberRequest(Context c, Action<String> action, ErrorAction error, double start_latitude, double start_longitude, double end_latitude, double end_longitude, final Uber uber) {
        return new GoodwayHttpClientGet<>(c, new ProcessJson<String>() {
            @Override
            public String processJson(JSONObject jsonObject) throws JSONException {
                return jsonObject.optString("request_id");
            }
        }, action, error, "http://developer.goodway.io/api/v1/uber/request?").execute(
                new AbstractMap.SimpleEntry<String, String>("start_latitude", Double.toString(start_latitude)),
                new AbstractMap.SimpleEntry<String, String>("start_longitude", Double.toString(start_longitude)),
                new AbstractMap.SimpleEntry<String, String>("product_id", uber.getProduct_id()),
                new AbstractMap.SimpleEntry<String, String>("end_latitude", Double.toString(end_latitude)),
                new AbstractMap.SimpleEntry<String, String>("end_longitude", Double.toString(end_longitude)));
    }

    public static AsyncTask checkMailAvailability(Context c, Action<Boolean> action, ErrorAction error, String mail){
        return new GoodwayHttpClientGet<>(c, new ProcessJson<Boolean>() {
            @Override
            public Boolean processJson(JSONObject jsonObject) throws JSONException {
                if(jsonObject.getBoolean("success")) {
                    JSONArray users = jsonObject.getJSONArray("users");
                    if (users.length() == 0) {
                        return true;
                    }
                }
                return false;
            }
        }, action, error, "http://developer.goodway.io/api/v1/authentication/mail?").execute(new AbstractMap.SimpleEntry<String, String>("mail", mail));
    }

    @Override
    protected Integer doInBackground(AbstractMap.SimpleEntry<String, String>... entries) {
        int length=0;
        try {
            HttpURLConnection urlConnection = GoodwayProtocol.getHttpGetUrlConnection(this.url, entries);
            Log.d("url=", "url= "+urlConnection.toString());
            int serverResponseCode = urlConnection.getResponseCode();
            String serverResponseMessage = urlConnection.getResponseMessage();

            Log.d(serverResponseCode+"", "Response code");
            Log.d(serverResponseMessage, "Response message");
            String jsonResult;
            if (serverResponseCode == 201 || serverResponseCode == 200) {
                Log.d(urlConnection.getResponseCode() + "", "response code");
                InputStream response = urlConnection.getInputStream();
                jsonResult = GoodwayProtocol.convertStreamToString(response);
                Log.d("response:", jsonResult.toString());
                try {
                    JSONObject obj= new JSONObject(jsonResult.toString());
                    publishProgress(processJson.processJson(obj));
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Log.d("error", "json exception");
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            length=-1;
            e.printStackTrace();
        }
        return length;
    }
    @Override
    protected void onProgressUpdate(T...progress){
        action.action(progress[0]);
    }

    protected void onPostExecute(Integer length){
        if(length<1 && error!=null){
            error.action(length);
        }
        else if(finish!=null){
            finish.action(length);
        }
    }


}
