package com.practice.serialzable.java_platform.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Description:通用实现Externalizable接口实现序列化
 *
 * <p>
 * 1. 必须实现Serializable接口，未添加序列化、反序列化提示：NotSerializableException
 * 2. 必须有无空参构造函数
 * 3. 类需要实现writeExternal和readExternal方法,并且写和读的顺序必须一致
 * 4. 未添加serialVersionUID常量id时，修改字段时反序列化会失败提示id不一致
 * </p>
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class ExternalizableImpl implements Externalizable {

    //扩展字段包括添加移除修改字段时必须提供序列化UUID，否则提示序列化id不一致异常
    private static final long serialVersionUID = 5458280172741364397L;

    private String name;

    private int age;

    public ExternalizableImpl() {
        System.out.println("invoke 0 arg constructor===");
    }

    public ExternalizableImpl(String name, int age) {
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


    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        System.out.println("ExternalizableImpl writeExternal invoke ==");
        objectOutput.writeUTF(name);
        objectOutput.write(age);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        System.out.println("ExternalizableImpl readExternal invoke ==");
        name = objectInput.readUTF();
        age = objectInput.read();
    }
}
