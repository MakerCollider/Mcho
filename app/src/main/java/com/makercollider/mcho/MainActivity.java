package com.makercollider.mcho;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.speech.tts.*;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents{
    Synthesizer syn_;
    MicrophoneRecognitionClient sst_client_ = null;

    final int PERM_RECORD_AUDIO = 10010;

    String nick_name_;
    int nick_name_length_;

    boolean response_trigger_ = false;

    WeatherAPI weather_api = new WeatherAPI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.nick_name_ = getString(R.string.nick_name);
        this.nick_name_length_ = nick_name_.length();

        if (syn_ == null) {
            // Create Text To Speech Synthesizer.
            syn_ = new Synthesizer(getString(R.string.api_key));
        }

         int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
         if(permissionCheck == PackageManager.PERMISSION_DENIED) {
             Log.d("Mcho", "Record audio permission denied");
             if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

             } else {
                 ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                         PERM_RECORD_AUDIO);
             }
         }

        syn_.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);

        // Voice v = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)", Voice.Gender.Female, true);
        Voice v = new Voice("zh-CN", "Microsoft Server Speech Text to Speech Voice (zh-CN, HuihuiRUS)", Voice.Gender.Female, true);
        syn_.SetVoice(v, null);

        this.sst_client_ = SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                this,
                this.getString(R.string.default_locale),
                this,
                this.getString(R.string.api_key),
                this.getString(R.string.luis_app_id),
                this.getString(R.string.luis_subscription_id));

        this.sst_client_.startMicAndRecognition();
    }

    @Override
    public void onRequestPermissionsResult(int requresCode, String permission[], int[] grantReuslts) {
        switch (requresCode) {
            case PERM_RECORD_AUDIO: {
                if(grantReuslts.length > 0 && grantReuslts[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Mcho", "Audio record permission granted");
                }
            }
        }
    }

    public void onPartialResponseReceived(String var1) {
        Log.d("Mcho", "SST Partial Response Receive");
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        String raw_response="", question_text="";

        this.sst_client_.endMicAndRecognition();
        if(response.RecognitionStatus == RecognitionStatus.RecognitionSuccess) {
            raw_response = response.Results[0].DisplayText;

            int start_index = raw_response.indexOf(this.nick_name_);

            if(start_index != -1) {
                question_text = raw_response.substring(start_index + nick_name_length_);
                response_trigger_ = true;
            }

            Log.d("Mcho", "SST Receive");
            Log.d("Mcho", "Raw: " + raw_response);
            Log.d("Mcho", "Question: " + question_text);
        } else {
            Log.w("Mcho", "SST Error:" + response.RecognitionStatus.toString());
        }

        this.sst_client_.startMicAndRecognition();
    }

    public void onIntentReceived(String var1) {
        Log.d("Mcho", "Intent Receive");
        if(response_trigger_ == true) {
            try {
                JSONObject json_response = new JSONObject(var1);

                String type = json_response.getJSONArray("intents").getJSONObject(0).getString("intent");
                JSONArray entities = json_response.getJSONArray("entities");

                Log.d("Mcho", "Intent Type: " + type);
                switch (type) {
                    case "builtin.intent.weather.check_weather":
                        if(entities.length() > 0) {
                            String entity_type = entities.getJSONObject(0).getString("type");
                            String location = entities.getJSONObject(0).getString("entity");
                            location = location.substring(location.indexOf(" ") + 1);
                            weather_api.location = location;

                            Log.d("Mcho", "Location: " + location);
                            Log.d("Mcho", "Entity type: " + entity_type);
                        }

                        Thread thread;
                        thread = new Thread(weather_api, "wt1");
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        syn_.SpeakToAudio(weather_api.location + "今天的天气是" + weather_api.weather +
                                "，温度"+ weather_api.temperature + "摄氏度" +
                                "，" + weather_api.wind_power + "级" + weather_api.wind_dir);
                        break;
                    case "GetTime":
                        break;
                    default:
                        syn_.SpeakToAudio("我不太理解你的意思");
                        Log.d("Mcho", "Unknow Intent Type");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            response_trigger_ = false;
        }
    }

    public void onError(int var1, String var2) {

    }

    public void onAudioEvent(boolean var1) {
        Log.d("Mcho", "AudioEvent: " + var1);
    }
}
