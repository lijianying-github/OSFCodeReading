package com.practice.kotlin

/**
 * Description:应用扩展函数包
 *
 * <注意参数为函数时的位置以及写法，参数写法>
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/3/4
 */

//=============================扩展函数参数为普通函数时情况=========================================//

//扩展所有类型增加json方法
//传参为高阶函数，则调用时内部单参数为it,多个需要显示指定
fun <T> T.json(block: (jsonStr: String) -> Unit) {
    if (this is Any) {
        //传入类型为对象,可以正常解析
        block(toString())
        return
    }
    //传入类型为
    if (this is Function<*>) {
        block("")
    }
}

fun  testJsonEx(){
    val name="test"
    name.json(block = {
        //单参数只有it
        println("name json model 1::$it")
    })

    //参数为函数时，可以写在（）外面
    name.json() {
        //单参数只有it
        println("name json model 2::$it")
    }

    //参数只有一个且为函数时，可以省略（）
    name.json {
        //单参数只有it
        println("name json model 3::$it")
    }

    //可以改名1
    name.json({
        jsonStr ->
        println("name json::$jsonStr")
    })

    //可以改名2
    name.json(){
        jsonStr ->
        println("name json::$jsonStr")
    }

    //可以改名3
    name.json{
        jsonStr ->
        println("name json::$jsonStr")
    }

}


//普通类型和函数混用的扩展函数
fun <T> T.json(errorMsg:String,block:(String)->Unit){
    if (this is Any) {
        //传入类型为对象,可以正常解析
        block(toString())
        return
    }
    //传入类型为
    if (this is Function<*>) {
        block(errorMsg)
    }
}

fun  testJsonEx2(){
    val name="test"
    val errorMsg="parse error"

    name.json(errorMsg,block = {
        //函数单参数只有it
        println("name json model 1::$it")
    })

    //最后一个参数为函数时，可以写在函数（）外面
    name.json(errorMsg) {
        //单参数只有it
        println("name json model 2::$it")
    }

    //可以改名1
    name.json(errorMsg,{
        jsonStr ->
        println("name json::$jsonStr")
    })

    //可以改名2
    name.json(errorMsg){
        jsonStr ->
        println("name json::$jsonStr")
    }
}

//为类型T扩展两个参数为1的方法
fun <T> T.addTwoFunction(func1:(T)->Unit,func2: (p:T) -> Unit){
    func1.invoke(this)
    func2(this)
}

fun testAddTwoFunction(){
    val testStr="1232"
    //多个扩展函数，或者函数参数最后一个不是函数变量时，不能写在外面
    testStr.addTwoFunction({ value ->
        println("fun 1 value=$value")
    }, {
        println("fun 2 value=$it")
    })
}


//扩展Pair类型增加析构值对方法
//传参为高阶函数，则调用时内部多参数时必须显示指定参数，此时不返回it,若不关心某个参数则写成_
fun <A,B> Pair<A,B>.toValue(otherInfo:String="",block: (first: A,second:B) -> Unit){
    println("otherInfo==$otherInfo")
    return block(this.first,this.second)
}

fun testPairToValue(){
    val pair=Pair("age",1)
    pair.toValue("test pair", { _, _ ->
        println("block1 run===")
    })

    pair.toValue("test pair") { _, _ ->
        println("block2 run===")
    }

    pair.toValue("test pair") { first, _ ->
        println("block3 run===$first")
    }
}
