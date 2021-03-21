package com.example.flircovid19.Flir;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.DistanceUnit;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.image.Rectangle;
import com.flir.thermalsdk.image.RotationAngle;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.ThermalImage;
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
import java.util.Objects;

public class FlirCameraHandler {
    private static final String TAG = "FlirCameraHandler";
    private Camera camera;
    private StreamDataListener streamDataListener;

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
            thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS);
            thermalImage.setDistanceUnit(DistanceUnit.METER);

            Bitmap flirBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();
            Bitmap rgb = BitmapAndroid.createBitmap(thermalImage.getFusion().getPhoto()).getBitMap();


            //double temperature= thermalImage.getValueAt(new Point(1050,400));

            Canvas canvas = new Canvas(flirBitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawCircle(500, 400, 25, paint);


            streamDataListener.receiveImages(new FlirFrameDataHolder(rgb, 0.2));

        }
    };

    public void startStream(StreamDataListener listener) {
        this.streamDataListener = listener;
        camera.subscribeStream(new ThermalImageStreamListener() {
            @Override
            public void onImageReceived() {
                camera.withImage(this, handleIncomingImage);
            }
        });
    }


}
