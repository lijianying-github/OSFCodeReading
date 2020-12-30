package com.experience.anotion;

import androidx.annotation.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Description:内置元注解：Retention,Target,Documented,Inherited,Repeatable
 * 也可以自己创建元注解，只需要把Target中添加ANNOTATION_TYPE
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/28
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.ANNOTATION_TYPE})
public @interface BindIntent {

    //枚举成员属性：类型只能是八大基本数据类型，String，Class,枚举以及他们的数组
    @NonNull String key() default "";
}
