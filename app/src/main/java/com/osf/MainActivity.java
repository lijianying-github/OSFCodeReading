package com.osf;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import javax.inject.Inject;

import di.component.DaggerMainComponent;
import di.scope.ModelA;
import di.scope.ModelCWithA;

public class MainActivity extends AppCompatActivity {

    @Inject
    ModelCWithA modelCWithA;

    @Inject
    ModelA modelA;


    private boolean isLaunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("nemo", "MainActivity onCreate=====================");
        DaggerMainComponent.create().injectActivity(this);
        Log.d("inject", "modelA ::"+modelA);
//        Log.d("inject", "modelCWithA ::"+modelCWithA);

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
            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
            intent.putExtra(SecondActivity.EXTRA_KEY_MESSAGE, "Main launch==");
            startActivity(intent);
            isLaunch = true;
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