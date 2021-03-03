package com.practice.generic;

import com.practice.generic.model.FuClass;
import com.practice.generic.model.ZiClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/3/3
 */
public class JavaGeneric<C> {

    public static void main(String[] args) {

        FuClass fuClass = new FuClass();
        ZiClass ziClass = new ZiClass();

        List<FuClass> fuClassList = new ArrayList<>();
        fuClassList.add(fuClass);

        List<ZiClass> ziClassList = new ArrayList<>();
        ziClassList.add(ziClass);

        JavaGeneric<String> javaGeneric = new JavaGeneric<>();

        javaGeneric.safeWriteList(fuClassList);
//        javaGeneric.safeWriteList(ziClassList);

        javaGeneric.safeReadList(fuClassList);
        javaGeneric.safeReadList(ziClassList);

        //泛型读写控制实现
        //? extends FuClass 指定传入结合泛型类的类型上界约束
        //此时只能在对该类型变量就行读操作，不能进行写操作
        List<? extends FuClass> readAbleList = new ArrayList<>(fuClassList);
        //不能进行安全的写入操作
//        readAbleList.add(fuClass);
//        readAbleList.add(ziClass);

        //可以进行安全的读操作
        FuClass read1 = readAbleList.get(0);
        FuClass read2 = readAbleList.get(1);

        //? super ZiClass 指定传入结合泛型类的类型上界约束
        //此时只能在对该类型变量就行读操作，不能进行写操作
        List<? super ZiClass> writeAbleList = new ArrayList<>(fuClassList);
        //不能进行安全的读操作，转换类型会失败
        Object result1 = writeAbleList.get(0);
        Object result2 = writeAbleList.get(1);


    }

    //泛型写控制实现
    //<? super FuClass>表示类型必须是ZiClass或者ZiClass的超类
    //此时只能在对该类型变量就行写操作，不能进行读操作
    //传入的list只能是
    public void safeWriteList(List<? super FuClass> list) {
        list.add(new ZiClass());
        list.add(new FuClass());

        //返回都是obj ,没有使用意义
        Object list0 = list.get(0);
        Object list1 = list.get(1);
    }

    //? extends FuClass 类型必须 FuClass 或者 FuClass 的子类，能够安全的类型转换成 FuClass
    //泛型读控制实现
    public  void safeReadList(List<? extends FuClass> list) {
        //可以安全的转换成FuClass进行方法调用，转换成子类会有失败的情况
        ZiClass ziClass = (ZiClass) list.get(0);
        FuClass lis1 = list.get(1);
        //不能进行泛型对象的写操作
//        list.add(new FuClass());
//        list.add(new ZiClass());
    }

    //普通泛型方法
    public <T> void genericMethod(T t) {
        System.out.println(t);
    }

    //静态泛型方法
    public static <R> R getResult(R r) {
        return r;
    }
}
