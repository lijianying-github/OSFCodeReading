package com.practice.generic

import com.practice.generic.model.FuClass
import com.practice.generic.model.ZiClass

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/3/3
 */
fun main() {

    val fuClass=FuClass()
    val ziClass=ZiClass()

    val list= ArrayList<FuClass>()
    list.add(fuClass)
    list.add(ziClass)

    writeAbleList(list)
    readAbleList(list)


}

//in ZiClass == ? super ZiClass
fun writeAbleList(list:MutableList<in ZiClass>){
    val result1= list[0]
    val result2= list[1]

    println("result1 $result1  result2:$result2")

    list.add(ZiClass())
    list.add(FuClass() as ZiClass)
}

//out FuClass == ? extend FuClass
fun readAbleList(list:MutableList<out FuClass>){
    val result1= list[0]
    val result2= list[1]

    //add return nothing
//    list.add(ZiClass())
//    list.add(FuClass() as ZiClass)
}