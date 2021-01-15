package di.scope

import javax.inject.Scope

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/13
 */
@Scope
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class ServiceSingleton()

