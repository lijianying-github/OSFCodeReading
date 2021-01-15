package di.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import di.scope.SingleInstanceModelA;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/8
 */
@Module
public class SingleInstanceModule {

    @Singleton
    @Provides
    SingleInstanceModelA provideSingleInstanceA(){
        return new SingleInstanceModelA();
    }

}
