package com.spring.annotation.processor.demo;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.spring.annotationbind.AnnotationManager;
import com.spring.annotations.BindInt;
import com.spring.annotations.BindString;

public class MainActivity extends AppCompatActivity {

    @BindString("bind string")
    public String message;

    @BindInt(123)
    public int messageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AnnotationManager.bind(this);
        Dialog dialog = new Dialog(this);
        dialog.setTitle("string:" + message + " int=" + messageId);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}
