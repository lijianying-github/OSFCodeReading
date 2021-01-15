package com.practice.concurrent.chapter1;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Description:线程创建练习
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/15
 */
public class CreateThreadPractice {

    //Thread派生实现
    public static class ExtendThread  extends Thread {
        @Override
        public void run() {
            super.run();
            System.out.println("create thread by extend thread===");
        }
    }

    //Runnable 方式实现
    public static class RunnableThread implements Runnable {
        @Override
        public void run() {
            System.out.println("create thread by implements Runnable===");
        }
    }


    //Runnable派生FutureTask实现
    public static class CallableThread implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("create thread by implements Runnable wrapper Callable ===");
            return "Callable Runnable==";
        }
    }


    public static void main(String[] args) {

        System.out.println("Thread create  test===========");

        //线程创建方式1
        ExtendThread extendThread= new ExtendThread();
        extendThread.start();

        //线程创建方式2.1
        RunnableThread runnableThread= new RunnableThread();
        new Thread(runnableThread).start();

        //线程创建方式2.2
        CallableThread callableThread= new CallableThread();
        FutureTask<String> task= new FutureTask<>(callableThread);
        new Thread(task).start();

        System.out.println("Thread create  test end===========");
    }

}
