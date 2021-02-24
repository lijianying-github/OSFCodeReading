package com.practice.inner_class;

/**
 * Description:成员内部类
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/24
 */
public class MemberInnerClass {

    private String outClassName;

    public static String TAG="MemberInnerClass";

    public MemberInnerClass(String outClassName) {
        this.outClassName = outClassName;
    }

    /**
     * 成员内部类（非静态内部类）：默认持有外部内实例的引用
     * 可以访问外部类的方法以及成员
     * 创建需要外部类实例.new xx()创建
     * 非静态成员内部类不能有静态成员和方法。
     */
    public class InnerClass{

        private String innerClassName;

//        成员非静态内部类不能有静态成员
//        private static int age=10;

        public InnerClass(String innerClassName) {
            this.innerClassName = innerClassName;
        }

        //        成员非静态内部类不能有静态方法
//        public static void show(){
//
//        }

        public void showInfo(){
            //非静态成员内部类可以访问外部类的方法和成员
//            showInnerInfo();
            System.out.println("InnerClass::===>outClassName::"+outClassName);
            System.out.println("InnerClass::===>innerClassName::"+innerClassName);
        }
    }


    /**
     * 静态内部类
     *不可以访问外部类的非静态方法以及成员
     *创建需要不需要外部类实例创建（类的加载上需要先加载外部类，可以起到延时加载的作用）
     *
     *静态内部类和一个标准的外部类相同，可以拥有静态成员和方法。
     */
    private static class InnerStaticClass{

        private String innerStaticClassName;
        public static String TAG="InnerStaticClass";

        public InnerStaticClass(String innerStaticClassName) {
            this.innerStaticClassName = innerStaticClassName;
        }

        public void showInfo(){
            //静态成员内部类不能访问外部类的非静态方法以及成员
//            System.out.println("InnerStaticClass::===>outClassName::"+outClassName);
            System.out.println("InnerStaticClass::===>innerStaticClassName::"+innerStaticClassName);
        }

        //        成员静态内部类可以有静态方法
        public static void showTag(){
            System.out.println("InnerStaticClass::===>showTag::"+TAG);
            showStaticInfo();
        }
    }

    public void  showInnerInfo(){
        InnerClass innerClass=new InnerClass("innerClass show");
        System.out.println("showInnerInfo::===>outClassName::"+outClassName);
        System.out.println("showInnerInfo::===>innerClassName::"+innerClass.innerClassName);
    }

    public void  showInnerStaticInfo(){
        InnerStaticClass innerClass=new InnerStaticClass("innerStaticClass show");
        System.out.println("showInnerStaticInfo::===>outClassName::"+outClassName);
        System.out.println("showInnerStaticInfo::===>innerClassName::"+innerClass.innerStaticClassName);
    }

    private static void showStaticInfo(){
        System.out.println("showStaticInfo::===>outClass::"+TAG);
    }

    public static void main(String[] args) {
        MemberInnerClass memberInnerClass=new MemberInnerClass("Outter");
        memberInnerClass.showInnerInfo();

        InnerClass innerClass=memberInnerClass.new InnerClass("innerClass");
        innerClass.showInfo();

        memberInnerClass.showInnerStaticInfo();
        InnerStaticClass innerStaticClass=new InnerStaticClass("innerStaticClass");
        innerStaticClass.showInfo();
    }

}
