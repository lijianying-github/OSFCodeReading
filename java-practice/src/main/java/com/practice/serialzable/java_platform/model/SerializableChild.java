package com.practice.serialzable.java_platform.model;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class SerializableChild extends UnSerializablePerson {

    private String gender;

    public SerializableChild(String name, int age) {
        super(name, age);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "SerializablePersonChild{" +
                "gender='" + gender + '\'' +
                '}';
    }
}
