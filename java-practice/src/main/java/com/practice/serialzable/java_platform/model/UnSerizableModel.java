package com.practice.serialzable.java_platform.model;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class UnSerizableModel {

    private String name="UnSerizableModel";

    public UnSerizableModel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UnSerizableModel{" +
                "name='" + name + '\'' +
                '}';
    }
}
