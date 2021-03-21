package com.example.flircovid19.FaceDetection;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.flircovid19.FrameMetadata;
import com.example.flircovid19.GraphicOverlay;
import com.example.flircovid19.VisionProcessorBase;
import com.example.flircovid19.ml.FackMaskDetection;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;

import static com.example.flircovid19.FaceDetection.FaceDetection.barbijo;

public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private Context context;
    private final FirebaseVisionFaceDetector detector;
    private boolean isEmptyFaces=false;

    public FaceDetectionProcessor(Context context) {
        this.context=context;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }
    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
     //       Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }
    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        if(isEmptyFaces){
         Thread barbijoThread= new Thread(){
                @Override
                public void run() {
                    super.run();
                    FaceDetection.BarbijoDetected(context,image.getBitmap());
                }
            };
            barbijoThread.start();
        }

        return detector.detectInImage(image);

    }

    @Override
    protected void onSuccess(@NonNull List<FirebaseVisionFace> faces, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {

        graphicOverlay.clear();
        isEmptyFaces=faces.size()>0;
        for (int i = 0; i < faces.size(); ++i) {
            FirebaseVisionFace face = faces.get(i);
            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay);
            graphicOverlay.add(faceGraphic);
            faceGraphic.updateFace(face, frameMetadata.getCameraFacing());
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }
}
