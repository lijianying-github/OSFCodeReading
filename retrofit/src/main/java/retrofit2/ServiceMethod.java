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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

import static retrofit2.Utils.methodError;

abstract class ServiceMethod<T> {
    static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
        //根据方法解析请求部分，封装请求参数
        RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

        //获取方法返回的泛型类型
        Type returnType = method.getGenericReturnType();
        //若返回的泛型类型是泛型或者通配符则无法确定类型，异常处理
        if (Utils.hasUnresolvableType(returnType)) {
            throw methodError(
                    method,
                    "Method return type must not include a type variable or wildcard: %s",
                    returnType);
        }
        //retrofit 接口返回值不能直接是void
        if (returnType == void.class) {
            throw methodError(method, "Service methods cannot return void.");
        }

        //根据方法以及请求解析response部分，封装response数据转换 并最终封装生成一个call
        //HttpServiceMethod是ServiceMethod的子类实现了invoke接口
        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }

    /**
     * 调用retrofit方法获取call对象
     * 调用默认实现：HttpServiceMethod.invoke()
     * 流程如下：
     * 1. 先构建一个retrofit包装的okhttp call代理类实现OkHttpCall
     * 2. 根据方法的返回类型去callAdapterFactories中查找对应的CallAdapter(找不到直接抛出异常程序终止)
     * 3. 调用CallAdapter的adapt方法将OkHttpCall包装成方法返回类型的call 比如rx call
     * 4. 返回包装的call
     * <p>
     * 不同的包装call的回调处理流程不一样
     * 默认的DefaultCallAdapterFactory是OkHttpCall+主线程回调handler实现
     * rx 是RxJava3CallAdapter+OkHttpCall实现，回调是rx链式实现
     *
     * @param args 方法参数值
     * @return 包装的Call
     */
    //获取call->HttpServiceMethod.invoke()->根据方法解析返回类型获取call
    abstract @Nullable
    T invoke(Object[] args);
}
