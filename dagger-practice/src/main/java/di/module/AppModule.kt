package di.module

import android.content.Context
import android.widget.Toast
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/13
 */
@Module
class AppModule(context: Context) {

    var mContext: Context = context

    @Provides
    fun provideApplicationContext(): Context {
        return mContext.applicationContext
    }

    @Provides
    fun providerToast(): Toast {
        return Toast.makeText(mContext, "666", Toast.LENGTH_SHORT)
    }


}