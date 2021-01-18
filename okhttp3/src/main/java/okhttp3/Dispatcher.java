/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.RealCall.AsyncCall;
import okhttp3.internal.Util;

/**
 * Policy on when async requests are executed.
 * 异步请求执行策略（不包含同步请求调用流程，只实现了同步请求的缓存和移除流程）
 *
 * <p>Each dispatcher uses an {@link ExecutorService} to run calls internally. If you supply your
 * own executor, it should be able to run {@linkplain #getMaxRequests the configured maximum} number
 * of calls concurrently.
 * 每个分发器都使用一个内部线程池去执行请求，若是使用自导那个一线程池，必须能够调用getMaxRequests方法去修改最大并发数
 */
public final class Dispatcher {
    //正在运行异步请求最大并发数
    private int maxRequests = 64;
    //单域名异步任务最大并发请求数
    private int maxRequestsPerHost = 5;

    //网络空闲回调
    private @Nullable
    Runnable idleCallback;

    /**
     * 请求执行线程池，懒加载创建
     */
    private @Nullable
    ExecutorService executorService;

    /**
     * 等待异步请求双端队列
     */
    private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>();

    /**
     * 执行中的异步请求双端队列（包含还没有结束的取消请求）
     */
    private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>();

    /**
     * 执行中的同步请求双端队列（包含还没有结束的取消请求）
     */
    private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();

    //支持自定义线程池分发器构建
    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    //默认线程池构建分发器
    public Dispatcher() {
    }

