package di.component;

import com.osf.MainActivity;

import dagger.Component;
import di.module.CommonModule;

/**
 * Description:主component
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/8
 */
@Component(modules = CommonModule.class)
public interface MainComponent {

    void  injectActivity(MainActivity activity);
}
