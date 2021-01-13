package di.component;

import android.app.Application;

import dagger.Component;
import di.module.SingleInstanceModule;

/**
 * Description:子component
 *
 * component 在哪里创建，生命周期和创建处保持一致，对于Android 单例，要想全局有效，就需要在Application出创建
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/8
 */
@Component(modules = {SingleInstanceModule.class})
public interface SubComponent {
    /**
     * 在 Application 处创建所有依赖并注入到application中
     * @param application application
     */
    void  injectApp(Application application);
}
