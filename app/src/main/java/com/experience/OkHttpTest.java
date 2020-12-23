package com.experience;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Description:com.experience.OkHttpTest 责任链测试
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/23
 */
public class OkHttpTest {

    public static void main(String[] args) {

        String testUrl="https://www.baidu.com/";

        OkHttpClient client=new OkHttpClient.Builder().build();

        Request request=new Request.Builder()
                .url(testUrl)
                .get()
                .build();

        try {
            Response response=client.newCall(request).execute();
            assert response.body() != null;
            System.out.print("response==" + response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
