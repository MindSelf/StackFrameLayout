package com.example.stackuplayout;

import androidx.annotation.ColorRes;

/**
 * Created by zhaolexi on 2020-01-16.
 */
class MyModel {

    @ColorRes
    int color;
    String text;

    MyModel(int color, String text) {
        this.color = color;
        this.text = text;
    }
}
