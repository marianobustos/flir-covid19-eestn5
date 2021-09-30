package com.example.flircovid19.FaceDetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;


import androidx.annotation.RequiresApi;

import com.example.flircovid19.GraphicOverlay;
import com.example.flircovid19.GraphicOverlay.Graphic;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import static com.example.flircovid19.FaceDetection.FaceDetection.detected;
import static com.example.flircovid19.MainActivity.touchX;
import static com.example.flircovid19.MainActivity.touchY;

public class FaceGraphic extends Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    public static float x; // Coordenada X para el punto de medición de temperatura
    public static float y; // Coordenada Y para el punto de medición de temperatura

    private static final int[] COLOR_CHOICES = {
            Color.BLUE //, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW
    };
    private static int currentColorIndex = 0;

    private int facing;

    private final Paint facePositionPaint;
    private final Paint idPaint;
    private final Paint boxPaint;

    private volatile FirebaseVisionFace firebaseVisionFace;

    public FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        currentColorIndex = (currentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[currentColorIndex];

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        idPaint = new Paint();
        idPaint.setColor(selectedColor);
        idPaint.setTextSize(ID_TEXT_SIZE);

        boxPaint = new Paint();
        boxPaint.setColor(selectedColor);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    /**
     * Updates the face instance from the detection of the most recent frame. Invalidates the relevant
     * portions of the overlay to trigger a redraw.
     */
    public void updateFace(FirebaseVisionFace face, int facing) {
        firebaseVisionFace = face;
        this.facing = facing;
        postInvalidate();
    }

    /** Draws the face annotations for position on the supplied canvas. */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void draw(Canvas canvas) {
        FirebaseVisionFace face = firebaseVisionFace;
        if (face == null) {
            return;
        }



    /*canvas.drawText("id: " + face.getTrackingId(), x + ID_X_OFFSET, y + ID_Y_OFFSET, idPaint);
    canvas.drawText(
        "happiness: " + String.format("%.2f", face.getSmilingProbability()),
        x + ID_X_OFFSET * 3,
        y - ID_Y_OFFSET,
        idPaint);
    if (facing == CameraSource.CAMERA_FACING_FRONT) {
      canvas.drawText(
          "right eye: " + String.format("%.2f", face.getRightEyeOpenProbability()),
          x - ID_X_OFFSET,
          y,
          idPaint);
      canvas.drawText(
          "left eye: " + String.format("%.2f", face.getLeftEyeOpenProbability()),
          x + ID_X_OFFSET * 6,
          y,
          idPaint);
    } else {
      canvas.drawText(
          "left eye: " + String.format("%.2f", face.getLeftEyeOpenProbability()),
          x - ID_X_OFFSET,
          y,
          idPaint);
      canvas.drawText(
          "right eye: " + String.format("%.2f", face.getRightEyeOpenProbability()),
          x + ID_X_OFFSET * 6,
          y,
          idPaint);
    }
*/
        x = translateX(face.getBoundingBox().centerX());
        y = translateY(face.getBoundingBox().centerY());
        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
        float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        int minFaceDetectionX = 400; //Ancho minimo de deteccion de rostro #2021
        float margenX = 1100; //#2021 #TODO Definir valor
        float margenY = 850;
        float offSetX = 0;
        float offSetY = 200;

        //filtro de tamaño para deteccion de rostro #2021
        //compruebo que el ancho de la imagen sea mayor que el minimo
        System.out.println("FACE: Reconocimiento de rostro");
        // #2021 Dibujo la región válida
        canvas.drawRect(offSetX,offSetY,offSetX+margenX,offSetY+margenY,boxPaint);
        System.out.println("ROSTRO: ANCHO---->" + face.getBoundingBox().width());
        System.out.println("ROSTRO: BOTTOM: " + bottom );
        System.out.println("ROSTRO: TOP: " + top );
        System.out.println("ROSTRO: LEFT: " + left );
        System.out.println("ROSTRO: RIGHT: " + right );
        detected=false;
        if(right - left > minFaceDetectionX){
            System.out.println("FACE: Ancho OK");
            //compruebo que el rostro este dentro de la region establecida entre el origen de la imagen y los margenes X e Y establecidos
            if (left > 0 && right < canvas.getWidth() && top > 0 && bottom < canvas.getHeight()) {
                System.out.println("FACE: Rostro en imagen");
                //rectangulo dentro del canvas
                if (right < margenX && bottom < (margenY + offSetY)) {
                    System.out.println("FACE: Rostro válido");
                    //rectangulo valido, dibujo el rectangulo
                    detected=true;
                    System.out.println("ROSTRO: OK");
                    canvas.drawRect(left, top, right, bottom, boxPaint);
                    FaceDetection.FaceIsDetected(canvas,face);
                }
            }


        }
        // Draws a circle at the position of the detected face, with the face's track id below.
        y = translateY(face.getBoundingBox().centerY()-face.getBoundingBox().height()/4);
        // face.getBoundingBox().DrawingFaceEllipse
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);
    }
}
