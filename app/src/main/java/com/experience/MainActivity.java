package com.experience;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private boolean isLaunch=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("nemo", "MainActivity onCreate=====================");

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("nemo", "MainActivity onRestart=====================");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("nemo", "MainActivity onStart=====================");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("nemo", "MainActivity onResume=====================");
        if (!isLaunch) {
            startActivity(new Intent(MainActivity.this, SecondActivity.class));
            isLaunch=true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("nemo", "MainActivity onPause=====================");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("nemo", "MainActivity onStop=====================");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("nemo", "MainActivity onDestroy=====================");
    }
}