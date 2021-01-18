package com.practice.design_patterns.creational_patterns.singleton;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class SingletonTest {

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {

            new Thread(() -> {

                HungrySingleton hungrySingleton = HungrySingleton.getInstance();
                System.out.println("hungrySingleton==" + hungrySingleton.hashCode()+"=="+ Thread.currentThread().getName());

                DoubleCheckLazySingleton doubleCheckLazySingleton = DoubleCheckLazySingleton.getInstance();
                System.out.println("doubleCheckLazySingleton==" + doubleCheckLazySingleton.hashCode()+"=="+ Thread.currentThread().getName());

                InnerClazzLazySingleton innerClazzLazySingleton = InnerClazzLazySingleton.getInstance();
                System.out.println("innerClazzLazySingleton==" + innerClazzLazySingleton.hashCode()+"=="+ Thread.currentThread().getName());
            }).start();
        }
    }
}
