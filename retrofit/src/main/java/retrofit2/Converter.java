/*
 * Copyright (C) 2012 Square, Inc.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Convert objects to and from their representation in HTTP. Instances are created by {@linkplain
 * Factory a factory} which is {@linkplain Retrofit.Builder#addConverterFactory(Factory) installed}
 * into the {@link Retrofit} instance.
 * 类型转换器接口定义（内置工厂具体实现创建
 * 需要实现3总类型转换：ResponseBody转xxx,xxx转RequestBody，以及xxx转sting
 */
public interface Converter<F, T> {

  /**
   * 将指定类型F转换成T
   * @param value 待转换值
   * @return 转换后值
   * @throws IOException
   */
    @Nullable
    T convert(F value) throws IOException;

    /**
     * Creates {@link Converter} instances based on a type and target usage.
     * 根据数据类型和目标使用场景构建类型转换器的工厂
     */
    abstract class Factory {

        /**
         * HTTP response body类型转换器（转换失败返回null）
         * 根据指定的返回type类型将HTTP response 转换成Type类型
         * 比如Type类型为SimpleResponse
         * 转换流程为：Http response：Call<SimpleResponse> 转换成SimpleResponse
         * <p>
         * Returns a {@link Converter} for converting an HTTP response body to {@code type}, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for
         * response types such as {@code SimpleResponse} from a {@code Call<SimpleResponse>}
         * declaration.
         */
        public @Nullable
        Converter<ResponseBody, ?> responseBodyConverter(
                Type type, Annotation[] annotations, Retrofit retrofit) {
            return null;
        }

        /**
         * HTTP request body类型转换器（转换失败返回null）
         * 目的是将给的类型type转换成RequestBody类型对象
         *
         * 主要适用于有带body请求的http类型比如@Body和@Part以及@PartMap这些
         *
         * Returns a {@link Converter} for converting {@code type} to an HTTP request body, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for types
         * specified by {@link Body @Body}, {@link Part @Part}, and {@link PartMap @PartMap} values.
         */
        public @Nullable
        Converter<?, RequestBody> requestBodyConverter(
                Type type,
                Annotation[] parameterAnnotations,
                Annotation[] methodAnnotations,
                Retrofit retrofit) {
            return null;
        }

        /**
         * 返回将给定type类型转换成string的类型转换器
         * 主要适用于url以及路径相关非body注解
         * Returns a {@link Converter} for converting {@code type} to a {@link String}, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for types
         * specified by {@link Field @Field}, {@link FieldMap @FieldMap} values, {@link Header @Header},
         * {@link HeaderMap @HeaderMap}, {@link Path @Path}, {@link Query @Query}, and {@link
         * QueryMap @QueryMap} values.
         */
        public @Nullable
        Converter<?, String> stringConverter(
                Type type, Annotation[] annotations, Retrofit retrofit) {
            return null;
        }

        /**
         * Extract the upper bound of the generic parameter at {@code index} from {@code type}. For
         * example, index 1 of {@code Map<String, ? extends Runnable>} returns {@code Runnable}.
         */
        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
            return Utils.getParameterUpperBound(index, type);
        }

        /**
         * Extract the raw class type from {@code type}. For example, the type representing {@code
         * List<? extends Runnable>} returns {@code List.class}.
         */
        protected static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }
    }
}
