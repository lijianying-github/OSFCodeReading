package com.practice.design_patterns.creational_patterns.singleton.java_version;

/**
 * Description:DCL懒汉式双重检查创建单例对象
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class UnSafeLazySingleton {

    //必须加volatile防止指令重排（可见性的体现和指令重排）
    private volatile static UnSafeLazySingleton instance;

    //防止反射调用
    private UnSafeLazySingleton() {
        if (instance != null) {
            throw new RuntimeException("instance already create==");
        }
    }

    /**
     * 非安全懒加载 创建单例
     *
     * @return instance
     */
    public static UnSafeLazySingleton getInstance() {
        if (instance == null) {
            instance = new UnSafeLazySingleton();
        }
        return instance;
    }
}
