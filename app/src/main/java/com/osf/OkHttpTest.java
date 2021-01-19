package com.osf;

import java.io.IOException;

import anotation.Nemo;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Description:com.experience.com.osf.OkHttpTest 责任链测试
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2020/12/23
 */
public class OkHttpTest {

    @Nemo(value = "121")
    public static void main(String[] args) {

        String testUrl = "https://www.baidu.com/";

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url(testUrl)
                .get()
                .build();

        System.out.println("start okhttp sync call====================");
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            System.out.println("parse response success==================" );
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("finish okhttp sync call====================\n\n\n\n");

        System.out.println("start okhttp async call====================");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("finish okhttp async call failed====================");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("finish okhttp async call success====================");
                assert response.body() != null;
                System.out.println("parse response success==================" );
            }
        });


    }
}
