package com.example.flircovid19;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;


import com.example.flircovid19.FaceDetection.FaceDetection;
import com.example.flircovid19.FaceDetection.FaceDetectionProcessor;
import com.example.flircovid19.Flir.DiscoveryStatus;
import com.example.flircovid19.Flir.FlirCameraHandler;
import com.example.flircovid19.Flir.FlirFrameDataHolder;
import com.example.flircovid19.Flir.StreamDataListener;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.BuildConfig;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static com.example.flircovid19.FaceDetection.FaceDetection.setBitmapPreview;

public class MainActivity extends AppCompatActivity {
    //CAMERA
    private static final int PERMISSION_REQUESTS = 1;
    private static final String TAG = "MainActivity";
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private Context context;
    //FLIR
    private ImageView imgViewFlir;
    private FlirCameraHandler flirCameraHandler;
    private LinkedBlockingDeque<FlirFrameDataHolder> frameBuffer = new LinkedBlockingDeque<>(21);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        FaceDetection.setContext(this);
        preview = findViewById(R.id.firePreview);
        graphicOverlay = findViewById(R.id.fireFaceOverlay);

        if (allPermissionsGranted()) {
            createCameraSource();
        } else {
            getRuntimePermissions();
        }

        /**flir**/
        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;
        ThermalSdkAndroid.init(this, enableLoggingInDebug);

        imgViewFlir = findViewById(R.id.imgView_flir);
        flirCameraHandler = new FlirCameraHandler();
        discoveryStatus.started();
        flirCameraHandler.startDicovery(cameraDiscoveryEventListener, discoveryStatus);


    }

    @Override
    protected void onStart() {
        super.onStart();
        FaceDetection.setContext(this);
    }

    /**************************************LIFE CYCLE*************************************/




    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();

    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }


    /**************************************FLIR*************************************/

    private DiscoveryStatus discoveryStatus = new DiscoveryStatus() {
        @Override
        public void started() {

        }

        @Override
        public void stopped() {

        }
    };


    private DiscoveryEventListener cameraDiscoveryEventListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.w(TAG, "[cameraDiscoveryEventListener][onCameraFound]:" + identity.toString());
            flirCameraHandler.connect(identity, connectionStatusListener);
            flirCameraHandler.startStream(streamDataListener);
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            flirCameraHandler.disconnected();
        }
    };

    private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onDisconnected(ErrorCode errorCode) {
            flirCameraHandler.disconnected();
        }
    };

    private StreamDataListener streamDataListener = new StreamDataListener() {
        @Override
        public void receiveImages(FlirFrameDataHolder dataHolder) {
            try {
                frameBuffer.put(dataHolder);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FlirFrameDataHolder poll = frameBuffer.poll();
                        setBitmapPreview(poll.flirMap);
                        //imgViewFlir.setImageBitmap(poll.flirMap);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    };

    /************************************** Camera *************************************/

    private void createCameraSource() {
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }
        cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor(context));
    }

    private void startCameraSource() {
        if (cameraSource != null) {

            try {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
                preview.start(cameraSource, graphicOverlay);

            } catch (IOException e) {
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    /************************************** permisos *************************************/
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }


    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }
}