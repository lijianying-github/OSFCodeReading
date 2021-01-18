/*
 * Copyright (C) 2014 Square, Inc.
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
package okhttp3.internal;

import javax.annotation.Nullable;
import javax.net.ssl.SSLSocket;
import okhttp3.Address;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.RealConnectionPool;

/**
 * Escalate internal APIs in {@code okhttp3} so they can be used from OkHttp's implementation
 * packages. The only implementation of this interface is in {@link OkHttpClient}.
 *
 * 该抽象类是增强okhttp3相关操作的抽象类，目的是能够在OkHttp的相同包结构下相关类使用这个增强api
 *
 * 该接口的唯一实现是在OkHttpClient内部静态代码块的instance初始化。
 * 说白了就是一个辅助处理请求头，连接池的工具类，在OkhttpClient类加载的时候完成初始化
 * 在okhttp3包下面大家都可以通过Internal.instance去使用这个增强api
 */
public abstract class Internal {

  //测试Internal实现类
  public static void initializeInstanceForTests() {
    // Needed in tests to ensure that the instance is actually pointing to something.
    new OkHttpClient();
  }

  public static Internal instance;

  //给传入的Headers添加一行没有任何校验的请求头，不校验header的key-value
  public abstract void addLenient(Headers.Builder builder, String line);

  //给传入的Headers添加请求头
  public abstract void addLenient(Headers.Builder builder, String name, String value);

  //根据传入connectionPool获取真实的连接池
  public abstract RealConnectionPool realConnectionPool(ConnectionPool connectionPool);

  //判定地址a,b是否相等
  public abstract boolean equalsNonHost(Address a, Address b);

  //获取响应的响应码
  public abstract int code(Response.Builder responseBuilder);

  //修改tlsConfiguration
  public abstract void apply(ConnectionSpec tlsConfiguration, SSLSocket sslSocket,
      boolean isFallback);

  //根据OkHttpClient和request创建webSocket call
  public abstract Call newWebSocketCall(OkHttpClient client, Request request);

  //初始化exchange
  public abstract void initExchange(
      Response.Builder responseBuilder, Exchange exchange);

  //根据response获取转化对象
  public abstract @Nullable Exchange exchange(Response response);
}
