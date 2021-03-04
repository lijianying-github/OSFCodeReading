package com.practice.kotlin

/**
 * Description:应用扩展函数包2,扩展函数传参为扩展函数
 *
 * <p>
 *
 * 手写系统扩展函数：
 * apply<=====>also
 * let<=====>run
 *
 * with就是一个普通函数，需要手动传入T，作用等价于run
 *
 *</p>
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/3/4
 */

//=============================扩展函数参数为扩展函数时情况=========================================//

//参数为扩展函数时，传入的是扩展函数——即扩展对象本身this,简单理解就是扩展函数就是this

//手写myApply:T类型进行this 无参返回扩展函数变换，返回是变换后的T
inline fun <T> T.myApply(block: T.() -> Unit): T {
    block()
    return this
}


// T.myApply() return T
fun testMyApply() {
    val name = StringBuilder()
    //result 类型为最后一个方法的返回值,目前最后一行是toString所以返回类型是String
    val result = name.myApply {
        append("1213123")
    }.myApply {
        append("3423423")
    }

    println("result value::$result")
    println("result type::${result.javaClass.typeName}")
}


//myApply:T类型进行扩展函数变换，函数内部持有源对象this，返回是变换后的T
inline fun <T> T.myAlso(block: (T) -> Unit): T {
    block(this)
    return this
}

//myAlso:T类型进行普通函数变换，函数内部传入源对象this，代号为it，返回是变换后的T
fun testMyAlso() {
    val str = StringBuilder()
    val result = str.myAlso {
        //必须手动添加it进行调用
        it.append("1213123")
    }.myAlso {
        it.append("dwewerwe")
    }//返回值还是str

    println("result type::${result.javaClass.typeName}")
    println("result value::$result")
}

//T.myLet  return method implement last line value
inline fun <T, R> T.myLet(block: (T) -> R): R {
    return block(this)
}


//myLet:T类型进行普通函数变换，函数内部传入源对象this，代号为it，返回值是变换函数的值
fun testMyLet() {
    val str = StringBuilder()
    val result = str.myLet {
        //必须手动添加it进行调用
        it.append("1213123")//type StringBuilder
    }.myLet {
        it.append("dwewerwe")
        it.toString()//type String
    }//返回值 String

    println("result type::${result.javaClass.typeName}")
    println("result value::$result")
}

//T.run 对T本身进行变换，返回值是变换后的值
inline  fun <T,R> T.myRun(block:T.()->R):R{
    return block()
}


//myRun:T类型进行扩展函数变换，函数内部传入源对象this，返回值是变换函数的值R
fun testMyRun() {
    val str = StringBuilder()
    val result = str.myRun {
        //必须手动添加it进行调用
        append("1213123")//type StringBuilder
    }.myRun {
        append("dwewerwe")
        toString()//type String
    }//返回值 String

    println("result type::${result.javaClass.typeName}")
    println("result value::$result")
}



