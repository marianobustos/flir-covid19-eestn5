package com.example.flircovid19.Utils;

public enum ColorStatus {
    SUCCESS("#388e3c"),
    DENIED("#9a0036");
    private String color;
    ColorStatus(String color){
        this.color=color;
    }
    public String getColor() {
        return color;
    }
}
