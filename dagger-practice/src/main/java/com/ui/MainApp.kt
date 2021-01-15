package com.ui

import android.app.Application
import android.content.Context
import di.component.AppComponent
import di.component.DaggerAppComponent
import di.module.AppModule

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/13
 */
class MainApp : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
    }
}