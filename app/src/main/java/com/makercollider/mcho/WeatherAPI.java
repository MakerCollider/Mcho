package com.makercollider.mcho;

/**
 * Created by flyma on 2016/12/16.
 */

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

public class WeatherAPI implements Runnable {
    String location = "";
    public static String weather = "";
    public static String temperature = "";
    public static String wind_dir="";
    public static String wind_power="";

    final private String request_url_ = "https://free-api.heweather.com/v5/now?key=e90a181987b749f99ef5cb4a6ea23a43&lang=zh&city=";

    @Override
    public void run() {
        // String response = sendGet("https://free-api.heweather.com/v5/now");
        String response = sendGet(request_url_ + location);
        Log.d("Mcho", response);
    }

    public static String sendGet(String url) {
        String responseBody = null;
        try {
            URL url_ = new URL(url);
            HttpURLConnection url_connection = (HttpURLConnection)url_.openConnection();
            url_connection.setRequestMethod("GET");
            url_connection.connect();

            int response_code = url_connection.getResponseCode();
            if (response_code == HttpURLConnection.HTTP_OK) {

                BufferedReader buffered_reader = new BufferedReader(
                        new InputStreamReader(url_connection.getInputStream(), "UTF-8"));
                String read_line = null;
                StringBuffer response = new StringBuffer();
                while ((read_line = buffered_reader.readLine()) != null) {
                    response.append(read_line);
                }

                buffered_reader.close();
                responseBody = response.toString();

                JSONObject json_response = new JSONObject(responseBody);
                JSONObject weather_now = json_response.getJSONArray("HeWeather5").getJSONObject(0).getJSONObject("now");
                weather = weather_now.getJSONObject("cond").getString("txt");
                weather = weather.replace("，", " and ");
                temperature = weather_now.getString("tmp");
                wind_dir = weather_now.getJSONObject("wind").getString("dir");
                wind_power = weather_now.getJSONObject("wind").getString("sc");
                return weather;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return responseBody;
    }
}
