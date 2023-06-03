package com.example.musicappv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Startup extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        Button button =findViewById(R.id.btnGetStarted);

        button.setOnClickListener(v -> {
            startActivity(new Intent(this, DisplaySongs.class));//rg3ha ya anwar
            finish();
        });
    }
}