package com.experience.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/29
 */
public class Person extends Animal {

    private String name;

    private int age;

    public String mark = "12w";

    public static int male = 1;

    private Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    private void show666() {
        System.out.println("6666====");
    }

    public String getName() {
        return name;
    }

    interface  A{}
    interface  B extends A{}

    public static void main(String[] args) {
        try {
            System.out.println("A interface class=="+A.class);
            System.out.println("B interface class=="+B.class);

            Class<Person> person = (Class<Person>) new Person().getClass();
            System.out.println("getName==" + person.getName());
            System.out.println("getSimpleName==" + person.getSimpleName());
            System.out.println("getCanonicalName==" + person.getCanonicalName());
            System.out.println("getModifiers==" + Modifier.toString(person.getModifiers()));

            Constructor<Person> cons = person.getDeclaredConstructor();
            Person ins=cons.newInstance();
            ins.show666();

            for (Field field : person.getDeclaredFields()) {
                System.out.println("filedName==" + field.getName());
                System.out.println("filedType==" + field.getType());
                System.out.println("filedGenType==" + field.getGenericType());
                System.out.println("filedValue==" + field.get(ins));
                System.out.println("=========================");
            }

            for (Method method : person.getDeclaredMethods()) {
                System.out.println("methodName==" + method.getName());
                System.out.println("methodReturnType==" + method.getReturnType());
                System.out.println("methodReturnType==" + method.getReturnType());
                System.out.println("=========================");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
