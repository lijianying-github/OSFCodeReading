# Android 平台序列化方案

## 实现原理
Android Parcel 序列化不是一种通用平台的序列化机制方案，主要致力于高性能的跨IPC传输实现的序列化机制。

因为Parcel中的不同数据类型的写入和读取实现规则（主要是类型写入的数据byte长度）不同Android版本代码不一致，  
这会导致旧版的数据可能不可读发序列化失败（针对系统升级时，会出现问题）。

因此不适合利用Parcel实现持久化，但是对于一个版本Android没有问题是可以使用的，但是考虑版本适配不推荐。

内部实现原理是：Parcel 的创建和回收都是native层C++实现，然后将创建的对象地址返回给Java层Parcel对象进行保存。  
Parcel对象在进行具体类型数据写入时直接调用Native C方法写入对于位数的byte数据到内存中（注意不同Android版本同一类型写入方式不同），  
写入完毕以后可以获取所有写入的数据进行本地持久化，也可以进行IPC通信
在Parcel初始创建时可以直接将一个字节数组输入到Parcel中，然后实现Parcelable接口对象可通过内部CREATOR接收Parcel完成对象的创建。

目标是借助于直接操作native C++完成数据读写，弥补Serializable java层面序列化性能不高，内存开销大的问题。但是实现相对比较复杂。

### 序列化流程：
1. 获取Parcel对象,写入对象并获取写入的字节数据
```
   UserParcelable userParcelable = new UserParcelable();
   userParcelable.setAge(666);
   userParcelable.setName("test parcelable");
        
   Parcel parcel = Parcel.obtain();
   userParcelable.writeToParcel(parcel, 0);
   //序列化，获取数据内容，保存本地，类似于手动本地序列化
   byte[] dataByte = parcel.marshall();
   //保存到文件
   FileUtils.save("parcel",dataByte)
   parcel.recycle()
   
```

### 反序列化流程：
1. 获取Parcel对象，写入持久化字节数据，设置读取位置为开始起始位置，读取数据到对象
```
    //获取序列化文件内容
     byte[] dataByte = FileUtils.getBytes("parcel");

     //反序列化：将byte数据流输入到Parcel对象中
     Parcel newParcel = Parcel.obtain();
     newParcel.unmarshall(dataByte, 0, dataByte.length);
     //设置数据开始读取的位置,默认是在最尾端，不调用直接拿值读取都是null
     newParcel.setDataPosition(0);

     UserParcelable newUser = UserParcelable.CREATOR.createFromParcel(newParcel);
     System.out.println(className + " after parcelable==" + newUser);
     newParcel.recycle();
```


## 实现方式：实现接口 Parcelable
1. 序列化类实现 Parcelable 接口
2. 必须创建Creator内部类实现
Demo如下：
```
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
 }
```

