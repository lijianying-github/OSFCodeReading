package com.practice.serialzable.java_platform.model;

import java.io.Serializable;

/**
 * Description:通用实现Serializable接口实现序列化)
 * <p>
 * 1. 必须实现Serializable接口，未添加序列化、反序列化提示：NotSerializableException
 * 2. 序列化类无继承关系，有无空参构造函数都行
 * 3. 类不需要添加任何方法需要借助于ObjectInputStream 和ObjectInputStream实现真正的序列化过程
 * 4. 未添加serialVersionUID常量id时，修改字段时反序列化会失败提示id不一致
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class SerializableImpl implements Serializable {

    //扩展字段包括添加移除修改字段时必须提供序列化UUID，否则提示序列化id不一致异常
    private static final long serialVersionUID = 6239528625710871130L;

    private String name;

    private int age;

    public SerializableImpl(String name, int age) {
        System.out.println("invoke 2 arg constructor===");
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
        return "SerializablePerson{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
