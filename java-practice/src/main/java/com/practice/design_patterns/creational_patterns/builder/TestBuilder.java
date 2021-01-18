package com.practice.design_patterns.creational_patterns.builder;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class TestBuilder {

    public static void main(String[] args) {
        NetClient netClient=new NetClient("www.baidu.com","GET");
        System.out.println("create netClient by new ::\n"+netClient.toString());


        NetClient netClientByBuilder=new NetClient.Builder()
                .setHasBody(true)
                .setName("test")
                .setUrl("www.baidu.com")
                .setVersion(1)
                .setRequestMethod("GET")
                .build();
        System.out.println("create netClient by builder ::\n"+netClientByBuilder.toString());
    }
}
