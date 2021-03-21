package com.example.flircovid19.FaceDetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

public class FaceDetection {


    private static float AXIS_MAJOR = 400;
    private static float AXIS_MENOR = 300;
    private static boolean detected = false;
    private static boolean awaint =false;
    private static int awaitingCount=0;
    public static void DrawingTemperature(Canvas canvas) {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void DrawingFaceEllipse(Canvas canvas) {
        float center_x = (canvas.getWidth() >> 1) - (AXIS_MENOR / 2);
        float center_y = (canvas.getHeight() >> 1) - (AXIS_MAJOR / 2);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);

        if (detected) {
            paint.setStrokeWidth(10);
            if(awaitingCount++>3){
                awaitingCount=0;
                paint.setColor(Color.parseColor("#ffc107"));
            }else paint.setColor(Color.parseColor("#b28704"));


        }
        else paint.setColor(Color.RED);

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
        double x = face.getBoundingBox().centerX() + 4;
        double y = face.getBoundingBox().centerY();

        //tolerancia
        double tolerance = 20;
        // Verifico si el tamaño de la cara es igual al tamaño del eje menor (eje_X) con una tolerancia de +-20px
        boolean faceIsWidth = Math.abs((face.getBoundingBox().width() - (AXIS_MENOR))) <= tolerance;
        boolean facePointIsCenter = Math.abs(x - center_x) <= tolerance && Math.abs(y - center_y) <= tolerance;
        //formula de ellipse= [(x-h)²/b²] + [(y-k)²/b²]=1
        //Verifico si el punto pertenece al elipse
        boolean isEmptyFaceInEllipse = (Math.pow((x - center_x), 2) / AXIS_MENOR) + (Math.pow((y - center_y), 2) / AXIS_MAJOR) <= AXIS_MAJOR;
        detected = isEmptyFaceInEllipse && faceIsWidth && facePointIsCenter;

    }

}
