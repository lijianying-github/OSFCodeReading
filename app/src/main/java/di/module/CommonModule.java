package di.module;

import dagger.Module;
import dagger.Provides;
import di.model.ModelA;
import di.model.ModelB;
import di.model.ModelCWithA;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/8
 */
@Module
public class CommonModule {

    @Provides
    ModelA provideModelA(){
        return new ModelA();
    }

    @Provides
    ModelB provideModelB(){
        return new ModelB();
    }

    @Provides
    ModelCWithA provideModelCWithA(ModelA modelA){
        return new ModelCWithA(modelA);
    }


}
