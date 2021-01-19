package com.practice.serialzable.android_platform;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class UserParcelable implements Parcelable {

    private String name;

    private int age;

    public UserParcelable() {
    }

    protected UserParcelable(Parcel in) {
        name = in.readString();
        age = in.readInt();
    }

    public static final Creator<UserParcelable> CREATOR = new Creator<UserParcelable>() {
        @Override
        public UserParcelable createFromParcel(Parcel in) {
            return new UserParcelable(in);
        }

        @Override
        public UserParcelable[] newArray(int size) {
            return new UserParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(age);
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
        return "UserParcelable{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    public static void main(String[] args) {

        String className = UserParcelable.class.getSimpleName();

        UserParcelable userParcelable = new UserParcelable();
        userParcelable.setAge(666);
        userParcelable.setName("test parcelable");

        System.out.println(className + " before parcelable==" + userParcelable);

        Parcel parcel = Parcel.obtain();
        userParcelable.writeToParcel(parcel, 0);

        //序列化，获取数据内容，保存本地，类似于手动本地序列化
        byte[] dataByte = parcel.marshall();

        //反序列化：将byte数据流输入到Parcel对象中
        Parcel newParcel = Parcel.obtain();
        newParcel.unmarshall(dataByte, 0, dataByte.length);
        //设置数据开始读取的位置,默认是在最尾端，不调用直接拿值读取都是null
        newParcel.setDataPosition(0);

        UserParcelable newUser = UserParcelable.CREATOR.createFromParcel(newParcel);
        System.out.println(className + " after parcelable==" + newUser);
        newParcel.recycle();

        parcel.recycle();
    }
}
