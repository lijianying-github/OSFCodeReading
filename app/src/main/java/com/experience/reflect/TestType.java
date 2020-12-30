package com.experience.reflect;

import androidx.annotation.Nullable;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/30
 */
interface TestType<T> {

    public ArrayList[] testList(T a);

    public static void main(String[] args) {
        try {
            for (Method method : TestType.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    System.out.println( method.getParameterTypes()[0]);
                    hasUnresolvableType(method.getGenericReturnType());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean hasUnresolvableType(@Nullable Type type) {
        //类型是class，直接实现子类,不是泛型类
        if (type instanceof Class<?>) {
            System.out.println("TypeClass::" + type);
            return false;
        }
        //参数化类型：比如Map<String,Int>这种
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            System.out.println("ParameterizedType::" + parameterizedType);
            for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                System.out.println("ParameterizedActualType::" + typeArgument);
                if (hasUnresolvableType(typeArgument)) {
                    return true;
                }
            }
            return false;
        }
        //数组
        if (type instanceof GenericArrayType) {
            System.out.println("GenericArrayType::" + (GenericArrayType) type);
            hasUnresolvableType(((GenericArrayType) type).getGenericComponentType());
        }
        //类型变量比如：T,K这种
        if (type instanceof TypeVariable) {
            System.out.println("TypeVariable::" + type);
            return true;
        }
        //通配符类型 ? extends String ,? super String
        if (type instanceof WildcardType) {
            System.out.println("WildcardType::" + type);
            return true;
        }
        String className = type == null ? "null" : type.getClass().getName();
        String message = "Expected a Class, ParameterizedType, or "
                + "GenericArrayType, but <"
                + type
                + "> is of type "
                + className;

        System.out.println(message);
        throw new IllegalArgumentException(message);
    }
}
