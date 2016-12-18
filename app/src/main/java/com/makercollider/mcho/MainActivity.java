package com.makercollider.mcho;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
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

import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents{
    Synthesizer syn_;
    MicrophoneRecognitionClient sst_client_ = null;

    final int PERM_RECORD_AUDIO = 10010;

    String nick_name_;
    int nick_name_length_;

    boolean response_trigger_ = false;

    WeatherAPI weather_api = new WeatherAPI();

    private static TextView log_text_view_;

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
                addLog("我", question_text);
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

                String value;
                Calendar date = Calendar.getInstance();

                String output_text = "";

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

                        output_text = String.format("%s现在的天气是%s，温度%s摄氏度，%s级%s。",
                                weather_api.location, weather_api.weather, weather_api.temperature,
                                weather_api.wind_power, weather_api.wind_dir);
                        break;
                    case "builtin.intent.calendar.check_availability":
                        if(entities.length() > 0) {
                            for (int entity_index = 0; entity_index < entities.length(); entity_index++) {
                                switch (entities.getJSONObject(entity_index).getString("type")) {
                                    case "builtin.calendar.start_time":
                                        value = entities.getJSONObject(entity_index).getJSONObject("resolution").getString("time");
                                        date.set(Calendar.HOUR_OF_DAY, Integer.parseInt(value.substring(1)));
                                        break;
                                    case "builtin.calendar.start_date":
                                        value = entities.getJSONObject(entity_index).getJSONObject("resolution").getString("date");
                                        date.set(Calendar.YEAR, Integer.parseInt(value.substring(0, 4)));
                                        date.set(Calendar.MONTH, Integer.parseInt(value.substring(5, 7))-1);
                                        date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(value.substring(8, 10)));
                                        break;
                                    default:
                                }
                            }
                        }

                        output_text = String.format("%d月，%d日，%d点，%s。",
                                date.get(Calendar.MONTH)+1, date.get(Calendar.DAY_OF_MONTH),
                                date.get(Calendar.HOUR), "没有待办事情");
                        Log.d("Mcho", "CheckDate: " + date.toString());
                        break;
                    case "builtin.intent.reminder.create_single_reminder":
                        String reminder_text = "";

                        if(entities.length() > 0) {
                            for (int entity_index = 0; entity_index < entities.length(); entity_index++) {
                                switch (entities.getJSONObject(entity_index).getString("type")) {
                                    case "builtin.reminder.start_time":
                                        value = entities.getJSONObject(entity_index).getJSONObject("resolution").getString("time");
                                        date.set(Calendar.HOUR_OF_DAY, Integer.parseInt(value.substring(1)));
                                        break;
                                    case "builtin.reminder.start_date":
                                        value = entities.getJSONObject(entity_index).getJSONObject("resolution").getString("date");
                                        date.set(Calendar.YEAR, Integer.parseInt(value.substring(0, 4)));
                                        date.set(Calendar.MONTH, Integer.parseInt(value.substring(5, 7))-1);
                                        date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(value.substring(8, 10)));
                                        break;
                                    case "builtin.reminder.reminder_text":
                                        reminder_text = entities.getJSONObject(entity_index).getString("entity");
                                        break;
                                    default:
                                }
                            }
                        }

                        output_text = String.format("好的，已为您建立事件。%d月，%d日，%d点，%s。",
                                date.get(Calendar.MONTH)+1, date.get(Calendar.DAY_OF_MONTH),
                                date.get(Calendar.HOUR), reminder_text);

                        Log.d("Mcho", "Set Reminder: " + date.toString());
                        break;
                    default:
                        output_text = "我不太理解你的意思。";
                        Log.d("Mcho", "Unknow Intent Type");
                }

                syn_.SpeakToAudio(output_text);
                addLog(this.nick_name_, output_text);
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

    public void addLog(String log_from, String log_string) {
        log_text_view_ = new TextView(this.getApplicationContext());

        log_text_view_.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        log_text_view_.setTextColor(Color.BLACK);
        // log_text_view_.setBackgroundColor(Color.LTGRAY);
        log_text_view_.setAlpha((float) 0.4);

        log_text_view_.setText(log_from + ": " + log_string);

        LinearLayout log_layout = (LinearLayout)this.findViewById(R.id.log_linearlayout);
        log_layout.addView(log_text_view_);
    }
}
