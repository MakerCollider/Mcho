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

public class MainActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents{
    Synthesizer syn_;
    MicrophoneRecognitionClient sst_client_ = null;

    final int PERM_RECORD_AUDIO = 10010;

    String nick_name_;
    int nick_name_length_;

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

        Voice v = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)", Voice.Gender.Female, true);
        // Voice v = new Voice("zh-CN", "Microsoft Server Speech Text to Speech Voice (zh-CN, HuihuiRUS)", Voice.Gender.Female, true);
        syn_.SetVoice(v, null);

        this.sst_client_ = SpeechRecognitionServiceFactory.createMicrophoneClient(
                this,
                SpeechRecognitionMode.ShortPhrase,
                this.getString(R.string.default_locale),
                this,
                this.getString(R.string.api_key));

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
        Log.d("Mcho", "Partial Response Receive");
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        String raw_response="", response_text="";

        this.sst_client_.endMicAndRecognition();
        if(response.RecognitionStatus == RecognitionStatus.RecognitionSuccess) {
            raw_response = response.Results[0].DisplayText;

            int start_index = raw_response.indexOf(this.nick_name_);

            if(start_index != -1) {
                response_text = raw_response.substring(start_index + nick_name_length_);
                syn_.SpeakToAudio(response_text);
            }

            Log.d("Mcho", "Final Response Receive");
            Log.d("Mcho", "Raw: " + raw_response);
            Log.d("Mcho", "Response: " + response_text);
        } else {
            Log.w("Mcho", "No Response" + response.RecognitionStatus.toString());
        }

        this.sst_client_.startMicAndRecognition();
    }

    public void onIntentReceived(String var1) {

    }

    public void onError(int var1, String var2) {

    }

    public void onAudioEvent(boolean var1) {
        Log.d("Mcho", "AudioEvent: " + var1);
    }
}
