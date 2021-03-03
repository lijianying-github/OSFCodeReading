package coroutines

import kotlinx.coroutines.*

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/3/3
 */
fun main() {
    blockCoroutines()
    globalCoroutines()
}

/**
 * 阻塞方式协程
 * 协程是依托于线程的，若依附线程提前执行完毕，协程内部延时的流程将不会执行
 *
 * runBlocking是阻塞式的返回栈为高阶实现的末行代码类型
 */
fun blockCoroutines() = runBlocking {
    GlobalScope.launch(Dispatchers.IO) {
        print("GlobalScope run ${Thread.currentThread().name}")
        Thread.sleep(2000)
        //if attach thread run finish ,the code will be not run
        print("GlobalScope run finish")
    }

    print("point1 run ${Thread.currentThread().name}")
    Thread.sleep(1000)
    print("method run finish===")
}

/**
 * 非阻塞式的
 */
fun globalCoroutines() {
    val job = GlobalScope.launch(Dispatchers.IO) {
        print("GlobalScope run ${Thread.currentThread().name}")
        for (i in 1..1000) {
            print("GlobalScope run $i")
        }
        Thread.sleep(3000)
        //if attach thread run finish ,the code will be not run
        print("GlobalScope run finish")
    }
    print("point1 run ${Thread.currentThread().name}")
    Thread.sleep(1000)
    print("method run finish===")
    //延时取消
    job.cancel()
    //立即取消
//    job.cancelAndJoin()
}

fun coroutinesAwait() {

    GlobalScope.launch(Dispatchers.Main) {
        //run in io
        val deffer = async(Dispatchers.IO) {
            print("async run ${Thread.currentThread().name}")
            Thread.sleep(3000)
            "success"
        }

        //run in main
        print("point1 run ${Thread.currentThread().name}")
        val result = deffer.await()
        print("async  run result==$result  ${Thread.currentThread().name}")
        print("method run finish===")
    }
}