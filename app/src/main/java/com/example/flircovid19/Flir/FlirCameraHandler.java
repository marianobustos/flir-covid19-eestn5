package com.example.flircovid19.Flir;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.flircovid19.FaceDetection.FaceDetection;
import static com.example.flircovid19.FaceDetection.FaceGraphic.x;
import static com.example.flircovid19.FaceDetection.FaceGraphic.y;
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.DistanceUnit;
import com.flir.thermalsdk.image.JavaImageBuffer;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.image.Rectangle;
import com.flir.thermalsdk.image.RotationAngle;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.measurements.MeasurementCircle;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;
import com.flir.thermalsdk.image.palettes.PaletteManager;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.ThermalImageStreamListener;



import java.io.IOException;
import java.nio.Buffer;
import java.util.List;
import java.util.Objects;

import static com.example.flircovid19.FaceDetection.FaceDetection.x_face;
import static com.example.flircovid19.FaceDetection.FaceDetection.y_face;
import static com.example.flircovid19.MainActivity.touchX;
import static com.example.flircovid19.MainActivity.touchY;

public class FlirCameraHandler {
    private static final String TAG = "FlirCameraHandler";
    private Camera camera;
    private StreamDataListener streamDataListener;
    private boolean drawingCircle=true;

    public FlirCameraHandler() {
    }

    public void startDicovery(DiscoveryEventListener ccameraDiscoveryListener, DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().scan(ccameraDiscoveryListener, CommunicationInterface.USB);
        discoveryStatus.started();
    }

    public void stopDiscovery(DiscoveryStatus discoveryStatus) {
        DiscoveryFactory.getInstance().stop();
        discoveryStatus.stopped();
    }

    public void connect(Identity identity, ConnectionStatusListener connectionStatusListener) {

        try {
            if (camera != null) {
                camera.disconnect();
                camera = null;
            }
            camera = new Camera();
            camera.connect(identity, connectionStatusListener, new ConnectParameters());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnected() {
        if (camera == null) return;
        if (camera.isGrabbing()) camera.unsubscribeAllStreams();
        camera.disconnect();
        camera = null;
    }


    private final Camera.Consumer<ThermalImage> handleIncomingImage = new Camera.Consumer<ThermalImage>() {
        @Override
        public void accept(ThermalImage thermalImage) {
            thermalImage.setPalette(PaletteManager.getDefaultPalettes().get(0));//img filter
            thermalImage.setDistanceUnit(DistanceUnit.METER);
            JavaImageBuffer buffer = thermalImage.getImage();

            Bitmap flirBitmap = BitmapAndroid.createBitmap(buffer).getBitMap();
            /*Bitmap rgb = BitmapAndroid.createBitmap(thermalImage.getFusion().getPhoto()).getBitMap();
            //max_point 480x640
            //960x1280
             */
            //int x_point= (int) ((x_face*480)/1456);
            //int y_point= (int) ((y_face*640)/1092);

            try {

                thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS);

                double temperature = thermalImage.getValueAt(new Point(touchX,touchY));
                FaceDetection.temperature= (float) temperature;
                //System.out.println("FLIR:"+(int)x_point+"x"+(int)y_point+"TEMPERATURE:"+temperature);
                System.out.println("FLIR:temp:"+(int)touchX+"x"+(int)touchY+"TEMPERATURE:"+temperature);
                System.out.println("Punto_OK");


            }catch (Exception e){
                System.out.println("Punto_ERROR");
                System.out.println(e);
            }
            /*Canvas canvas = new Canvas(flirBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(20);
            canvas.drawCircle(thermalImage.getImage().width>>1, thermalImage.getImage().height>>1, 25, paint);
            paint.setColor(Color.BLUE);
            canvas.drawCircle(x_face, y_face, 25, paint);*/
            streamDataListener.receiveImages(new FlirFrameDataHolder(flirBitmap, 0.2));

        }
    };

    public void startStream(StreamDataListener listener) {
        this.streamDataListener = listener;
        camera.getRemoteControl().getCalibration().autoAdjust();
        camera.subscribeStream(new ThermalImageStreamListener() {
            @Override
            public void onImageReceived() {
                camera.withImage(this, handleIncomingImage);
            }
        });
    }


}
