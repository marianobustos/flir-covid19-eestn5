package com.example.flircovid19.Flir;

public interface StreamDataListener {
    void receiveImages(FlirFrameDataHolder dataHolder);
}
