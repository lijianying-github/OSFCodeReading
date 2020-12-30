package com.experience.anotion;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Description:intent  intent自动解析赋值实现
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/28
 */
public class BindIntentHelper {

    public static void parseIntent(Activity activity) {

        Bundle data = activity.getIntent().getExtras();

        if (data == null) {
            return;
        }

        //获取当前类的所有声明方法包括private,不包含父类方法,getFields()可以获取父类非private方法
        Field[] allFields = activity.getClass().getDeclaredFields();
        for (Field field : allFields) {
            //判断字段是否有指定类型注解标记
            if (field.isAnnotationPresent(BindIntent.class)) {
                //获取注解实例
                BindIntent bindIntent = field.getAnnotation(BindIntent.class);
                //获取注解的值
                String key = bindIntent.key();

                if (!data.containsKey(key)) {
                    return;
                }

                //获取field的类型
                Class<?> fieldType = field.getType().getComponentType();

                Object value = data.get(key);

                if (field.getType().isArray() && Parcelable.class.isAssignableFrom(fieldType)) {
                    //parcel list需要单独处理，跟parcel的
                    Object[] objs = (Object[]) value;
                    value = Arrays.copyOf(objs, objs.length, (Class<? extends Object[]>) field.getType());
                }

                //设置访问权限并赋值给要反射的字段
                field.setAccessible(true);
                try {
                    field.set(activity, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
