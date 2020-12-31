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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * 参数处理器：负责处理参数对应的注解比如：Path，Query等
 * <p>
 * 具体注解类型实现类缓存了当前注解的以及标注的参数信息
 * 当获取ParameterHandler集合进行迭代调用传入RequestBuilder时调用对应实现类的方法触发
 * RequestBuilder的setXXX方法最终完成参数注解解析到RequestBuilder的构建
 *
 * @param <T>
 */
abstract class ParameterHandler<T> {

    /**
     * 将参数值追加到RequestBuilder中的具体实现细节
     *
     * @param builder 请求构建Builder
     * @param value   注解参数传递实参
     * @throws IOException
     */
    abstract void apply(RequestBuilder builder, @Nullable T value) throws IOException;

    /**
     * 构建ParameterHandler类型迭代器（目标是对传入的具有迭代功能集合进行迭代取值赋值
     * 追加RequestBuilder中的值）
     *
     * @return
     */
    final ParameterHandler<Iterable<T>> iterable() {
        return new ParameterHandler<Iterable<T>>() {
            @Override
            void apply(RequestBuilder builder, @Nullable Iterable<T> values) throws IOException {
                if (values == null) return; // Skip null values.

                for (T value : values) {
                    ParameterHandler.this.apply(builder, value);
                }
            }
        };
    }

    final ParameterHandler<Object> array() {
        return new ParameterHandler<Object>() {
            @Override
            void apply(RequestBuilder builder, @Nullable Object values) throws IOException {
                if (values == null) return; // Skip null values.

                for (int i = 0, size = Array.getLength(values); i < size; i++) {
                    //noinspection unchecked
                    ParameterHandler.this.apply(builder, (T) Array.get(values, i));
                }
            }
        };
    }

    static final class RelativeUrl extends ParameterHandler<Object> {
        private final Method method;
        private final int p;

        RelativeUrl(Method method, int p) {
            this.method = method;
            this.p = p;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable Object value) {
            if (value == null) {
                throw Utils.parameterError(method, p, "@Url parameter is null.");
            }
            builder.setRelativeUrl(value);
        }
    }

    static final class Header<T> extends ParameterHandler<T> {
        private final String name;
        private final Converter<T, String> valueConverter;

        Header(String name, Converter<T, String> valueConverter) {
            this.name = Objects.requireNonNull(name, "name == null");
            this.valueConverter = valueConverter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) return; // Skip null values.

            String headerValue = valueConverter.convert(value);
            if (headerValue == null) return; // Skip converted but null values.

