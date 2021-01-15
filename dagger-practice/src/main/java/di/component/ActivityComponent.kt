package di.component

import com.ui.MainActivity
import dagger.Component
import di.module.ActivityModule
import di.scope.ActivitySingleton

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/13
 */
@Component(modules = [ActivityModule::class], dependencies = [AppComponent::class])
@ActivitySingleton
interface ActivityComponent {

    fun  injectMainActivity(activity:MainActivity)

}