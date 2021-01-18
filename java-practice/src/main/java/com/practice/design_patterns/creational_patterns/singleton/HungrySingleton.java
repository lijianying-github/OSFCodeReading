package com.practice.design_patterns.creational_patterns.singleton;

/**
 * Description:饿汉式静态方法创建单例
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class HungrySingleton {

    private final static HungrySingleton INSTANCE = new HungrySingleton();

    private HungrySingleton() {
        if (INSTANCE != null) {
            throw new RuntimeException("already creare instance==");
        }
    }

    public static HungrySingleton getInstance() {
        return INSTANCE;
    }

}