            builder.addHeader(name, headerValue);
        }
    }

    static final class Path<T> extends ParameterHandler<T> {
        //请求方法
        private final Method method;
        //参数位置
        private final int p;
        //path注解的name   name 非空
        private final String name;
        //path注解参数的类型转换器，最终转换成String
        private final Converter<T, String> valueConverter;
        //参数值是否需要进行Url encode编码处理
        private final boolean encoded;

        Path(Method method, int p, String name, Converter<T, String> valueConverter, boolean encoded) {
            this.method = method;
            this.p = p;
            this.name = Objects.requireNonNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) {
                throw Utils.parameterError(
                        method, p, "Path parameter \"" + name + "\" value must not be null.");
            }
            builder.addPathParam(name, valueConverter.convert(value), encoded);
        }
    }

    static final class Query<T> extends ParameterHandler<T> {
        private final String name;
        private final Converter<T, String> valueConverter;
        private final boolean encoded;

        Query(String name, Converter<T, String> valueConverter, boolean encoded) {
            this.name = Objects.requireNonNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }


        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            //Query注解的参数值若注解中value为空或者参数实参为空则过滤
            if (value == null) return; // Skip null values.

            String queryValue = valueConverter.convert(value);
            if (queryValue == null) return; // Skip converted but null values

            builder.addQueryParam(name, queryValue, encoded);
        }
    }

    static final class QueryName<T> extends ParameterHandler<T> {
        private final Converter<T, String> nameConverter;
        private final boolean encoded;

        QueryName(Converter<T, String> nameConverter, boolean encoded) {
            this.nameConverter = nameConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) return; // Skip null values.
            builder.addQueryParam(nameConverter.convert(value), null, encoded);
        }
    }

    static final class QueryMap<T> extends ParameterHandler<Map<String, T>> {
        private final Method method;
        private final int p;
        private final Converter<T, String> valueConverter;
        private final boolean encoded;

        QueryMap(Method method, int p, Converter<T, String> valueConverter, boolean encoded) {
            this.method = method;
            this.p = p;
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable Map<String, T> value) throws IOException {
            //QueryMap参数注解实参不能为空，key,value不能为空以及value通过转化器转换后不能为空
            if (value == null) {
                throw Utils.parameterError(method, p, "Query map was null");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey == null) {
                    throw Utils.parameterError(method, p, "Query map contained null key.");
                }
                T entryValue = entry.getValue();
                if (entryValue == null) {
                    throw Utils.parameterError(
                            method, p, "Query map contained null value for key '" + entryKey + "'.");
                }

                String convertedEntryValue = valueConverter.convert(entryValue);
                if (convertedEntryValue == null) {
                    throw Utils.parameterError(
                            method,
                            p,
                            "Query map value '"
                                    + entryValue
                                    + "' converted to null by "
                                    + valueConverter.getClass().getName()
                                    + " for key '"
                                    + entryKey
                                    + "'.");
                }

                //map遍历添加Query参数
                builder.addQueryParam(entryKey, convertedEntryValue, encoded);
            }
        }
    }

    static final class HeaderMap<T> extends ParameterHandler<Map<String, T>> {
        private final Method method;
        private final int p;
        private final Converter<T, String> valueConverter;

        HeaderMap(Method method, int p, Converter<T, String> valueConverter) {
            this.method = method;
            this.p = p;
            this.valueConverter = valueConverter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable Map<String, T> value) throws IOException {
            //HeaderMap参数注解实参不能为空，key,value不能为空以及value通过转化器转换后不能为空
            if (value == null) {
                throw Utils.parameterError(method, p, "Header map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String headerName = entry.getKey();
                if (headerName == null) {
                    throw Utils.parameterError(method, p, "Header map contained null key.");
                }
                T headerValue = entry.getValue();
                if (headerValue == null) {
                    throw Utils.parameterError(
                            method, p, "Header map contained null value for key '" + headerName + "'.");
                }
                builder.addHeader(headerName, valueConverter.convert(headerValue));
            }
        }
    }

    static final class Headers extends ParameterHandler<okhttp3.Headers> {
        private final Method method;
        private final int p;

        Headers(Method method, int p) {
            this.method = method;
            this.p = p;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable okhttp3.Headers headers) {
            if (headers == null) {
                throw Utils.parameterError(method, p, "Headers parameter must not be null.");
            }
            builder.addHeaders(headers);
        }
    }

    static final class Field<T> extends ParameterHandler<T> {
        private final String name;
        private final Converter<T, String> valueConverter;
        private final boolean encoded;

        Field(String name, Converter<T, String> valueConverter, boolean encoded) {
            this.name = Objects.requireNonNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) return; // Skip null values.

            String fieldValue = valueConverter.convert(value);
            if (fieldValue == null) return; // Skip null converted values

            builder.addFormField(name, fieldValue, encoded);
        }
    }

    static final class FieldMap<T> extends ParameterHandler<Map<String, T>> {
        private final Method method;
        private final int p;
        private final Converter<T, String> valueConverter;
        private final boolean encoded;

        FieldMap(Method method, int p, Converter<T, String> valueConverter, boolean encoded) {
            this.method = method;
            this.p = p;
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable Map<String, T> value) throws IOException {

            if (value == null) {
                throw Utils.parameterError(method, p, "Field map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey == null) {
                    throw Utils.parameterError(method, p, "Field map contained null key.");
                }
                T entryValue = entry.getValue();
                if (entryValue == null) {
                    throw Utils.parameterError(
                            method, p, "Field map contained null value for key '" + entryKey + "'.");
                }

                String fieldEntry = valueConverter.convert(entryValue);
                if (fieldEntry == null) {
                    throw Utils.parameterError(
                            method,
                            p,
                            "Field map value '"
                                    + entryValue
                                    + "' converted to null by "
                                    + valueConverter.getClass().getName()
                                    + " for key '"
                                    + entryKey
                                    + "'.");
                }

                builder.addFormField(entryKey, fieldEntry, encoded);
            }
        }
    }

    static final class Part<T> extends ParameterHandler<T> {
        private final Method method;
        private final int p;
        private final okhttp3.Headers headers;
        private final Converter<T, RequestBody> converter;

        Part(Method method, int p, okhttp3.Headers headers, Converter<T, RequestBody> converter) {
            this.method = method;
            this.p = p;
            this.headers = headers;
            this.converter = converter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) {
            if (value == null) return; // Skip null values.

            RequestBody body;
            try {
                body = converter.convert(value);
            } catch (IOException e) {
                throw Utils.parameterError(method, p, "Unable to convert " + value + " to RequestBody", e);
            }
            builder.addPart(headers, body);
        }
    }

    static final class RawPart extends ParameterHandler<MultipartBody.Part> {
        static final RawPart INSTANCE = new RawPart();

        private RawPart() {
        }

        @Override
        void apply(RequestBuilder builder, @Nullable MultipartBody.Part value) {
            if (value != null) { // Skip null values.
                builder.addPart(value);
            }
        }
    }

    static final class PartMap<T> extends ParameterHandler<Map<String, T>> {
        private final Method method;
        private final int p;
        private final Converter<T, RequestBody> valueConverter;
        private final String transferEncoding;

        PartMap(
                Method method, int p, Converter<T, RequestBody> valueConverter, String transferEncoding) {
            this.method = method;
            this.p = p;
            this.valueConverter = valueConverter;
            this.transferEncoding = transferEncoding;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable Map<String, T> value) throws IOException {
            if (value == null) {
                throw Utils.parameterError(method, p, "Part map was null.");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String entryKey = entry.getKey();
                if (entryKey == null) {
                    throw Utils.parameterError(method, p, "Part map contained null key.");
                }
                T entryValue = entry.getValue();
                if (entryValue == null) {
                    throw Utils.parameterError(
                            method, p, "Part map contained null value for key '" + entryKey + "'.");
                }

                okhttp3.Headers headers =
                        okhttp3.Headers.of(
                                "Content-Disposition",
                                "form-data; name=\"" + entryKey + "\"",
                                "Content-Transfer-Encoding",
                                transferEncoding);

                builder.addPart(headers, valueConverter.convert(entryValue));
            }
        }
    }

    static final class Body<T> extends ParameterHandler<T> {
        private final Method method;
        private final int p;
        private final Converter<T, RequestBody> converter;

        Body(Method method, int p, Converter<T, RequestBody> converter) {
            this.method = method;
            this.p = p;
            this.converter = converter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) {
            if (value == null) {
                throw Utils.parameterError(method, p, "Body parameter value must not be null.");
            }
            RequestBody body;
            try {
                body = converter.convert(value);
            } catch (IOException e) {
                throw Utils.parameterError(method, e, p, "Unable to convert " + value + " to RequestBody");
            }
            builder.setBody(body);
        }
    }

    static final class Tag<T> extends ParameterHandler<T> {
        final Class<T> cls;

        Tag(Class<T> cls) {
            this.cls = cls;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) {
            builder.addTag(cls, value);
        }
    }
}
