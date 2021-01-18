package com.practice.design_patterns.creational_patterns.singleton;

import org.apache.lucene.util.RamUsageEstimator;

import java.lang.reflect.Constructor;

/**
 * Description:JVM创建一个对象默认16bytes 创建一个枚举默认24bytes
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class SingletonTest {

    public static void main(String[] args) {
        testMultiThreadCreate();
//        testMultiThreadReflectCreate();
    }

    /**
     * 多线程创建对象模拟
     */
    private static void testMultiThreadCreate() {
        for (int i = 0; i < 10; i++) {

            new Thread(() -> {
                testHungrySingleton();
                testKotlinSingleton();
                testDoubleCheckLazySingleton();
                testInnerClazzLazySingleton();
                testEnumSingleton();
            }).start();
        }
    }

    /**
     * 多线程反射常见的对象模拟
     */
    private static void testMultiThreadReflectCreate() {
        for (int i = 0; i < 10; i++) {

            new Thread(() -> {
                testSingletonReflectInvoke(HungrySingleton.class);
                testSingletonReflectInvoke(InnerClazzLazySingleton.class);
                testSingletonReflectInvoke(DoubleCheckLazySingleton.class);
                testSingletonReflectInvoke(KotlinSingleton.class);
            }).start();
        }
    }

    private static void testHungrySingleton() {
        HungrySingleton hungrySingleton = HungrySingleton.getInstance();
        String size = RamUsageEstimator.humanReadableUnits(RamUsageEstimator.shallowSizeOf(hungrySingleton));
        System.out.println("hungrySingleton==" + hungrySingleton.hashCode()
                + "==" + Thread.currentThread().getName() + "==size::" + size);

    }

    private static void testInnerClazzLazySingleton() {
        InnerClazzLazySingleton innerClazzLazySingleton = InnerClazzLazySingleton.getInstance();
        String size = RamUsageEstimator.humanReadableUnits(RamUsageEstimator.shallowSizeOf(innerClazzLazySingleton));
        System.out.println("innerClazzLazySingleton==" + innerClazzLazySingleton.hashCode()
                + "==" + Thread.currentThread().getName() + "==size::" + size);
    }

    private static void testDoubleCheckLazySingleton() {
        DoubleCheckLazySingleton doubleCheckLazySingleton = DoubleCheckLazySingleton.getInstance();
        String size = RamUsageEstimator.humanReadableUnits(RamUsageEstimator.shallowSizeOf(doubleCheckLazySingleton));
        System.out.println("doubleCheckLazySingleton==" + doubleCheckLazySingleton.hashCode()
                + "==" + Thread.currentThread().getName() + "==size::" + size);
    }

    private static void testEnumSingleton() {
        EnumSingleton enumSingleton = EnumSingleton.INSTANCE;
        String size = RamUsageEstimator.humanReadableUnits(RamUsageEstimator.shallowSizeOf(enumSingleton));
        System.out.println("enumSingleton==" + enumSingleton.hashCode()
                + "==" + Thread.currentThread().getName() + "==size::" + size);
    }

    //kotlin 版单例创建，反射可以创建多个对象
    private static void testKotlinSingleton() {
        KotlinSingleton kotlinSingleton = KotlinSingleton.INSTANCE;
        String size = RamUsageEstimator.humanReadableUnits(RamUsageEstimator.shallowSizeOf(kotlinSingleton));
        System.out.println("kotlinSingleton==" + kotlinSingleton.hashCode()
                + "==" + Thread.currentThread().getName() + "==size::" + size);
    }

    private static <T> void testSingletonReflectInvoke(Class<T> clazz) {
        try {
            Constructor<T> kotlinSingletonConstructor = clazz.getDeclaredConstructor();
            kotlinSingletonConstructor.setAccessible(true);
            T reflectInstance = kotlinSingletonConstructor.newInstance();
            System.out.println("kotlinSingleton reflect==" + reflectInstance.hashCode() + "==" + Thread.currentThread().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
