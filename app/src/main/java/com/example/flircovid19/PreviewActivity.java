package com.example.flircovid19;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.flircovid19.FaceDetection.FaceDetection;
import com.example.flircovid19.Utils.ColorStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import static com.example.flircovid19.FaceDetection.FaceDetection.bitmap_preview;
import static com.example.flircovid19.FaceDetection.FaceDetection.hasMask;
import static com.example.flircovid19.FaceDetection.FaceDetection.temperature;

public class PreviewActivity extends AppCompatActivity {
    private ConstraintLayout constraintLayout;
    private ImageView imageView ;
    private ImageView imgViewAccessStatus;
    private TextView txtAccessStatus;
    private TextView txtMask;
    private TextView txtTemperature;
    private float temp=0;
    private boolean accessSuccess =false;
    private float TEMPERATURE_RANGE_DEFAULT= (float)37.5;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        FaceDetection.setContext(this);

        BigDecimal bd = new BigDecimal(temperature);
        temp=bd.setScale(1,RoundingMode.HALF_UP).floatValue();
        constraintLayout=findViewById(R.id.layouActivity);
        txtAccessStatus=findViewById(R.id.accessStatus);
        txtMask=findViewById(R.id.masked);
        txtTemperature=findViewById(R.id.temperature);
        imgViewAccessStatus = findViewById(R.id.imgAccessStatus);
        imageView=findViewById(R.id.img_preview);

        txtMask.append(hasMask?"SI":"NO");
        txtTemperature.append(String.valueOf(temp));
        txtTemperature.append("ºC");

        AccessStatus(temp<TEMPERATURE_RANGE_DEFAULT && hasMask);

        if(bitmap_preview != null){
            imageView.setImageBitmap(bitmap_preview);
        }
        TimeOutFinish(7);
    }

    private void AccessStatus(boolean accessSuccess){
        if(accessSuccess){
            constraintLayout.setBackgroundColor(Color.parseColor(ColorStatus.SUCCESS.getColor()));
            txtAccessStatus.setText("ACCESO PERMITIDO");
            imgViewAccessStatus.setBackgroundResource(R.drawable.status_success);
        }
        else{
            txtAccessStatus.setText("ACCESO DENEGADO");
            constraintLayout.setBackgroundColor(Color.parseColor(ColorStatus.DENIED.getColor()));
            imgViewAccessStatus.setBackgroundResource(R.drawable.status_error);
        }
    }


    private void TimeOutFinish(int seconds){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bitmap_preview=null;
                finish();
            }
        },seconds*1000);
    }

}