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
        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }

    abstract @Nullable
    T invoke(Object[] args);
}
