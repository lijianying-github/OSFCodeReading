package com.practice.concurrent.chapter7;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
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
    private final BlockingQueue<Runnable> mBlockQueue = new LinkedBlockingQueue<>(1);

    private final int mProcessorSize = Runtime.getRuntime().availableProcessors();

    private final int mCorePoolSize = mProcessorSize / 2;

    private ThreadPoolExecutor mThreadPool;

    //自定义拒绝策略，交给另外一个线程池去执行
    private BlockingQueue<Runnable> mBackupTaskBlockQueue;
    private ThreadPoolExecutor mBackupPool;
    private RejectedExecutionHandler mCustomHandler = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            if (mBackupPool == null) {
                mBackupTaskBlockQueue = new LinkedBlockingQueue<>(100);
                mBackupPool = new ThreadPoolExecutor(mCorePoolSize, mProcessorSize, 20, TimeUnit.SECONDS, mBackupTaskBlockQueue);
            }

            mBackupPool.execute(() -> {
                System.out.println("mBackupPool execute==");
                runnable.run();
            });
        }
    };

    public void initPool(int taskType) {
        if (taskType == TASK_TYPE_COMPUTE) {
            mThreadPool = new ThreadPoolExecutor(
                    //拒绝策略，无法处理提交任务时由任务提交者执行
                    mCorePoolSize, mProcessorSize + 1, 20, TimeUnit.SECONDS, mBlockQueue, new ThreadPoolExecutor.CallerRunsPolicy());
        } else if (taskType == TASK_TYPE_IO) {
            mThreadPool = new ThreadPoolExecutor(
                    //拒绝策略：无法处理提交任务时由丢弃任务
                    mCorePoolSize, mProcessorSize * 2, 20, TimeUnit.SECONDS, mBlockQueue, new ThreadPoolExecutor.DiscardPolicy());
        } else if (taskType == TASK_MIX) {
            mThreadPool = new ThreadPoolExecutor(
                    //拒绝策略：无法处理提交任务时由丢弃最旧任务
                    mCorePoolSize, mProcessorSize, 20, TimeUnit.SECONDS, mBlockQueue, new ThreadPoolExecutor.DiscardOldestPolicy());
        } else {
            mThreadPool = new ThreadPoolExecutor(
                    //拒绝策略：无法处理提交任务时抛出RejectedExecutionException异常
                    mCorePoolSize, mProcessorSize, 20, TimeUnit.SECONDS, mBlockQueue, new ThreadPoolExecutor.AbortPolicy());
        }
    }

    public void switchToCustomRejectHandler() {
        mThreadPool = new ThreadPoolExecutor(
                //拒绝策略：自定义，当前执行不了任务交给备用线程池去执行
                1, 1, 1, TimeUnit.SECONDS, mBlockQueue, mCustomHandler);
        mThreadPool.setRejectedExecutionHandler(mCustomHandler);
    }

    public static void main(String[] args) {

        ThreadPoolPractice testPool = new ThreadPoolPractice();
        testPool.initPool(TASK_TYPE_COMPUTE);

        for (int i = 0; i < 10; i++) {
            testPool.mThreadPool.execute(new RunTask("taskName" + i));
        }
        testPool.mThreadPool.shutdown();

        System.out.println("test custom reject policy==");
        testCustomRejectPolicy();
    }

    static void testCustomRejectPolicy() {

        ThreadPoolPractice testPool = new ThreadPoolPractice();
        testPool.switchToCustomRejectHandler();
        for (int i = 0; i < 10000; i++) {
            Future<String> task = testPool.mThreadPool.submit(new CallableTask());
            if (i == 0) {
                for (int j = 0; j < 10; j++) {
                    testPool.mThreadPool.execute(new RunTask("taskName" + j));
                }
            }

            try {
                System.out.println(task.get());
            } catch (InterruptedException | ExecutionException e) {
                testPool.mThreadPool.shutdown();
                e.printStackTrace();
            }
        }
    }

    static class RunTask implements Runnable {

        private String mTaskName;

        public RunTask(String mTaskName) {
            this.mTaskName = mTaskName;
        }

        @Override
        public void run() {
            System.out.println("execute run task::" + mTaskName);
        }
    }

    static class CallableTask implements Callable<String> {

        @Override
        public String call() throws Exception {
            long beginTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - beginTime < TimeUnit.SECONDS.toMillis(5)) {
                //DO haoshi operate
            }
            return "call task result::" + Thread.currentThread().getName();
        }
    }


}
