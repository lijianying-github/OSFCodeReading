package com.experience.anotion;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.Toast;

import java.lang.reflect.Field;

/**
 * Description:intent  intent自动解析赋值实现
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/28
 */
public class BindIntentHelper {

    public static void parseIntent(Activity activity) {
        //获取当前类的所有声明方法包括private,不包含父类方法,getFields()可以获取父类非private方法
        Field[] allFields = activity.getClass().getDeclaredFields();
        for (Field field : allFields) {
            //判断字段是否有指定类型注解标记
            if (field.isAnnotationPresent(BindIntent.class)) {
                //获取注解实例
                BindIntent bindIntent = field.getAnnotation(BindIntent.class);
                //获取注解的值
                String key = bindIntent.key();
                String defaultValue = bindIntent.defaultValue();
                //处理
                String value = activity.getIntent().getStringExtra(key);
                //设置访问权限并赋值给要反射的字段
                field.setAccessible(true);
                if (TextUtils.isEmpty(value)) {
                    value = defaultValue;
                }
                try {
                    String initValue= (String) field.get(activity);
                    Toast.makeText(activity,"initValue::"+initValue,Toast.LENGTH_SHORT).show();
                    field.set(activity, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