    public synchronized ExecutorService executorService() {
        if (executorService == null) {
            //默认线程池，最大核心线程数是int最大值，阻塞队列是不缓存的阻塞队列，线程空闲时长60s
            //默认拒绝策略是终止任务，AbortPolicy
            //使用效果每次提交一个任务，立马复用或者创建一个线程去执行任务。
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), Util.threadFactory("OkHttp Dispatcher", false));
        }
        return executorService;
    }

    /**
     * 修改线程池池执行请求的最大并发数，
     * 若修改的值小于当前正在运行请求列表任务数，则正在运行任务继续运行后在应用修改值
     */
    public void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        synchronized (this) {
            this.maxRequests = maxRequests;
        }
        //检查任务执行
        promoteAndExecute();
    }

    public synchronized int getMaxRequests() {
        return maxRequests;
    }

    /**
     * 设置每个主机并发执行的最大请求数。这将根据URL的主机名限制请求。
     * 注意，对单个IP地址的并发请求仍然可能超过此限制:多个主机名可能共享一个IP地址，或者通过相同的HTTP代理进行路由
     * <p>
     * 若修改的值小于当前正在运行请求列表任务数，则正在运行任务继续运行后在应用修改值
     *
     * <p>WebSocket 域名请求并发不受此约束限制.
     */
    public void setMaxRequestsPerHost(int maxRequestsPerHost) {
        if (maxRequestsPerHost < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
        }
        synchronized (this) {
            this.maxRequestsPerHost = maxRequestsPerHost;
        }
        promoteAndExecute();
    }

    public synchronized int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    /**
     * 设置空闲状态回调。该回调在分发器变成空闲状态（即：正在运行任务数为0）时被回调
     * <p>
     * 注意：一个网络请求是否被认为是空闲状态根据请求运行方式是不同的。
     * 异步请求在调用回调Callback.onResponse()或者Callback.onFailure()后会变成空闲状态。
     * 同步请求一旦在execute()方法有返回值就变成空闲状态。
     * 这意味着，如果您正在进行同步调用，那么网络层将不会真正处于空闲状态，直到所有返回的响应都已关闭才是真正的空闲状态。
     */
    public synchronized void setIdleCallback(@Nullable Runnable idleCallback) {
        this.idleCallback = idleCallback;
    }

    //提交异步网络请求
    void enqueue(AsyncCall call) {
        synchronized (this) {
            readyAsyncCalls.add(call);

            // Mutate the AsyncCall so that it shares the AtomicInteger of an existing running call to
            // the same host.
            if (!call.get().forWebSocket) {
                //同步统一请求主机域名共享请求次数AtomicInteger值
                AsyncCall existingCall = findExistingCallWithHost(call.host());
                if (existingCall != null) call.reuseCallsPerHostFrom(existingCall);
            }
        }
        //检查执行任务队列
        promoteAndExecute();
    }

    /**
     * runningAsyncCalls和readyAsyncCalls中查找相同域名的请求
     *
     * @param host 主机名
     * @return 请求
     */
    @Nullable
    private AsyncCall findExistingCallWithHost(String host) {
        for (AsyncCall existingCall : runningAsyncCalls) {
            if (existingCall.host().equals(host)) return existingCall;
        }
        for (AsyncCall existingCall : readyAsyncCalls) {
            if (existingCall.host().equals(host)) return existingCall;
        }
        return null;
    }

    /**
     * 取消所有异步和同步请求
     */
    public synchronized void cancelAll() {
        for (AsyncCall call : readyAsyncCalls) {
            call.get().cancel();
        }

        for (AsyncCall call : runningAsyncCalls) {
            call.get().cancel();
        }

        for (RealCall call : runningSyncCalls) {
            call.cancel();
        }
    }

    /**
     * 检查异步任务队列
     * <p>
     * 将readyAsyncCalls根据最大并发数限制添加到runningAsyncCalls里面，并且将新添加的请求任务提交线程池执行
     * 注意：此方法是执行具体请求任务，会回调用户代码，不能在synchronization同步代码中使用，原因是可能会导致死锁发生
     * 避免死锁导致的无法排定错误
     *
     * @return true 返回分发器是有任务正在运行
     * （若返回false,则后续调用idleHandler流程）
     */
    private boolean promoteAndExecute() {
        //检查当前方法是否加锁，若加锁则异常终止
        assert (!Thread.holdsLock(this));

        //从readyAsyncCalls集合中赛选的可以进入runningAsyncCalls集合的任务集合
        List<AsyncCall> executableCalls = new ArrayList<>();

        boolean isRunning;

        synchronized (this) {
            for (Iterator<AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
                AsyncCall asyncCall = i.next();

                //超出最大并发数，退出当前方法.返回false
                if (runningAsyncCalls.size() >= maxRequests) break;

                //当前等待请求域名执行是否次数超限，超限则过滤
                //请求次数为CAS原子类保存
                if (asyncCall.callsPerHost().get() >= maxRequestsPerHost)
                    continue; // Host max capacity.

                i.remove();
                //异步请求开始执行，对应主机请求次数+1
                asyncCall.callsPerHost().incrementAndGet();

                //异步任务从等待队列移动到运行队列
                executableCalls.add(asyncCall);
                runningAsyncCalls.add(asyncCall);
            }

            isRunning = runningCallsCount() > 0;
        }

        //提交新增运行任务到线程池执行
        for (int i = 0, size = executableCalls.size(); i < size; i++) {
            AsyncCall asyncCall = executableCalls.get(i);
            asyncCall.executeOn(executorService());
        }

        return isRunning;
    }

    /**
     * 提交同步请求（添加到同步请求队列）
     */
    synchronized void executed(RealCall call) {
        runningSyncCalls.add(call);
    }

    /**
     * 结束异步请求
     */
    void finished(AsyncCall call) {
        //请求对应域名请求共享次数-1
        call.callsPerHost().decrementAndGet();
        //runningAsyncCalls中移除请求并检查分发器空闲状态
        finished(runningAsyncCalls, call);
    }

    /**
     * 结束同步请求
     */
    void finished(RealCall call) {
        finished(runningSyncCalls, call);
    }

    /**
     * 移除任务集合中对应任务
     *
     * @param calls 任务集合
     * @param call  待移除任务
     */
    private <T> void finished(Deque<T> calls, T call) {
        Runnable idleCallback;
        synchronized (this) {
            //移除队列中任务不存在，不应该出现这种问题
            if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
            idleCallback = this.idleCallback;
        }

        //检查异步任务队列
        boolean isRunning = promoteAndExecute();

        //任务队列没有任务执行，执行空闲回调
        if (!isRunning && idleCallback != null) {
            idleCallback.run();
        }
    }

    /**
     * 获取当前readyAsyncCalls备份集合
     */
    public synchronized List<Call> queuedCalls() {
        List<Call> result = new ArrayList<>();
        for (AsyncCall asyncCall : readyAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取当前runningSyncCalls和runningAsyncCalls备份集合
     */
    public synchronized List<Call> runningCalls() {
        List<Call> result = new ArrayList<>();
        result.addAll(runningSyncCalls);
        for (AsyncCall asyncCall : runningAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取当前readyAsyncCalls集合任务数
     */
    public synchronized int queuedCallsCount() {
        return readyAsyncCalls.size();
    }

    /**
     * 获取当前正在运行任务个数
     *
     * @return 异步任务和同步任务个数
     */
    public synchronized int runningCallsCount() {
        return runningAsyncCalls.size() + runningSyncCalls.size();
    }
}
