package com.practice.serialzable.java_platform.model;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class UnSerializablePerson {

    private String name;

    private int age;

    public UnSerializablePerson(){

    }

    public UnSerializablePerson(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "UnSerializablePerson{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
