package com.practice.design_patterns.creational_patterns.singleton.java_version;

/**
 * Description:懒汉式匿名内部类创建单例
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class InnerClazzLazySingleton {

    private InnerClazzLazySingleton() {
        if (getInstance() != null) {
            throw new RuntimeException("already create singleton===");
        }
    }

    private static class LazyHolder {
        private static final InnerClazzLazySingleton INSTANCE = new InnerClazzLazySingleton();
    }

    public static InnerClazzLazySingleton getInstance() {
        return LazyHolder.INSTANCE;
    }

}
