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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

import kotlin.Unit;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Streaming;

/**
 * 内置类型转换器（实现了Http response,requestbody,string类型转换）
 */
final class BuiltInConverters extends Converter.Factory {
    /**
     * Not volatile because we don't mind multiple threads discovering this.
     */
    private boolean checkForKotlinUnit = true;

    /**
     * ResponseBody装换成目标方法返回对象
     * 内置response只支持返回结果为Okhttp ResponseBody,void以及kotlin Unit其他返回null
     *
     * @param type        方法返回结果
     * @param annotations 方法注解集合
     * @param retrofit    retrofit
     * @return 转换器 只支持返回结果为Okhttp ResponseBody,void以及kotlin Unit其他返回null
     */
    @Override
    public @Nullable
    Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {

        //一般返回结果是okhttp默认的ResponseBody
        if (type == ResponseBody.class) {
            //判断字节流还是字符流
            return Utils.isAnnotationPresent(annotations, Streaming.class)
                    ? StreamingResponseBodyConverter.INSTANCE
                    : BufferingResponseBodyConverter.INSTANCE;
        }
        //void类型 返回null
        if (type == Void.class) {
            return VoidResponseBodyConverter.INSTANCE;
        }

        //Unit对象
        if (checkForKotlinUnit) {
            try {
                if (type == Unit.class) {
                    return UnitResponseBodyConverter.INSTANCE;
                }
            } catch (NoClassDefFoundError ignored) {
                checkForKotlinUnit = false;
            }
        }
        return null;
    }

  /**
   * 内置 body请求转换实现
   * @param type 只支持RequestBody以及子类
   * @param parameterAnnotations
   * @param methodAnnotations
   * @param retrofit
   * @return RequestBody
   */
    @Override
    public @Nullable
    Converter<?, RequestBody> requestBodyConverter(
            Type type,
            Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations,
            Retrofit retrofit) {
        if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
            return RequestBodyConverter.INSTANCE;
        }
        return null;
    }

    static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override
        public Void convert(ResponseBody value) {
            value.close();
            return null;
        }
    }

    static final class UnitResponseBodyConverter implements Converter<ResponseBody, Unit> {
        static final UnitResponseBodyConverter INSTANCE = new UnitResponseBodyConverter();

        @Override
        public Unit convert(ResponseBody value) {
            value.close();
            return Unit.INSTANCE;
        }
    }

    static final class RequestBodyConverter implements Converter<RequestBody, RequestBody> {
        static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

        @Override
        public RequestBody convert(RequestBody value) {
            return value;
        }
    }

    /**
     * 字节流数据转换器
     */
    static final class StreamingResponseBodyConverter
            implements Converter<ResponseBody, ResponseBody> {
        static final StreamingResponseBodyConverter INSTANCE = new StreamingResponseBodyConverter();

        @Override
        public ResponseBody convert(ResponseBody value) {
            return value;
        }
    }

    /**
     * 字符流数据转换器
     */
    static final class BufferingResponseBodyConverter
            implements Converter<ResponseBody, ResponseBody> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override
        public ResponseBody convert(ResponseBody value) throws IOException {
            try {
                // Buffer the entire body to avoid future I/O.
                //拷贝ResponseBody值防止其他IO操作的影响
                return Utils.buffer(value);
            } finally {
                value.close();
            }
        }
    }

    //内置toString Converter
    static final class ToStringConverter implements Converter<Object, String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override
        public String convert(Object value) {
            return value.toString();
        }
    }
}
