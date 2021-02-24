package com.practice.inner_class;

/**
 * Description:方法内部类(包括普通方法内部类（有class）和匿名内部类（没有class的声明）)
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/2/24
 */
public class MethodInnerClass {

    private String name;

    public MethodInnerClass(String name) {
        this.name = name;
    }

    /***
     * 显示方法内部类
     * @param index 方法形参，有方法内部类是jdk1.8会设置成final类型，内部类无法修改其值
     */
    public void showMethodInnerClass(int index) {

        //方法内部类不能有访问权限修饰符，可以当作方法内部的一个局部变量
        //类的可见性只局限在声明的方法内部，外界无法感知该类的存在以及访问该类
        //方法内部类可以访问外部类的所有方法和属性
        class InnerClass {

            private String innerClassName;

            public InnerClass(String innerClassName) {
                this.innerClassName = innerClassName;
            }

            public void showInner() {
                //约定方法内部内的方法形参必须是final类型，在内部类中无法修改其传递的形参值，需要创建临时变量访问
                //index++;

                //创建临时变量访问
                int result = index;
                result++;

                System.out.println("MethodInnerClass::===>innerClassName::" + innerClassName);
                System.out.println("MethodInnerClass::===>outName::" + name);
                System.out.println("MethodInnerClass::===>result::" + result);
                showStaticTag();
                showTag();
            }
        }

        InnerClass innerClass = new InnerClass("InnerClass");
        innerClass.showInner();
    }

    /***
     * 显示方法匿名内部类
     * @param index 方法形参，有方法内部类是jdk1.8会设置成final类型，内部类无法修改其值
     */
    public void  showAnonymousInnerClass(int index){

        //匿名内部类不能有访问权限修饰符，可以当作方法内部的一个局部变量
        //没有class的声明可以依托于接口或者抽象类进行创建。
        //匿名内部类持有外部类的引用，可以访问外部类的所有方法和属性
        AnonymousInnerClassInterface anonymousInnerClass=new AnonymousInnerClassInterface() {
            @Override
            public void showFunction(String message) {
//                index++;

                //创建临时变量访问
                int result = index;
                result++;

                System.out.println("MethodInnerClass::===>outName::" + name);
                System.out.println("MethodInnerClass::===>result::" + result);
                showStaticTag();
                showTag();
            }
        };
    }

    public static void showStaticTag() {
        System.out.println("MethodInnerClass::===>showStaticTag");
    }

    public void showTag() {
        System.out.println("MethodInnerClass::===>showTag");
    }


    public static void main(String[] args) {
        MethodInnerClass memberInnerClass = new MethodInnerClass("MemberInnerClass");
        memberInnerClass.showMethodInnerClass(1);
    }
}

