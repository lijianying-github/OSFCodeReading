package com.practice.serialzable.java_platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Description:触发序列化工具类
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class SerializableUtils {

    private static final String SerializablePath = "E:\\OkhttpExperience\\java-practice\\src\\main\\java\\com\\practice\\serialzable\\file";

    public static void saveObject(Object object, String fileName) {
        File file = new File(SerializablePath , fileName + ".out");
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            //解析目标对象class生成二进制文件，若Serializable实现类有writeObject方法则调用类的方法
            objectOutputStream.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T readObject(String fileName) {

        File file = new File(SerializablePath, fileName + ".out");
        try {
            //解析二进制文件生成解析对象，若Serializable实现类有readObject方法则调用类的方法
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
            return (T) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
