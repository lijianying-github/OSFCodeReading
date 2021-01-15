package com.practice.concurrent.chapter7;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Description:线程池练习
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/15
 */
public class ThreadPoolPractice {

    /**
     * CPU密集型任务：最多线程数=cpu核心线程数+1(+1原因虚拟内存防止页缺失包装cpu一直使用)
     */
    private static final int TASK_TYPE_COMPUTE = 715;

    /**
     * IO型：最大线程数：2*cpu核心线程数，主要原因是IO阻塞
     * IO操作基于DMA中断，不会使用CPU
     */
    private static final int TASK_TYPE_IO = 402;

    /**
     * 混合型：IO和CPU任务混合：根据IO任务和CPU任务耗时数量级决定一个或者已某个任务为主
     * 都差不多可以拆分成2个线程池
     */
    private static final int TASK_MIX = 718;

    /**
     * 阻塞队列：设置要求：有界队列，否则会撑爆内存
     */
    private final BlockingQueue<Runnable> mBlockQueue = new LinkedBlockingQueue<>(100);

    private final int mProcessorSize = Runtime.getRuntime().availableProcessors();

    private final int mCorePoolSize = mProcessorSize / 2;

    private ThreadPoolExecutor mThreadPool;

    public void initPool(int taskType) {
        if (taskType == TASK_TYPE_COMPUTE) {
            mThreadPool = new ThreadPoolExecutor(
                    mCorePoolSize, mProcessorSize + 1, 20, TimeUnit.SECONDS, mBlockQueue);
        } else if (taskType == TASK_TYPE_IO) {
            mThreadPool = new ThreadPoolExecutor(
                    mCorePoolSize, mProcessorSize * 2, 20, TimeUnit.SECONDS, mBlockQueue);
        } else {
            mThreadPool = new ThreadPoolExecutor(
                    mCorePoolSize, mProcessorSize, 20, TimeUnit.SECONDS, mBlockQueue);
        }
    }

    public static void main(String[] args) {

        ThreadPoolPractice cpuPool = new ThreadPoolPractice();
        cpuPool.initPool(TASK_TYPE_COMPUTE);

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            cpuPool.mThreadPool.execute(() -> {
                System.out.println("add an task to ====" + finalI);
            });
        }


        ThreadPoolPractice ioPool = new ThreadPoolPractice();
        ioPool.initPool(TASK_TYPE_IO);

        ioPool.mThreadPool.execute(() -> {
            try {
                System.out.println("add io task=====");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        ThreadPoolPractice mixPool = new ThreadPoolPractice();
        mixPool.initPool(TASK_MIX);

        for (int i = 0; i < 20; i++) {
            mixPool.mThreadPool.execute(() -> {
                try {
                    System.out.println("add mix task=====");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

    }

}
