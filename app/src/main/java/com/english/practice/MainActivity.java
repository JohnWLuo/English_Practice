package com.english.practice;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.twilio.video.CameraCapturer;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.VideoTextureView;


import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.hardware.*;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    private String mAccessToken;
    private static final String TAG = MainActivity.class.getName();
    /*
     * Android application UI elements
     */
    private FrameLayout previewFrameLayout;
    private ViewGroup localContainer;
    private ViewGroup participantContainer;
    private FloatingActionButton callActionFab;
    private OkHttpClient client = new OkHttpClient();

    private Context mContext;
    private LocalAudioTrack localAudioTrack;
    private CameraCapturer cameraCapturerCompat;
    private LocalVideoTrack localVideoTrack;

    private VideoTextureView localVideoTextureView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super refers to the immediate parents onCreate method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        mContext = this.getApplicationContext();
        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        }

        initializeVideoTextureViews();
        createAudioAndVideoTracks();
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return (resultCamera == PackageManager.PERMISSION_GRANTED) && (resultMic == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "Camera and Microphone permissions needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void createAudioAndVideoTracks() {
        localAudioTrack = LocalAudioTrack.create(this, true);

        cameraCapturerCompat = new CameraCapturer(this, getAvailableCameraSource());

        localVideoTrack =  LocalVideoTrack.create(this, true, cameraCapturerCompat);

        localVideoTrack.addRenderer(localVideoTextureView);
    }


    private CameraCapturer.CameraSource getAvailableCameraSource() {
        return (CameraCapturer.isSourceAvailable(CameraCapturer.CameraSource.FRONT_CAMERA)) ?
                (CameraCapturer.CameraSource.FRONT_CAMERA) :
                (CameraCapturer.CameraSource.BACK_CAMERA);
    }

    private void initializeVideoTextureViews() {
        localVideoTextureView = findViewById(R.id.localContainer);
        localVideoTextureView.setMirror(true);



        VideoTextureView remoteParticipantVideoTextureView = findViewById(R.id.participantContainer);
        remoteParticipantVideoTextureView.setVisibility(GONE);
    }














    /*
    private void getCapabilityToken() {
        try {
            run("http://[your-ngrok-url].ngrok.io/token", new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String token = response.body().string();
                        JSONObject obj = new JSONObject(token);
                        mAccessToken = obj.getString("token");
                        Log.d(TAG, token);
                        initializeTwilioSdk(mAccessToken);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    /*private Call run(String url, Callback callback) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Call response = client.newCall(request);
        response.enqueue(callback);
        return response;
    }*/



}
