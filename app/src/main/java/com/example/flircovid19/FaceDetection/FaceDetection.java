package com.example.flircovid19.FaceDetection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.flircovid19.MainActivity;
import com.example.flircovid19.PreviewActivity;
import com.example.flircovid19.ml.FackMaskDetection;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;

import static com.example.flircovid19.FaceDetection.FaceGraphic.x;
import static com.example.flircovid19.FaceDetection.FaceGraphic.y;
import static com.example.flircovid19.MainActivity.debug;
import static com.example.flircovid19.MainActivity.touchX;
import static com.example.flircovid19.MainActivity.touchY;

public class FaceDetection {
    public static float temperature=50;
    public static boolean hasMask =false;
    public static float x_face = 0;
    public static float y_face = 0;
    public static float AXIS_MAJOR = 400;
    public static float AXIS_MENOR = 300;
    public static int DEFAULT_CENTER_X=1445>>1;
    public static int DEFAULT_CENTER_Y=1092>>1;
    public static int CENTER_X=0;
    public static int CENTER_Y=0;
    public static boolean detected = false;
    private static int awaint = 0;
    private static int awaitingCount = 0;
    private static Paint paintToleranceWidth = new Paint();
    private static Paint paintToleranceCenter = new Paint();
    private static Paint paintText = new Paint();
    private static int color;
    private static Context context;
    public static Bitmap bitmap_preview;
    private static boolean asd=false;
    //tolerancia
    public  static int tolerance_center = 40;
    public static int tolerance_width = 50;
    public static void setContext(Context ctx){
        context=ctx;
    }
    public static void setBitmapPreview(Bitmap bmp){
        bitmap_preview=bmp;
    }
    public static void DrawingText(Canvas canvas, String text, Paint paint) {
        paint.setTextSize(50f);
        paint.setTextAlign(Paint.Align.CENTER);


        canvas.drawText(
                text,
                (canvas.getWidth() >> 1)+CENTER_X,
                ((canvas.getHeight() >> 1) + (AXIS_MAJOR + 80) / 2)+CENTER_Y,
                paint
        );
    }

    //Circulo de tolerancia #2021
    /*public static void DrawingTolerance(Canvas canvas){
        float center_x = (canvas.getWidth() >> 1) +CENTER_X;
        float center_y = (canvas.getHeight() >> 1) +CENTER_Y;
        float drawInitX=center_x-(AXIS_MENOR/2)-(tolerance_width/2);
        float drawEndX=center_x+(AXIS_MENOR/2)-(tolerance_width/2);
        paintToleranceCenter.setColor(Color.RED);
        paintToleranceCenter.setStyle(Paint.Style.STROKE);
        paintToleranceCenter.setStrokeWidth(1f);
        canvas.drawCircle(center_x, center_y, tolerance_center, paintToleranceCenter);

        paintToleranceWidth.setColor(Color.RED);
        paintToleranceCenter.setStyle(Paint.Style.STROKE);
        paintToleranceWidth.setStrokeWidth(8);

        canvas.drawLine(drawInitX,center_y,drawInitX+tolerance_width,center_y,paintToleranceWidth);
        canvas.drawLine(drawEndX,center_y,drawEndX+tolerance_width,center_y,paintToleranceWidth);
    }*/

    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    
    //elipse de toma de temperatura #2021

    public static void DrawingFaceEllipse(Canvas canvas) {
        float center_x = ((canvas.getWidth() >> 1) - (AXIS_MENOR / 2))+CENTER_X;
        float center_y = ((canvas.getHeight() >> 1) - (AXIS_MAJOR / 2))+CENTER_Y;
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);

        //tiemp de espera para tomar temperatura
        if (detected) {

            paint.setStrokeWidth(10);
            paint.setColor(Color.parseColor("#ffc107"));
            paintText.setColor(Color.parseColor("#ffc107"));
            DrawingText(canvas, "Espere...", paintText);

            // Espero 5 segundos para impacientar al usuario
            if (awaitingCount++ > 5) {
                awaitingCount=0;
                if(context instanceof  MainActivity && !debug){
                    context.startActivity(new Intent(context, PreviewActivity.class));
                }
            }

        } else{
            paint.setColor(Color.RED);
            if(!hasMask){
                paintText.setColor(Color.RED);
                DrawingText(canvas, "SIN TAPA BOCA", paintText);
            }
            awaitingCount=0;

        }
        //if(debug){
         //   DrawingTolerance(canvas);
        //}
        //canvas.drawOval(center_x, center_y, center_x + AXIS_MENOR, center_y + AXIS_MAJOR, paint);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void FaceIsDetected(Canvas canvas, FirebaseVisionFace face) {
        //System.out.println("isDetected"+canvas.getWidth()+"x"+canvas.getHeight());
        Paint d = new Paint();
        d.setColor(Color.BLUE);
        //Punto medio de la img
        float center_x = (canvas.getWidth() >> 1)-CENTER_X;
        float center_y = (canvas.getHeight() >> 1)+CENTER_Y;

        //Punto de la frente
        //La camara termica esta 90° girada con respecto de la camara de la tablet
        //#2021 Tomo el punto de medicion a 3/4 de la cara en el eje vertical
        //esto se hace para no tomar la medicion sobre el barbijo
        float x = face.getBoundingBox().centerX();
        float y = face.getBoundingBox().centerY()-face.getBoundingBox().height()/4;
        // Actualizo punto de lectura
        // Luego de aplicar la transformación de escala y desplazamiento
        // delta x tablet 630 (1210 ----> 580)
        // delta y flir -453 (553 ----> 100)

        touchY = (int)((x -1210) * 453 / 630 + 553);
        // delta y tablet 32 (479 ---> 511)
        // delta x FLIR -22 (331 ---> 309)
        touchX = (int)((y - 479) * (-22) / 32 + 331);
        System.out.println("Punto_Tablet X:" + x);
        System.out.println("Punto_Tablet Y:" + y);
        System.out.println("Punto_FLIR X:" + touchX);
        System.out.println("Punto_FLIR Y:" + touchY);
        x_face = x;
        y_face = y;
        System.out.println("detected:" + x_face+"x"+y_face+"---->"+center_x+"x"+center_y);


        // Verifico si el tamaño de la cara es igual al tamaño del eje menor (eje_X) con una tolerancia de +-20px
        //boolean faceIsWidth = Math.abs((face.getBoundingBox().width() - (AXIS_MENOR))) <= tolerance_width;
        //boolean facePointIsCenter = Math.abs(x - center_x) <= tolerance_center && Math.abs(y - center_y) <= tolerance_center;
        //formula de ellipse= [(x-h)²/b²] + [(y-k)²/b²]=1
        //Verifico si el punto pertenece al elipse
        //boolean isEmptyFaceInEllipse = (Math.pow((x - center_x), 2) / AXIS_MENOR) + (Math.pow((y - center_y), 2) / AXIS_MAJOR) <= AXIS_MAJOR;
        //detected = isEmptyFaceInEllipse && faceIsWidth && facePointIsCenter;
        detected = true;
    }

    public static void maskDetected(Context context, Bitmap image){
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

            hasMask =withmask>witoutmask;
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

}
