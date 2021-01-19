package com.practice.serialzable.java_platform;

import com.practice.serialzable.java_platform.model.ControlSerializableImpl;
import com.practice.serialzable.java_platform.model.ExternalizableImpl;
import com.practice.serialzable.java_platform.model.SerializableImpl;
import com.practice.serialzable.java_platform.model.SerializablePerson;
import com.practice.serialzable.java_platform.model.SerializablePersonChild;
import com.practice.serialzable.java_platform.model.SerizableModel;
import com.practice.serialzable.java_platform.model.UnSerializablePerson;
import com.practice.serialzable.java_platform.model.UnSerializablePersonWithSerializableChild;
import com.practice.serialzable.java_platform.model.UnSerizableModel;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/19
 */
public class JavaPlatformSerializableTest {

    public static void main(String[] args) {
//        testSimpleSerializable(true);
//        testSimpleExternalizable();
        testExtendSerializable(false);
//        testCombineSerializable();
    }

    //测试实现Serializable接口序列化
    private static void testSimpleSerializable(boolean isControl) {

        try {
            if (!isControl) {
                String fileName = SerializableImpl.class.getSimpleName();
                SerializableImpl instance = new SerializableImpl("tes serizable", 13);

                //触发序列化
                SerializableUtils.saveObject(instance, fileName);
                System.out.println(fileName + " before serializable ::" + instance);

                Thread.sleep(5000);

                //触发反序列化
                SerializableImpl newInstance = SerializableUtils.readObject(fileName);
                System.out.println(fileName + " after serializable newInstance::" + newInstance);
            }else {
                String fileName = ControlSerializableImpl.class.getSimpleName();
                ControlSerializableImpl instance = new ControlSerializableImpl("test control serizable", 13,"add");

                //触发序列化
                SerializableUtils.saveObject(instance, fileName);
                System.out.println(fileName + " before serializable ::" + instance);

                Thread.sleep(5000);

                //触发反序列化
                ControlSerializableImpl newInstance = SerializableUtils.readObject(fileName);
                System.out.println(fileName + " after serializable newInstance::" + newInstance);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //测试实现Externalizable接口序列化
    private static void testSimpleExternalizable() {
        try {
            String fileName = ExternalizableImpl.class.getSimpleName();
            ExternalizableImpl instance = new ExternalizableImpl("tes external serizable", 13);

            //触发序列化
            SerializableUtils.saveObject(instance, fileName);
            System.out.println(fileName + " before serializable ::" + instance);

            Thread.sleep(5000);

            //触发反序列化
            ExternalizableImpl newInstance = SerializableUtils.readObject(fileName);
            System.out.println(fileName + " after serializable newInstance::" + newInstance);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //继承关系的序列化
    //1. 父类实现序列化，子类继承父类，则有无空参构造都行
    //2. 父类未实现序列化接口，子类需要实现序列化接口，并且需要子类和父类都提供空参构造
    //3. 父类未实现序列化接口,并且子类中有父类引用关系,若想实现序列化与反序列化,则父类必须实现序列化接口
    private static void testExtendSerializable(boolean isSuperImplSerializable) {
        try {
            if (isSuperImplSerializable) {
                String fileName = SerializablePersonChild.class.getSimpleName();
                SerializablePersonChild instance = new SerializablePersonChild("tes extend serizable", 13);
                instance.setGender("男");

                //触发序列化
                SerializableUtils.saveObject(instance, fileName);
                System.out.println(fileName + " before serializable ::" + instance);

                Thread.sleep(5000);

                //触发反序列化
                SerializablePersonChild newInstance = SerializableUtils.readObject(fileName);
                System.out.println(fileName + " after serializable newInstance::" + newInstance);
            } else {
                String fileName = UnSerializablePersonWithSerializableChild.class.getSimpleName();
                UnSerializablePersonWithSerializableChild instance = new UnSerializablePersonWithSerializableChild("tes extend un serizable", 13);
                instance.setGender("男");

                //触发序列化
                SerializableUtils.saveObject(instance, fileName);
                System.out.println(fileName + " before serializable ::" + instance);

                Thread.sleep(5000);

                //触发反序列化
                UnSerializablePersonWithSerializableChild newInstance = SerializableUtils.readObject(fileName);
                System.out.println(fileName + " after serializable newInstance::" + newInstance);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //测试组合关系序列化：一个要求，要序列化成员对象，对象必须实现序列化
    private static void testCombineSerializable() {
        try {
            String fileName = SerizableModel.class.getSimpleName();

            UnSerializablePerson unSerializablePerson=new UnSerializablePerson();
            unSerializablePerson.setAge(321);
            unSerializablePerson.setName("UnSerializablePerson");

            UnSerizableModel unSerizableModel=new UnSerizableModel("UnSerizableModel");

            SerizableModel instance = new SerizableModel(new SerializablePerson("test combine", 66));
            instance.setUnSerializablePersonWithEmptyCons(unSerializablePerson);
            instance.setUnSerizableModelWithNoEmptyCons(unSerizableModel);


            //触发序列化
            SerializableUtils.saveObject(instance, fileName);
            System.out.println(fileName + " before serializable ::" + instance);

            Thread.sleep(5000);

            //触发反序列化
            SerizableModel newInstance = SerializableUtils.readObject(fileName);
            System.out.println(fileName + " after serializable newInstance::" + newInstance);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
