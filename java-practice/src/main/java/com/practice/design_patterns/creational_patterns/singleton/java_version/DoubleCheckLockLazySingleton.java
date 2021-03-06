package com.practice.design_patterns.creational_patterns.singleton.java_version;

/**
 * Description:DCL懒汉式双重检查创建单例对象
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class DoubleCheckLockLazySingleton {

    //必须加volatile防止指令重排
    private volatile static  DoubleCheckLockLazySingleton instance;

    //防止反射调用
    private DoubleCheckLockLazySingleton() {
        if (instance != null) {
            throw new RuntimeException("instance already create==");
        }
    }

    /**
     * 双重检验DCL 创建单例
     *
     * @return instance
     */
    public static  DoubleCheckLockLazySingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckLockLazySingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckLockLazySingleton();
                }
            }
        }
        return instance;
    }
}
