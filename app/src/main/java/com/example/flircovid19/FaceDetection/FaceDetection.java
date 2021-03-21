package com.example.flircovid19.FaceDetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.flircovid19.ml.FackMaskDetection;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;

public class FaceDetection {
    public static boolean barbijo=false;
    public static float x_face = 0;
    public static float y_face = 0;
    private static float AXIS_MAJOR = 400;
    private static float AXIS_MENOR = 300;
    private static boolean detected = false;
    private static int awaint = 0;
    private static int awaitingCount = 0;
    private static Paint paintText = new Paint();
    private static int color;


    public static void DrawingText(Canvas canvas, String text, Paint paint) {
        paint.setTextSize(50f);
        paint.setTextAlign(Paint.Align.CENTER);


        canvas.drawText(
                text,
                (canvas.getWidth() >> 1),
                (canvas.getHeight() >> 1) + (AXIS_MAJOR + 80) / 2,
                paint
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void DrawingFaceEllipse(Canvas canvas) {
        float center_x = (canvas.getWidth() >> 1) - (AXIS_MENOR / 2);
        float center_y = (canvas.getHeight() >> 1) - (AXIS_MAJOR / 2);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
        if(!barbijo){
            paint.setColor(Color.RED);
            paintText.setColor(Color.RED);
            DrawingText(canvas, "SIN BARBIJO", paintText);
        }
        if (detected && barbijo) {
            if (awaitingCount++ > 5) {
                paint.setColor(Color.parseColor("#8bc34a"));
                paintText.setColor(Color.parseColor("#8bc34a"));
                DrawingText(canvas, "Temperatura: 34ºC", paintText);
            } else{
                paint.setStrokeWidth(10);
                paint.setColor(Color.parseColor("#ffc107"));
                paintText.setColor(Color.parseColor("#ffc107"));
                DrawingText(canvas, "Espere...", paintText);
            }

        } else{
             paint.setColor(Color.RED);
            awaitingCount=0;

        }

        canvas.drawOval(center_x, center_y, center_x + AXIS_MENOR, center_y + AXIS_MAJOR, paint);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void FaceIsDetected(Canvas canvas, FirebaseVisionFace face) {
        //System.out.println("isDetected"+canvas.getWidth()+"x"+canvas.getHeight());
        Paint d = new Paint();
        d.setColor(Color.BLUE);
        //Punto medio de la img
        float center_x = canvas.getWidth() >> 1;
        float center_y = canvas.getHeight() >> 1;

        //Punto de la frente
        float x = face.getBoundingBox().centerX() + 4;
        float y = face.getBoundingBox().centerY();

        x_face = x;
        y_face = y;

        //tolerancia
        double tolerance = 20;
        // Verifico si el tamaño de la cara es igual al tamaño del eje menor (eje_X) con una tolerancia de +-20px
        boolean faceIsWidth = Math.abs((face.getBoundingBox().width() - (AXIS_MENOR))) <= tolerance;
        boolean facePointIsCenter = Math.abs(x - center_x) <= tolerance && Math.abs(y - center_y) <= tolerance;
        //formula de ellipse= [(x-h)²/b²] + [(y-k)²/b²]=1
        //Verifico si el punto pertenece al elipse
        boolean isEmptyFaceInEllipse = (Math.pow((x - center_x), 2) / AXIS_MENOR) + (Math.pow((y - center_y), 2) / AXIS_MAJOR) <= AXIS_MAJOR;
        detected = isEmptyFaceInEllipse && faceIsWidth && facePointIsCenter;
        System.out.println("detected:" + detected);

    }

    public static void BarbijoDetected(Context context, Bitmap image){
        try {
            FackMaskDetection model = FackMaskDetection.newInstance(context);
            TensorImage img = TensorImage.fromBitmap(image);
            FackMaskDetection.Outputs outputs = model.process(img);
            List<Category> probabilities = outputs.getProbabilityAsCategoryList();
            float withmask=0;
            float witoutmask=0;
            for(Category probability:probabilities){
                if(probability.getLabel().equals("with_mask")) withmask=probability.getScore();
                else witoutmask=probability.getScore();
            }

            barbijo=withmask>witoutmask;
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

}
