package di.component

import android.app.Application
import android.widget.Toast
import dagger.Component
import di.module.AppModule
import javax.inject.Singleton

/**
 * Description:application component
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/13
 */
@Component(modules = [AppModule::class])
interface AppComponent {

    val toast: Toast

    fun injectApp(app: Application)

}
