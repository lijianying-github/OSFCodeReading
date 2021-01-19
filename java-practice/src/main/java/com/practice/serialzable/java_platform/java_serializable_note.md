# JAVA平台实现序列化流程

## 实现原理

### 序列化流程
当调用 ObjectOutStream.write(Object object)时，反射解析object class对象
1. 检查object以及object的成员引用对象有无实现Serializable接口，若未实现则抛出未实现序列化异常。
2. 打开文件对象，开始序列化流程
3. 计算object的serialVersionUID并保存到文件
4. 若object是实现 Serializable 接口则检查有无私有空返回方法writeObject有则调用该方法序列化字段到文件，若无该方法则逐个解析字段以及类型全部序列化
5. 若object是实现 Externalizable 接口则调用writeExternal方法序列化字段到文件

### 反序列化流程
当调用 ObjectInputStream.readObject()方法时，加载ObjectInputStream构造传入文件流。
1. 根据文件内容加载解析出对象描述信息类 ObjectStreamClass（内部包含生成class对象）
2. 根据class对象计算出新的serialVersionUID和ObjectStreamClass解析出的文件中的serialVersionUID比较，若不一致则异常终止。
3. 根据class反射构造方法生成目标对象object,判断class对象的实现接口方式
4. 若是实现 Serializable 接口则检查有无私有空返回方法readObject有则调用该方法中readXX反序列化字段到object，若无该方法则逐个解析class字段以及完成字段赋值
5. 若实现 Externalizable 接口则调用readExternal方法readXXX序列化字段到object
6. 解析完成

备注：serialVersionUID 目的验证版本的一致性，根据类class相关信息计算出的一个hash值，若重写该字段则使用该字段的值
具体使用该字段是在构建 ObjectStreamClass 对象时根据getDeclaredSUID方法先解析了class的serialVersionUID字段值，没有时再计算

## 实现接口 Serializable
1. 简单类（无引用或者继承对象）默认实现Serializable接口即可无特殊要求。
2. 有继承关系的子类实现序列化，若父类未实现序列化，则父类需要提供空参构造方法
3. 有继承关系的子类实现序列化，若父类实现序列化，则父类不需要提供空参构造方法
4. 有组合关系即序列化类中引用了其他类，则引用类必须要实现序列化
5. 未添加serialVersionUID常量字段，对序列化类字段修改时则会抛出id不一致异常
6. 未实现Serializable接口类强行使用 ObjectOutStream 和 ObjectInputStream 则抛出为实现Serializable异常

## 实现 Serializable 接口子接口 Externalizable
1. 限制规则和实现 Serializable接口一致
2. 需要手动实现 writeExternal 和 readExternal方法手动实现字段序列化
3. 字段序列化和反序列化顺序必须一致，不一致则反序列化异常失败

## 可控字段序列化实现
### 实现接口 Serializable可控字段序列化流程
1. 对字段添加transit标识符
2. 或者手动添加如下方法手动实现指定字段序列化，注意读写顺序一致（注意必须是如下格式）：

```java
    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(name);
        objectOutputStream.write(age);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        name = objectInputStream.readUTF();
        age = objectInputStream.read();
    }

```

### 实现接口 Externalizable 可控字段序列化流程
1. 对字段添加transit标识符
2. 在 writeExternal 和 readExternal方法中手动实现指定字段序列化,注意读写顺序一致

```java
    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeUTF(name);
        objectOutput.write(age);
    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        name = objectInput.readUTF();
        age = objectInput.read();
    }
 ```
