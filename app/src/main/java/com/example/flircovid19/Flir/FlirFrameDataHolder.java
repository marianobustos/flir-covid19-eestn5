package com.example.flircovid19.Flir;

import android.graphics.Bitmap;

public class FlirFrameDataHolder {
    public final Bitmap flirMap;
    public final Double temperature;

    public FlirFrameDataHolder(Bitmap flirMap, Double temperature) {
        this.flirMap = flirMap;
        this.temperature = temperature;
    }
}
