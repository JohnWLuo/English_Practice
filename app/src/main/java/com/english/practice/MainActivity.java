package com.english.practice;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.OpusCodec;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoTextureView;
import com.twilio.video.Room;
import com.twilio.video.LocalParticipant;


import com.twilio.video.AudioCodec;
import com.twilio.video.VideoCodec;

import com.koushikdutta.ion.Ion;
import com.twilio.video.VideoTrack;
import com.twilio.video.Vp8Codec;
import com.twilio.video.EncodingParameters;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Collections;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    private String accessToken;
    private Room room;

    /*
     * Android application UI elements
     */

    private AudioManager audioManager;
    private Context mContext;
    private LocalAudioTrack localAudioTrack;
    private CameraCapturer cameraCapturerCompat;
    private LocalVideoTrack localVideoTrack;
    private LocalParticipant localParticipant;
    private TextView videoStatusTextView;
    private String remoteParticipantIdentity;
    private boolean disconnectedFromOnDestroy;

    private AudioCodec audioCodec;
    private VideoCodec videoCodec;
    private EncodingParameters encodingParameters;
    private int previousAudioMode;
    private static final String TAG = "English Practice";


    private VideoTextureView localVideoTextureView;
    private boolean isSpeakerPhoneEnabled = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super refers to the immediate parents onCreate method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        videoStatusTextView = findViewById(R.id.videoStatusText);

        mContext = this.getApplicationContext();
        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        }

        initializeVideoTextureViews();
        createAudioAndVideoTracks();

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        audioCodec = getAudioCodec();
        videoCodec = getVideoCodec();

        retrieveAccessTokenfromServer();

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


    private void retrieveAccessTokenfromServer() {
        Ion.with(this)
                .load(String.format("https://byzantine-hippopotamus-3086.twil.io/AccessToken?identity=%s", "John"))
                .asString().setCallback((e, token) -> {
                    if (e == null) {
                        this.accessToken = token;
                        /*Toast.makeText(this,
                               "This is access token " + this.accessToken, Toast.LENGTH_LONG)
                                .show();*/
                        connectToRoom("TestRoom");
                    } else {
                        Toast.makeText(this,
                               "Error Retrieving Access Token", Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    private void connectToRoom(String roomName) {
        //configureAudio(true);
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);

        /*
         * Add local audio track to connect options to share with participants.
         */
        if (localAudioTrack != null) {
            connectOptionsBuilder
                    .audioTracks(Collections.singletonList(localAudioTrack));
        }

        /*
         * Add local video track to connect options to share with participants.
         */
        if (localVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(localVideoTrack));
        }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(Collections.singletonList(audioCodec));
        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));

        /*
         * Set the sender side encoding parameters.
         */
        connectOptionsBuilder.encodingParameters(encodingParameters);

        room = Video.connect(this, connectOptionsBuilder.build(), roomListener());
        //setDisconnectAction();
    }

    public AudioCodec getAudioCodec() {
        return new OpusCodec();
    }

    public VideoCodec getVideoCodec() {
        return new Vp8Codec();
    }

    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                localParticipant = room.getLocalParticipant();
                videoStatusTextView.setText(String.format("Connected to %s", room.getName()));
                setTitle(room.getName());
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
                videoStatusTextView.setText(String.format("Reconnecting to %s", room.getName()));
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                videoStatusTextView.setText(String.format("Connected to %s", room.getName()));
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                videoStatusTextView.setText("Failed to connect");
//                configureAudio(false);
//                intializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                localParticipant = null;
                videoStatusTextView.setText(String.format("Disconnected from %s", room.getName()));
                room = null;
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!disconnectedFromOnDestroy) {
                    //configureAudio(false);
                    initializeVideoTextureViews();
                    //intializeUI();
                }
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {

            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
            }

            @Override
            public void onRecordingStarted(Room room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

}
