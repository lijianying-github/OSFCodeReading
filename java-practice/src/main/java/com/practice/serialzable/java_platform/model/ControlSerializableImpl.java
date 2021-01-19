package com.practice.serialzable.java_platform.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Description:可控制指定字段序列化过程的 实现Serializable接口序列化
 * <p>
 * 1. 必须实现Serializable接口，未添加序列化、反序列化提示：NotSerializableException
 * 2. 序列化类无继承关系，有无空参构造函数都行
 * 3. 类不需要添加任何方法需要借助于ObjectInputStream 和ObjectInputStream实现真正的序列化过程
 * 4. 未添加serialVersionUID常量id时，修改字段时反序列化会失败提示id不一致
 * <p>
 * 5. 字段可控序列化方式添加transient标注或者添加私有方法
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class ControlSerializableImpl implements Serializable {

    //扩展字段包括添加移除修改字段时必须提供序列化UUID，否则提示序列化id不一致异常
    private static final long serialVersionUID = 6239528625710871130L;

    private String name;

    private int age;

    //静态和transient标志的不会被序列化和反序列化
    public transient String add;

    public ControlSerializableImpl(String name, int age, String add) {
        System.out.println("invoke 3 arg constructor===");
        this.name = name;
        this.age = age;
        this.add = add;
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

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        System.out.println("ControlSerializableImpl writeObject invoke ==");
        objectOutputStream.writeUTF(name);
        objectOutputStream.write(age);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        System.out.println("ControlSerializableImpl readObject invoke ==");
        name = objectInputStream.readUTF();
        age = objectInputStream.read();
    }

    @Override
    public String toString() {
        return "ControlSerializableImpl{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", add='" + add + '\'' +
                '}';
    }
}
