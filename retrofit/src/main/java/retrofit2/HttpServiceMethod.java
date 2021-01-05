/*
 * Copyright (C) 2015 Square, Inc.
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
package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

import kotlin.coroutines.Continuation;
import okhttp3.ResponseBody;

import static retrofit2.Utils.getRawType;
import static retrofit2.Utils.methodError;

/** Adapts an invocation of an interface method into an HTTP call.
 * 将接口方法适配成Http call
 * */
abstract class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {
  /**
   *检查接口方法的注解，通过注解构建HTTP代言
   */
  static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
      Retrofit retrofit, Method method, RequestFactory requestFactory) {

    //kotlin部分协程处理
    boolean isKotlinSuspendFunction = requestFactory.isKotlinSuspendFunction;
    boolean continuationWantsResponse = false;
    boolean continuationBodyNullable = false;

    Annotation[] annotations = method.getAnnotations();

    //http 请求返回数据类型
    Type adapterType;

    if (isKotlinSuspendFunction) {
      //kotlin部分协程解析
      Type[] parameterTypes = method.getGenericParameterTypes();
      //协程方法中，最后一个参数是Continuation<T> T是构建请求的返回类型也就是responseType

      //通过协程参数获取方法的响应数据类型
      Type responseType =
          Utils.getParameterLowerBound(
              0, (ParameterizedType) parameterTypes[parameterTypes.length - 1]);

      //指定泛型参数类型的响应数据类型：如Response<String>
      if (getRawType(responseType) == Response.class && responseType instanceof ParameterizedType) {
        // Unwrap the actual body type from Response<T>.
        //获取Response<T>中T的类型，解析response返回数据类型成功，协程可以处理响应数据
        responseType = Utils.getParameterUpperBound(0, (ParameterizedType) responseType);
        continuationWantsResponse = true;
      } else {
        //http responseType解析失败
        // TODO figure out if type is nullable or not
        // Metadata metadata = method.getDeclaringClass().getAnnotation(Metadata.class)
        // Find the entry for method
        // Determine if return type is nullable or not
      }

      //缓存当前协程Http call 以及response的参数化类型
      adapterType = new Utils.ParameterizedTypeImpl(null, Call.class, responseType);
      //检查方法注解中是否有SkipCallbackExecutor注解，没有则添加.
      //SkipCallbackExecutorImpl是注解SkipCallbackExecutor的实现类
      //这个注解保证协程回调能够直接跳过executor获取返回结果回调
      annotations = SkipCallbackExecutorImpl.ensurePresent(annotations);
    } else {
      //非kotlin方法，适配类型为方法的返回类型
      adapterType = method.getGenericReturnType();
    }

    //创建Http call适配器，主要是根据返回类型，注解去retrofit callAdapterFactories中查询
    CallAdapter<ResponseT, ReturnT> callAdapter =
        createCallAdapter(retrofit, method, adapterType, annotations);

    //获取适配器的responseType
    Type responseType = callAdapter.responseType();
    //方法返回响应类型最低也应该是okhttp的 ResponseBody或者是retrofit的Response<XXX>
    if (responseType == okhttp3.Response.class) {
      throw methodError(
          method,
          "'"
              + getRawType(responseType).getName()
              + "' is not a valid response body type. Did you mean ResponseBody?");
    }
    //必须指定返回Response泛型的具体类型也就是body的具体类型
    if (responseType == Response.class) {
      throw methodError(method, "Response must include generic type (e.g., Response<String>)");
    }
    // TODO support Unit for Kotlin?
    //HEAD请求返回数据类型不能是void，这个ServiceMethod外层调用这个方法前已判断限制
    if (requestFactory.httpMethod.equals("HEAD") && !Void.class.equals(responseType)) {
      throw methodError(method, "HEAD method must use Void as response type.");
    }

    //根据callAdapter中解析的响应数据类型，在retrofit converterFactories中查询对应的数据转换器
    Converter<ResponseBody, ResponseT> responseConverter =
        createResponseConverter(retrofit, method, responseType);

    okhttp3.Call.Factory callFactory = retrofit.callFactory;
    if (!isKotlinSuspendFunction) {
      //非kotlin 创建Call适配器
      return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
    } else if (continuationWantsResponse) {
      //创建协程Call adapter
      //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
      return (HttpServiceMethod<ResponseT, ReturnT>)
          new SuspendForResponse<>(
              requestFactory,
              callFactory,
              responseConverter,
              (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter);
    } else {
      //kotlin 协程返不关心返回数据类型 call适配器构造
      //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
      return (HttpServiceMethod<ResponseT, ReturnT>)
          new SuspendForBody<>(
              requestFactory,
              callFactory,
              responseConverter,
              (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter,
              continuationBodyNullable);
    }
  }

  //根据returnType，注解，在retrofit callAdapterFactories中查询对应的call适配器
  private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
      Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
    try {
      //noinspection unchecked
      return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
    } catch (RuntimeException e) { // Wide exception range because factories are user code.
      throw methodError(method, e, "Unable to create call adapter for %s", returnType);
    }
  }

  //根据responseType，注解，在retrofit converterFactories中查询对应的response body 转换器
  private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
      Retrofit retrofit, Method method, Type responseType) {
    Annotation[] annotations = method.getAnnotations();
    try {
      return retrofit.responseBodyConverter(responseType, annotations);
    } catch (RuntimeException e) { // Wide exception range because factories are user code.
      throw methodError(method, e, "Unable to create converter for %s", responseType);
    }
  }

  private final RequestFactory requestFactory;
  private final okhttp3.Call.Factory callFactory;
  private final Converter<ResponseBody, ResponseT> responseConverter;

  HttpServiceMethod(
      RequestFactory requestFactory,
      okhttp3.Call.Factory callFactory,
      Converter<ResponseBody, ResponseT> responseConverter) {
    this.requestFactory = requestFactory;
    this.callFactory = callFactory;
    this.responseConverter = responseConverter;
  }

  /**
   * retrofit方法调用时触发方法
   * @param args 方法参数
   * @return retrofit call OkHttpCall
   */
  @Override
  final @Nullable ReturnT invoke(Object[] args) {
    Call<ResponseT> call = new OkHttpCall<>(requestFactory, args, callFactory, responseConverter);
    return adapt(call, args);
  }

  /**
   * 对retrofit 最原始的OkHttpCall进行再次适配，比如适配成rx的observable call或者协程
   * @param call OkHttpCall
   * @param args 方法参数，目前只提供给协程使用
   * @return 再次适配的call
   */
  protected abstract @Nullable ReturnT adapt(Call<ResponseT> call, Object[] args);

  /**
   * 非协程retrofit 方法 call二次包装适配器
   * @param <ResponseT>
   * @param <ReturnT>
   */
  static final class CallAdapted<ResponseT, ReturnT> extends HttpServiceMethod<ResponseT, ReturnT> {
    private final CallAdapter<ResponseT, ReturnT> callAdapter;

    CallAdapted(
        RequestFactory requestFactory,
        okhttp3.Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter,
        CallAdapter<ResponseT, ReturnT> callAdapter) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
    }

    @Override
    protected ReturnT adapt(Call<ResponseT> call, Object[] args) {
      return callAdapter.adapt(call);
    }
  }

  static final class SuspendForResponse<ResponseT> extends HttpServiceMethod<ResponseT, Object> {
    private final CallAdapter<ResponseT, Call<ResponseT>> callAdapter;

    SuspendForResponse(
        RequestFactory requestFactory,
        okhttp3.Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter,
        CallAdapter<ResponseT, Call<ResponseT>> callAdapter) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
    }

    @Override
    protected Object adapt(Call<ResponseT> call, Object[] args) {
      call = callAdapter.adapt(call);

      //noinspection unchecked Checked by reflection inside RequestFactory.
      Continuation<Response<ResponseT>> continuation =
          (Continuation<Response<ResponseT>>) args[args.length - 1];

      // See SuspendForBody for explanation about this try/catch.
      try {
        return KotlinExtensions.awaitResponse(call, continuation);
      } catch (Exception e) {
        return KotlinExtensions.suspendAndThrow(e, continuation);
      }
    }
  }

  static final class SuspendForBody<ResponseT> extends HttpServiceMethod<ResponseT, Object> {
    private final CallAdapter<ResponseT, Call<ResponseT>> callAdapter;
    private final boolean isNullable;

    SuspendForBody(
        RequestFactory requestFactory,
        okhttp3.Call.Factory callFactory,
        Converter<ResponseBody, ResponseT> responseConverter,
        CallAdapter<ResponseT, Call<ResponseT>> callAdapter,
        boolean isNullable) {
      super(requestFactory, callFactory, responseConverter);
      this.callAdapter = callAdapter;
      this.isNullable = isNullable;
    }

    @Override
    protected Object adapt(Call<ResponseT> call, Object[] args) {
      call = callAdapter.adapt(call);

      //noinspection unchecked Checked by reflection inside RequestFactory.
      Continuation<ResponseT> continuation = (Continuation<ResponseT>) args[args.length - 1];

      // Calls to OkHttp Call.enqueue() like those inside await and awaitNullable can sometimes
      // invoke the supplied callback with an exception before the invoking stack frame can return.
      // Coroutines will intercept the subsequent invocation of the Continuation and throw the
      // exception synchronously. A Java Proxy cannot throw checked exceptions without them being
      // declared on the interface method. To avoid the synchronous checked exception being wrapped
      // in an UndeclaredThrowableException, it is intercepted and supplied to a helper which will
      // force suspension to occur so that it can be instead delivered to the continuation to
      // bypass this restriction.
      try {
        return isNullable
            ? KotlinExtensions.awaitNullable(call, continuation)
            : KotlinExtensions.await(call, continuation);
      } catch (Exception e) {
        return KotlinExtensions.suspendAndThrow(e, continuation);
      }
    }
  }
}
