package com.example.flircovid19;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;


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
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import pl.droidsonroids.gif.GifImageView;

import static com.example.flircovid19.FaceDetection.FaceDetection.AXIS_MAJOR;
import static com.example.flircovid19.FaceDetection.FaceDetection.AXIS_MENOR;
import static com.example.flircovid19.FaceDetection.FaceDetection.CENTER_X;
import static com.example.flircovid19.FaceDetection.FaceDetection.CENTER_Y;
import static com.example.flircovid19.FaceDetection.FaceDetection.DEFAULT_CENTER_X;
import static com.example.flircovid19.FaceDetection.FaceDetection.DEFAULT_CENTER_Y;
import static com.example.flircovid19.FaceDetection.FaceDetection.setBitmapPreview;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
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
    //DEBUG
    private ImageView imgViewBtn;
    private ConstraintLayout defaultLayout, debugLayout;
    private Button btnSave, btnCancel;
    private GifImageView gif;
    private int auxAxisMenor, auxAxisMejor;
    private int auxCenterX, auxCenterY;
    //elipse length
    private SeekBar ellipseX, ellipseY;
    private TextView txtEllipseX, txtEllipseY;

    private final String KEY_AXIS_X = "KEY_AXIS_X";
    private final String KEY_AXIS_Y = "KEY_AXIS_Y";
    //elipse centro
    private SeekBar ellipseCenterX, ellipseCenterY;
    private TextView txtEllipseCenterX, txtEllipseCenterY;
    private final String KEY_AXIS_CENTER_X = "KEY_AXIS_CENTER_X";
    private final String KEY_AXIS_CENTER_Y = "KEY_AXIS_CENTER_Y";
    //DEBUG
    public static Boolean debug = false;
    //DRAWING
    private TextView txtTemperatura;
    Canvas canvas;
    Paint paint;
    //TEMPERATURA
    private int auxTouchX, auxtouchY;
    public static int touchX = 0, touchY = 0;
    private final String KEY_AXIS_TEMPERATURE_X = "KEY_AXIS_TEMPERATURE_X";
    private final String KEY_AXIS_TEMPERATURE_Y = "KEY_AXIS_TEMPERATURE_Y";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        //shared
        sharedPreferences = this.getSharedPreferences("APP", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //IA
        FaceDetection.setContext(this);
        preview = findViewById(R.id.firePreview);
        graphicOverlay = findViewById(R.id.fireFaceOverlay);

        if (allPermissionsGranted()) {
            createCameraSource();
        } else {
            getRuntimePermissions();
        }

        defaultAxis();
        getAxis();

        /**flir**/
        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;
        ThermalSdkAndroid.init(this, enableLoggingInDebug);

        imgViewFlir = findViewById(R.id.imgView_flir);
        imgViewFlir.getLayoutParams().height = 640;
        imgViewFlir.getLayoutParams().width = 480;
        paint = new Paint();

        txtTemperatura = findViewById(R.id.txt_temperatura);

        imgViewFlir.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int[] viewCoords = new int[2];
                imgViewFlir.getLocationOnScreen(viewCoords);
                touchX = (int) event.getX();
                touchY = (int) event.getY();

                return true;
            }
        });
        flirCameraHandler = new FlirCameraHandler();
        discoveryStatus.started();
        flirCameraHandler.startDicovery(cameraDiscoveryEventListener, discoveryStatus);

        /**DEBUG**/
        gif = findViewById(R.id.gif);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        defaultLayout = findViewById(R.id.default_layout);
        debugLayout = findViewById(R.id.layout_debug);
        imgViewBtn = findViewById(R.id.bt_debug);
        imgViewBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                getAxis();
                defaultAxis();
                ToggleView();
                return false;
            }
        });

        btnSave.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        /*ellipse*/
        txtEllipseX = findViewById(R.id.txtEllipseX);
        txtEllipseY = findViewById(R.id.txtEllipseY);
        ellipseX = findViewById(R.id.elipse_x);
        ellipseY = findViewById(R.id.elipse_y);

        txtEllipseX.setText("Elipse X: ".concat(String.valueOf(AXIS_MENOR)).concat("px"));
        ellipseX.setProgress((int) AXIS_MENOR);
        ellipseX.setMax(1000);
        ellipseX.setOnSeekBarChangeListener(ellipseXListener);


        txtEllipseY.setText("Elipse Y: ".concat(String.valueOf(AXIS_MAJOR)).concat("px"));
        ellipseY.setProgress((int) AXIS_MAJOR);
        ellipseY.setMax(1000);
        ellipseY.setOnSeekBarChangeListener(ellipseYListener);

        //ellipse center
        txtEllipseCenterX = findViewById(R.id.txtEllipseCenterX);
        txtEllipseCenterY = findViewById(R.id.txtEllipseCenterY);
        ellipseCenterX = findViewById(R.id.elipse_center_x);
        ellipseCenterY = findViewById(R.id.elipse_center_y);


        txtEllipseCenterY.setText("Elipse centro Y: ".concat(String.valueOf(CENTER_Y)).concat("px"));
        ellipseCenterY.setProgress(CENTER_Y);
        ellipseCenterY.setMax(DEFAULT_CENTER_Y * 2);

        txtEllipseCenterX.setText("Elipse centro X: ".concat(String.valueOf(CENTER_X)).concat("px"));
        ellipseCenterX.setProgress(CENTER_X);
        ellipseCenterX.setMax(DEFAULT_CENTER_X * 2);

        ellipseCenterX.setOnSeekBarChangeListener(ellipseCenterXListener);
        ellipseCenterY.setOnSeekBarChangeListener(ellipseCenterYListener);


    }

    /************************************** DEBUG *************************************/
    private void getAxis() {
        AXIS_MENOR = sharedPreferences.getFloat(KEY_AXIS_X, 300);
        AXIS_MAJOR = sharedPreferences.getFloat(KEY_AXIS_Y, 400);
        CENTER_X = sharedPreferences.getInt(KEY_AXIS_CENTER_X, 0);
        CENTER_Y = sharedPreferences.getInt(KEY_AXIS_CENTER_Y, 0);
        touchX = sharedPreferences.getInt(KEY_AXIS_TEMPERATURE_X, 240);
        touchY = sharedPreferences.getInt(KEY_AXIS_TEMPERATURE_Y, 620);
    }

    private void SaveAxis(float x, float y, int cx, int cy, int tx, int ty) {
        editor.putFloat(KEY_AXIS_X, x);
        editor.putFloat(KEY_AXIS_Y, y);
        editor.putInt(KEY_AXIS_CENTER_X, cx);
        editor.putInt(KEY_AXIS_CENTER_Y, cy);
        editor.putInt(KEY_AXIS_TEMPERATURE_X, tx);
        editor.putInt(KEY_AXIS_TEMPERATURE_Y, ty);
        editor.commit();
    }

    private void defaultAxis() {
        auxAxisMejor = (int) AXIS_MAJOR;
        auxAxisMenor = (int) AXIS_MENOR;
        auxCenterX = CENTER_X;
        auxCenterY = CENTER_Y;
        auxTouchX = touchX;
        auxtouchY = touchY;

    }

    //ELLIPSE CENTROS
    SeekBar.OnSeekBarChangeListener ellipseCenterXListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            CENTER_X = i - DEFAULT_CENTER_X;
            txtEllipseCenterX.setText("Elipse centro X: ".concat(String.valueOf(CENTER_X)).concat("px"));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener ellipseCenterYListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            CENTER_Y = i - DEFAULT_CENTER_Y;
            txtEllipseCenterY.setText("Elipse centro Y: ".concat(String.valueOf(CENTER_Y)).concat("px"));


        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };


    //ELLIPSE
    SeekBar.OnSeekBarChangeListener ellipseXListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            AXIS_MENOR = i;
            txtEllipseX.setText("Elipse X: ".concat(String.valueOf(i)).concat("px"));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener ellipseYListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            AXIS_MAJOR = i;
            txtEllipseY.setText("Elipse Y: ".concat(String.valueOf(i)).concat("px"));


        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                ToggleView();
                AXIS_MAJOR = auxAxisMejor;
                AXIS_MENOR = auxAxisMenor;
                CENTER_X = auxCenterX;
                CENTER_Y = auxCenterY;
                txtEllipseX.setText("Elipse X: ".concat(String.valueOf(AXIS_MENOR)).concat("px"));
                txtEllipseY.setText("Elipse Y: ".concat(String.valueOf(AXIS_MAJOR)).concat("px"));
                txtEllipseCenterX.setText("Elipse centro X: ".concat(String.valueOf(CENTER_X)).concat("px"));
                txtEllipseCenterY.setText("Elipse centro Y: ".concat(String.valueOf(CENTER_Y)).concat("px"));

                break;
            case R.id.btn_save:
                ToggleView();
                SaveAxis(AXIS_MENOR, AXIS_MAJOR, CENTER_X, CENTER_Y, touchX, touchY);
                break;
        }

    }

    private void GifRandomResource() {
        final int MARIANO = 0, DELUCAS = 1, MARKELOFF = 2;
        int[] numbers = {MARIANO, DELUCAS, MARKELOFF};
        int num = numbers[new Random().nextInt(numbers.length)];
        switch (num) {
            case MARIANO:

                gif.setBackgroundResource(R.drawable.gif_bustos);
                break;
            case DELUCAS:
                gif.setBackgroundResource(R.drawable.de_lucas);

                break;
            case MARKELOFF:
                gif.setBackgroundResource(R.drawable.marquez);
                break;

        }


    }

    private void ToggleView() {

        GifRandomResource();
        if (defaultLayout.getVisibility() == View.VISIBLE) {
            //debug view visibility
            debug = true;
            defaultLayout.setVisibility(View.INVISIBLE);
            debugLayout.setVisibility(View.VISIBLE);
            /*gif.setVisibility(View.VISIBLE);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    gif.setVisibility(View.INVISIBLE);
                }
            }, 3500);*/
        } else {
            //default layout visibility
            debug = false;
            defaultLayout.setVisibility(View.VISIBLE);
            debugLayout.setVisibility(View.INVISIBLE);
            gif.setVisibility(View.INVISIBLE);
        }

    }


    /**************************************LIFE CYCLE*************************************/

    @Override
    protected void onStart() {
        super.onStart();
        FaceDetection.setContext(this);
    }


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

                        Bitmap resize = Bitmap.createScaledBitmap(poll.flirMap, 480, 640, false);
                        canvas = new Canvas(resize);
                        paint.setColor(Color.GREEN);
                        canvas.drawCircle(touchX, touchY, 5, paint);
                        txtTemperatura.setText("Temperatura:" + FaceDetection.temperature + "ªC");
                        imgViewFlir.setImageBitmap(resize);
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
