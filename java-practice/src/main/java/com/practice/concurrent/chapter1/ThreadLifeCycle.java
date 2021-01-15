package com.practice.concurrent.chapter1;

/**
 * Description:线程生命周期
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/15
 */
public class ThreadLifeCycle {

    static class ThreadA extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                //STATE:Running
                for (int i = 0; i < 6; i++) {
                    System.out.println("Thread A STATE:Running.....");
                    //让出cpu执行权，资源不释放 STATE:READY
                    if (i == 3) {
                        System.out.println("Thread A yield STATE:READY .....");
                        Thread.yield();
                    }
                }
                interrupt();
            }
            System.out.println("Thread A is DEAD .....");
        }
    }

    static class ThreadB extends Thread {
        @Override
        public void run() {
            super.run();
            //State TIMED_WAIT
            try {
                System.out.println("Thread B STATE:Running.....");
                System.out.println("Thread B is TIMED_WAIT sleep 1s.....");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Thread B is DEAD .....");
        }
    }

    public static void main(String[] args) {
        //New state
        System.out.println("Thread A is Create STATE:NEW .....");
        Thread threadA = new ThreadA();

        System.out.println("Thread B is Create STATE:NEW .....");
        ThreadB threadB = new ThreadB();

        //Running
        threadA.start();
        threadB.start();

    }

}
