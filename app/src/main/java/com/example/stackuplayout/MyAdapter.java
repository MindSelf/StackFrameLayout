package com.example.stackuplayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by zhaolexi on 2020-01-16.
 */
class MyAdapter extends StackFrameLayout.Adapter<TextView, MyModel> {

    @Override
    View createView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.view_template, parent, false);
    }

    @Override
    void bindView(TextView view, MyModel model) {
        Context context = view.getContext();
        view.setBackgroundColor(context.getResources().getColor(model.color));
        view.setText(model.text);
        view.setTag(model.text);
    }

}
