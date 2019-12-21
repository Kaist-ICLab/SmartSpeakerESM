package kr.ac.kaist.kse.ic.cha.smartspeaker;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import java.time.format.TextStyle;

import kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ESMService;
import kr.ac.kaist.kse.ic.cha.smartspeaker.esm.SoundService;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ESMService.ACTION_FORCE_TO_PLAY_ESM;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_FRONT; // Camera.CameraInfo.CAMERA_FACING_FRONT

    private SurfaceView surfaceView;
    private CameraPreview mCameraPreview;
    private View mLayout;
    public static TextView recording;
    public static TextView expTime;

    MediaPlayer mediaPlayer;
    //You can change photo-level threshold minimum and maximum values of a slider at main page here
    final int max = 24000;
    final int min = 16000;
    final int step = 500;
    //You can change photo-level threshold here
    public static int thresholdForDetection = 20000;
    public static boolean isSetting;
    public static boolean TakingPicture;
    //You can change operation time (from stratTime to endTime)
    public static int startTime = 10;
    public static int endTime = 22;
    public static boolean isSaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recording = (TextView)findViewById(R.id.recording);
        expTime = (TextView)findViewById(R.id.startTimeShow);

        //start button
        findViewById(R.id.buttonStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
                Log.i("test","시작 버튼 누름");
                //You can change text here
                recording.setText("Collecting data...");
            }
        });


        //stop button
        findViewById(R.id.buttonStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                //You can change text here
                recording.setText("Finished");
            }
        });



        //set button : Check volumes for ESM questions
        findViewById(R.id.buttonSound).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //You can change text here
                recording.setText("Checking sound");
                if(mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.esmvoicefinal);
                mediaPlayer.start();


            }
        });

        //pic button : Take a picture of a room before starting data collection as a basement
        findViewById(R.id.buttonpicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamera();
                TakingPicture = true;

            }
        });

        //move button : To activate CameraPreview function. Without this button, activating CameraPreview function along with initiating app,
        //app became too slow to do setting
        //After finishing other settings (sound check, taking a room picture, setting oepration time), then activate CameraPreview function and take a room picture
        findViewById(R.id.buttonDetect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSetting = true;

            }
        });

        //done button : and then, finish the setting by clicking this button.
        findViewById(R.id.buttonFinish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View a1 = findViewById(R.id.buttonStart);
                View a2 = findViewById(R.id.buttonStop);
                View a = findViewById(R.id.buttonSound);
                View b = findViewById(R.id.buttonFinish);
                View c = findViewById(R.id.threshold);
                View d = findViewById(R.id.slider);
                View e = findViewById(R.id.buttonpicture);
                View f = findViewById(R.id.timeSetting);
                View g = findViewById(R.id.buttonDetect);
                a.setVisibility(View.GONE);
                b.setVisibility(View.GONE);
                c.setVisibility(View.GONE);
                d.setVisibility(View.GONE);
                e.setVisibility(View.GONE);
                f.setVisibility(View.GONE);
                g.setVisibility(View.GONE);
                a1.setVisibility(View.VISIBLE);
                a2.setVisibility(View.VISIBLE);
                //You can change text here
                recording.setText("Setting is done");
                isSetting = false;
                if(mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

            }
        });

        //time button : set customized operation time

        findViewById(R.id.buttonTime).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText startTimeIn = (EditText)findViewById(R.id.startTime);
                EditText endTimeIn = (EditText)findViewById(R.id.endTime);
                try {
                    String str1 = startTimeIn.getText().toString().trim();
                    String str2 = endTimeIn.getText().toString().trim();
                    startTime = Integer.parseInt(str1);
                    endTime = Integer.parseInt(str2);
                    //You can change text here
                    expTime.setText("From "+startTime+" to " + endTime);
                } catch(NumberFormatException e){
                    Log.i("test", "Error starting camera preview: " + e.getMessage());
                }
            }
        });



        final TextView threshold = (TextView) findViewById(R.id.threshold);
        final AppCompatSeekBar slider = (AppCompatSeekBar) findViewById(R.id.slider);
        setSeekBarMax(slider, max);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                setSeekBarchange(progress, threshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        setSeekBarAnimation(slider);


        if(!hasPermissions())
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);


        // Makes the status bar invisible
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Keep the screen on
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        mLayout = findViewById(R.id.layout_main);
        surfaceView = findViewById(R.id.camera_preview_main);


        // Invisible on the screen until the runtime permission is complete.
        surfaceView.setVisibility(View.GONE);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {

            int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);


            if ( cameraPermission == PackageManager.PERMISSION_GRANTED
                    && writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED) {
//                startCamera();


            }else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "This app requires camera and external storage access.",
                            Snackbar.LENGTH_INDEFINITE).setAction("YES", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            ActivityCompat.requestPermissions( MainActivity.this, REQUIRED_PERMISSIONS,
                                    PERMISSIONS_REQUEST_CODE);
                        }
                    }).show();


                } else {
                    // If the user has never denied permission, the permission request is made immediately
                    // The result of the request is received from onRequestPermissionResult
                    ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                            PERMISSIONS_REQUEST_CODE);
                }

            }

        } else {

            final Snackbar snackbar = Snackbar.make(mLayout, "Your device does not support cameras.",
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("YES", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        }



    }

    void startCamera(){

        // Create the Preview view and set it as the content of this Activity.
        mCameraPreview = new CameraPreview(this, this, CAMERA_FACING, surfaceView);

        //surfaceView.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(mCameraPreview!=null) {
            mCameraPreview.surfaceDestroyed(surfaceView.getHolder());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCameraPreview!=null) {
            mCameraPreview.surfaceDestroyed(surfaceView.getHolder());
        }
    }

    private boolean hasPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }


    private static final long SECOND = 1000;

    private static final long MINUTE = 60*SECOND;

    private void start(){
        Intent service_intent = new Intent(this, ESMService.class);
        //You can change a minimum or maximum value of random interval
        service_intent.putExtra(ESMService.EXTRA_INTERVAL_MIN, 15*MINUTE);
        service_intent.putExtra(ESMService.EXTRA_INTERVAL_MAX, 25*MINUTE);
        startService(service_intent);
        Intent sound_intent = new Intent(this, SoundService.class);
        startService(sound_intent);

    }

    private void stop(){
        Intent service_intent = new Intent(this, ESMService.class);
        stopService(service_intent);
        Intent sound_intent = new Intent(this, SoundService.class);
        stopService(sound_intent);

    }

    private void setSeekBarMax(AppCompatSeekBar sb, int max_value){
        sb.setMax((int)((max_value-min)/step));
    }
    private void setSeekBarchange(int progress, TextView tv){
        int value = min + (progress * step);
        //You can change text here
        tv.setText("threshold : ("+value+")");
        thresholdForDetection = value ;
    }
    private void setSeekBarAnimation(AppCompatSeekBar sb){
        int progress_half = 8;
        ObjectAnimator animation = ObjectAnimator.ofInt(sb, "progress", progress_half);
        animation.setDuration(100);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();

    }

    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    //External storage, recording permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length> 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] ==  PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }



        if ( requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length) {

            boolean check_result = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if ( check_result ) {

//                startCamera();
            }
            else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "Permission denied Please run the app again to allow permission.",
                            Snackbar.LENGTH_INDEFINITE).setAction("YES", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();

                }else {

                    Snackbar.make(mLayout, "Permissions should be allowed in Settings (app info).",
                            Snackbar.LENGTH_INDEFINITE).setAction("YES", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();
                }
            }

        }
    }



}
