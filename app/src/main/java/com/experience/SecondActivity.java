package com.experience;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.experience.anotion.BindIntent;
import com.experience.anotion.BindIntentHelper;

public class SecondActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_MESSAGE="message";

    @BindIntent(key = EXTRA_KEY_MESSAGE)
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BindIntentHelper.parseIntent(this);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d("nemo","SecondActivity onCreate=====================");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("nemo","SecondActivity onNewIntent=====================");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("nemo","SecondActivity onRestart=====================");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("nemo","SecondActivity onStart=====================");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("nemo","SecondActivity onRestoreInstanceState=====================");
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        Log.d("nemo","SecondActivity onRestoreInstanceState2=====================");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("nemo","SecondActivity onResume=====================");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("nemo","SecondActivity onPause=====================");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("nemo","SecondActivity onSaveInstanceState=====================");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        Log.d("nemo","SecondActivity onSaveInstanceState 2=====================");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("nemo","SecondActivity onStop=====================");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("nemo","SecondActivity onDestroy=====================");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("nemo","SecondActivity onConfigurationChanged=====================");
    }
}