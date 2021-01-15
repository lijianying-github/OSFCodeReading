package di

import android.app.Activity
import android.content.Context
import com.ui.MainApp
import di.component.ActivityComponent
import di.component.AppComponent
import di.component.DaggerActivityComponent

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/13
 */

fun Context.appComponent(): AppComponent {
    val mainApp=this.applicationContext as MainApp
    return mainApp.appComponent
}

fun Activity.activityDiComponent(): ActivityComponent {
    return DaggerActivityComponent.builder().appComponent(appComponent()).build()
}