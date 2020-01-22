package com.example.stackuplayout;

import android.os.Bundle;
import android.view.View;

import java.util.Stack;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MyAdapter adapter;
    private Stack<MyModel> data = new Stack<>();
    private int[] colors = {android.R.color.holo_red_light, android.R.color.holo_orange_dark,
            android.R.color.holo_green_dark, android.R.color.holo_blue_bright,
            android.R.color.holo_purple, android.R.color.white};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        StackFrameLayout stackFrameLayout = findViewById(R.id.stack_layout);
        stackFrameLayout.setAdapter(adapter = new MyAdapter());
        stackFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = data.size();
                data.push(new MyModel(colors[size % colors.length], String.valueOf(size)));
                adapter.setLinkRecordModel(data);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!data.isEmpty()) {
            data.pop();
            adapter.setLinkRecordModel(data);
        } else {
            super.onBackPressed();
        }
    }
}
