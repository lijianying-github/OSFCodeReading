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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import kotlin.coroutines.Continuation;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.Multipart;
import retrofit2.http.OPTIONS;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.QueryName;
import retrofit2.http.Tag;
import retrofit2.http.Url;

import static retrofit2.Utils.methodError;
import static retrofit2.Utils.parameterError;

/**
 * RequestFactory根据传入的retrofit方法生成一个可以发起请求的对象
 * 包含请求方法，头，路径，以及参数封装实现
 */
final class RequestFactory {
    static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
        return new Builder(retrofit, method).build();
    }

    //RequestFactory
    private final Method method;
    private final HttpUrl baseUrl;
    final String httpMethod;
    private final @Nullable
    String relativeUrl;
    private final @Nullable
    Headers headers;
    private final @Nullable
    MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
    private final ParameterHandler<?>[] parameterHandlers;
    final boolean isKotlinSuspendFunction;

    RequestFactory(Builder builder) {
        method = builder.method;
        baseUrl = builder.retrofit.baseUrl;
        httpMethod = builder.httpMethod;
        relativeUrl = builder.relativeUrl;
        headers = builder.headers;
        contentType = builder.contentType;
        hasBody = builder.hasBody;
        isFormEncoded = builder.isFormEncoded;
        isMultipart = builder.isMultipart;
        parameterHandlers = builder.parameterHandlers;
        isKotlinSuspendFunction = builder.isKotlinSuspendFunction;
    }

    okhttp3.Request create(Object[] args) throws IOException {
        @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
                ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

        int argumentCount = args.length;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException(
                    "Argument count ("
                            + argumentCount
                            + ") doesn't match expected count ("
                            + handlers.length
                            + ")");
        }

        RequestBuilder requestBuilder =
                new RequestBuilder(
                        httpMethod,
                        baseUrl,
                        relativeUrl,
                        headers,
                        contentType,
                        hasBody,
                        isFormEncoded,
                        isMultipart);

        if (isKotlinSuspendFunction) {
            // The Continuation is the last parameter and the handlers array contains null at that index.
            argumentCount--;
        }

        List<Object> argumentList = new ArrayList<>(argumentCount);
        for (int p = 0; p < argumentCount; p++) {
            argumentList.add(args[p]);
            handlers[p].apply(requestBuilder, args[p]);
        }

        return requestBuilder.get().tag(Invocation.class, new Invocation(method, argumentList)).build();
    }

    /**
     * Inspects the annotations on an interface method to construct a reusable service method. This
     * requires potentially-expensive reflection so it is best to build each service method only once
     * and reuse it. Builders cannot be reused.
     */
    static final class Builder {
        // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
        private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
        private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
        private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

        final Retrofit retrofit;
        final Method method;

        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        boolean gotField;
        boolean gotPart;
        boolean gotBody;
        boolean gotPath;
        boolean gotQuery;
        boolean gotQueryName;
        boolean gotQueryMap;
        boolean gotUrl;
        @Nullable
        String httpMethod;
        boolean hasBody;
        boolean isFormEncoded;
        boolean isMultipart;
        @Nullable
        String relativeUrl;
        @Nullable
        Headers headers;
        @Nullable
        MediaType contentType;
        @Nullable
        Set<String> relativeUrlParamNames;
        @Nullable
        ParameterHandler<?>[] parameterHandlers;
        boolean isKotlinSuspendFunction;

        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            //获取方法注解
            this.methodAnnotations = method.getAnnotations();
            //获取方法参数类型，包括泛型比如T，若是调用getParameterTypes对与泛型方法中T则返回为Object
            this.parameterTypes = method.getGenericParameterTypes();
            //获取所有参数的注解
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }

        RequestFactory build() {
            for (Annotation annotation : methodAnnotations) {
                //解析方法注解：请求方法和路径信息以及header相关
                parseMethodAnnotation(annotation);
            }

            //方法必须包含请求方法注解
            if (httpMethod == null) {
                throw methodError(method, "HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }

            //Multipart和FormUrlEncoded必须用在有Body的请求方法上
            //对于自定义Http注解hasBody为false和非（POST,PATCH,PUT）
            // 请求方法不能和Multipart和FormUrlEncoded一起使用
            //并且Multipart和FormUrlEncoded不能一起使用只能二选一
            if (!hasBody) {
                if (isMultipart) {
                    throw methodError(
                            method,
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw methodError(
                            method,
                            "FormUrlEncoded can only be specified on HTTP methods with "
                                    + "request body (e.g., @POST).");
                }
            }

            //解析方法参数注解以及类型.注解数组是二维数组
            int parameterCount = parameterAnnotationsArray.length;

            parameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0, lastParameter = parameterCount - 1; p < parameterCount; p++) {
                parameterHandlers[p] =
                        parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p], p == lastParameter);
            }

//            没有url参数
            if (relativeUrl == null && !gotUrl) {
                throw methodError(method, "Missing either @%s URL or @Url parameter.", httpMethod);
            }

//            body校验
            if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
                throw methodError(method, "Non-body HTTP method cannot contain @Body.");
            }
//            Form-encoded注解必须有Field参数注解
            if (isFormEncoded && !gotField) {
                throw methodError(method, "Form-encoded method must contain at least one @Field.");
            }
//            Multipart方法注解必须有@Part参数注解
            if (isMultipart && !gotPart) {
                throw methodError(method, "Multipart method must contain at least one @Part.");
            }

            //将注解解析出来的参数构建RequestFactory对象
            return new RequestFactory(this);
        }

        //解析方法注解：主要解析http方法注解，header注解以及Multipart、FormUrlEncoded注解
        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof DELETE) {
                parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
            } else if (annotation instanceof GET) {
                parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
            } else if (annotation instanceof HEAD) {
                parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
            } else if (annotation instanceof PATCH) {
                parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
            } else if (annotation instanceof POST) {
                parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
            } else if (annotation instanceof PUT) {
                parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
            } else if (annotation instanceof OPTIONS) {
                parseHttpMethodAndPath("OPTIONS", ((OPTIONS) annotation).value(), false);
            } else if (annotation instanceof HTTP) {
                //解析自定义请求方法
                HTTP http = (HTTP) annotation;
                parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
            } else if (annotation instanceof retrofit2.http.Headers) {
                //解析头：结构“key:value”的数组
                String[] headersToParse = ((retrofit2.http.Headers) annotation).value();
                //添加了Headers注解必须有值，否则运行失败
                if (headersToParse.length == 0) {
                    throw methodError(method, "@Headers annotation is empty.");
                }
                headers = parseHeaders(headersToParse);
            } else if (annotation instanceof Multipart) {
                //Multipart和FormUrlEncoded只能二选一
                if (isFormEncoded) {
                    throw methodError(method, "Only one encoding annotation is allowed.");
                }
                isMultipart = true;
            } else if (annotation instanceof FormUrlEncoded) {
                //Multipart和FormUrlEncoded只能二选一
                if (isMultipart) {
                    throw methodError(method, "Only one encoding annotation is allowed.");
                }
                isFormEncoded = true;
            }
        }

        /**
         * 解析http请求方法注解
         *
         * @param httpMethod 请求方法
         * @param value      请求路径
         * @param hasBody    是否有body体 POST,PUT,PATCH这个三个方法有body
         */
        private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
            //一个method只能有一个请求方法
            if (this.httpMethod != null) {
                throw methodError(
                        method,
                        "Only one HTTP method is allowed. Found: %s and %s.",
                        this.httpMethod,
                        httpMethod);
            }
            this.httpMethod = httpMethod;
            this.hasBody = hasBody;

            //解析路径信息
            if (value.isEmpty()) {
                return;
            }

            // 获取相对路径中query类型即?前面的字符串
            int question = value.indexOf('?');
            if (question != -1 && question < value.length() - 1) {
                // 保证?后面查询字符串必须是具体的查询语句不能有占位符比如
                //@GET("/getIpInfo.php?ip={ip}")
                //若？后面有占位符则提示用户用标准的@Query注解
                String queryParams = value.substring(question + 1);
                Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
                if (queryParamMatcher.find()) {
                    throw methodError(
                            method,
                            "URL query string \"%s\" must not have replace block. "
                                    + "For dynamic query parameters use @Query.",
                            queryParams);
                }
            }

            //赋值相对路径
            this.relativeUrl = value;
            //解析路径中path参数名称
            this.relativeUrlParamNames = parsePathParameters(value);
        }

        /**
         * 解析Header注解
         *
         * @param headers {“key:value”,“key:value”}格式
         * @return Headers对象
         */
        private Headers parseHeaders(String[] headers) {
            Headers.Builder builder = new Headers.Builder();
            for (String header : headers) {
                //格式校验：必须是Name: Value格式
                int colon = header.indexOf(':');
                if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                    throw methodError(
                            method, "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
                }
                //获取headerName和headerValue
                String headerName = header.substring(0, colon);
                String headerValue = header.substring(colon + 1).trim();

                if ("Content-Type".equalsIgnoreCase(headerName)) {
                    try {
                        //Content-Type是http请求的标准的标注位，所以value必须是标准协议允许下的命名方式，
                        //若不是，则无法发起http请求，协议不允许
                        contentType = MediaType.get(headerValue);
                    } catch (IllegalArgumentException e) {
                        //非法Content-Type value赋值
                        throw methodError(method, e, "Malformed content type: %s", headerValue);
                    }
                } else {
                    //headerName内部校验规则:非空非中文字符串
                    //headerValue内部校验规则:非null 非中文字符串
                    //"\u0020"是空格，
                    builder.add(headerName, headerValue);
                }
            }
            //Headers类内部就是一个String数组，key-value-key-value的形式，偶数位为name，奇数位为value
            //Headers类内部添加了size,name，value获取的方法就是基于这个原理
            return builder.build();
        }

        /**
         * 参数多注解解析
         *
         * @param p                 注解位置
         * @param parameterType     参数类型
         * @param annotations       参数注解集合
         * @param allowContinuation 允许继续
         * @return 解析结果
         */
        private @Nullable
        ParameterHandler<?> parseParameter(
                int p, Type parameterType, @Nullable Annotation[] annotations, boolean allowContinuation) {
            ParameterHandler<?> result = null;
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    //解析指定类型参数的注解
                    ParameterHandler<?> annotationAction =
                            parseParameterAnnotation(p, parameterType, annotations, annotation);

                    if (annotationAction == null) {
                        continue;
                    }

                    if (result != null) {
                        //多个retrofit注解会有多个result
                        throw parameterError(
                                method, p, "Multiple Retrofit annotations found, only one allowed.");
                    }

                    result = annotationAction;
                }
            }

            if (result == null) {
                if (allowContinuation) {
                    try {
                        if (Utils.getRawType(parameterType) == Continuation.class) {
                            isKotlinSuspendFunction = true;
                            return null;
                        }
                    } catch (NoClassDefFoundError ignored) {
                    }
                }
                throw parameterError(method, p, "No Retrofit annotation found.");
            }

            return result;
        }

        @Nullable
        private ParameterHandler<?> parseParameterAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Url) {
                //校验参数类型：不能是泛型参数以及通配符
                //处理Url类型注解：只能有一个，不能含有Path注解，不能请求方法注解中相对路径并存
                //顺序上必须在@Query相关类型前面,但是会限定一个参数只能有一个retrofit注解
                validateResolvableType(p, type);
                if (gotUrl) {
                    //Url类型注解：只能有一个
                    throw parameterError(method, p, "Multiple @Url method annotations found.");
                }
                if (gotPath) {
                    //Url类型注解：不能含有Path注解
                    throw parameterError(method, p, "@Path parameters may not be used with @Url.");
                }
                if (gotQuery) {
                    throw parameterError(method, p, "A @Url parameter must not come after a @Query.");
                }
                if (gotQueryName) {
                    throw parameterError(method, p, "A @Url parameter must not come after a @QueryName.");
                }
                if (gotQueryMap) {
                    throw parameterError(method, p, "A @Url parameter must not come after a @QueryMap.");
                }
                if (relativeUrl != null) {
                    //不能请求方法注解中相对路径并存，relativeUrl先解析完成
                    throw parameterError(method, p, "@Url cannot be used with @%s URL", httpMethod);
                }

                //url注解解析完成
                gotUrl = true;

                //检查type的类型
                if (type == HttpUrl.class
                        || type == String.class
                        || type == URI.class
                        || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
                    return new ParameterHandler.RelativeUrl(method, p);
                } else {
                    throw parameterError(
                            method,
                            p,
                            "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
                }

            } else if (annotation instanceof Path) {
//                Path注解只能在请求方法中有相对路径时使用，不能和Url注解混用
                validateResolvableType(p, type);
                if (gotQuery) {
                    throw parameterError(method, p, "A @Path parameter must not come after a @Query.");
                }
                if (gotQueryName) {
                    throw parameterError(method, p, "A @Path parameter must not come after a @QueryName.");
                }
                if (gotQueryMap) {
                    throw parameterError(method, p, "A @Path parameter must not come after a @QueryMap.");
                }
                if (gotUrl) {
                    throw parameterError(method, p, "@Path parameters may not be used with @Url.");
                }
                if (relativeUrl == null) {
                    throw parameterError(
                            method, p, "@Path can only be used with relative url on @%s", httpMethod);
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();
                //校验Path注解名字是否和方法相对路径一致
                validatePathName(p, name);

                Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                return new ParameterHandler.Path<>(method, p, name, converter, path.encoded());

            } else if (annotation instanceof Query) {
                validateResolvableType(p, type);
                Query query = (Query) annotation;
                String name = query.value();
                boolean encoded = query.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQuery = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(
                                method,
                                p,
                                rawParameterType.getSimpleName()
                                        + " must include generic type (e.g., "
                                        + rawParameterType.getSimpleName()
                                        + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter = retrofit.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            retrofit.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded);
                }

            } else if (annotation instanceof QueryName) {
                validateResolvableType(p, type);
                QueryName query = (QueryName) annotation;
                boolean encoded = query.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQueryName = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(
                                method,
                                p,
                                rawParameterType.getSimpleName()
                                        + " must include generic type (e.g., "
                                        + rawParameterType.getSimpleName()
                                        + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter = retrofit.stringConverter(iterableType, annotations);
                    return new ParameterHandler.QueryName<>(converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            retrofit.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.QueryName<>(converter, encoded).array();
                } else {
                    Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                    return new ParameterHandler.QueryName<>(converter, encoded);
                }

            } else if (annotation instanceof QueryMap) {
                validateResolvableType(p, type);
                Class<?> rawParameterType = Utils.getRawType(type);
                gotQueryMap = true;
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(method, p, "@QueryMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(
                            method, p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(method, p, "@QueryMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter = retrofit.stringConverter(valueType, annotations);

                return new ParameterHandler.QueryMap<>(
                        method, p, valueConverter, ((QueryMap) annotation).encoded());

            } else if (annotation instanceof Header) {
                validateResolvableType(p, type);
                Header header = (Header) annotation;
                String name = header.value();

                Class<?> rawParameterType = Utils.getRawType(type);
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(
                                method,
                                p,
                                rawParameterType.getSimpleName()
                                        + " must include generic type (e.g., "
                                        + rawParameterType.getSimpleName()
                                        + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter = retrofit.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Header<>(name, converter).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            retrofit.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Header<>(name, converter).array();
                } else {
                    Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                    return new ParameterHandler.Header<>(name, converter);
                }

            } else if (annotation instanceof HeaderMap) {
                if (type == Headers.class) {
                    return new ParameterHandler.Headers(method, p);
                }

                validateResolvableType(p, type);
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(method, p, "@HeaderMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(
                            method, p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(method, p, "@HeaderMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter = retrofit.stringConverter(valueType, annotations);

                return new ParameterHandler.HeaderMap<>(method, p, valueConverter);

            } else if (annotation instanceof Field) {
                validateResolvableType(p, type);
                if (!isFormEncoded) {
                    throw parameterError(method, p, "@Field parameters can only be used with form encoding.");
                }
                Field field = (Field) annotation;
                String name = field.value();
                boolean encoded = field.encoded();

                gotField = true;

                Class<?> rawParameterType = Utils.getRawType(type);
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(
                                method,
                                p,
                                rawParameterType.getSimpleName()
                                        + " must include generic type (e.g., "
                                        + rawParameterType.getSimpleName()
                                        + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter = retrofit.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            retrofit.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded);
                }

            } else if (annotation instanceof FieldMap) {
                validateResolvableType(p, type);
                if (!isFormEncoded) {
                    throw parameterError(
                            method, p, "@FieldMap parameters can only be used with form encoding.");
                }
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(method, p, "@FieldMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(
                            method, p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(method, p, "@FieldMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter = retrofit.stringConverter(valueType, annotations);

                gotField = true;
                return new ParameterHandler.FieldMap<>(
                        method, p, valueConverter, ((FieldMap) annotation).encoded());

            } else if (annotation instanceof Part) {
                validateResolvableType(p, type);
                if (!isMultipart) {
                    throw parameterError(
                            method, p, "@Part parameters can only be used with multipart encoding.");
                }
                Part part = (Part) annotation;
                gotPart = true;

                String partName = part.value();
                Class<?> rawParameterType = Utils.getRawType(type);
                if (partName.isEmpty()) {
                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
                        if (!(type instanceof ParameterizedType)) {
                            throw parameterError(
                                    method,
                                    p,
                                    rawParameterType.getSimpleName()
                                            + " must include generic type (e.g., "
                                            + rawParameterType.getSimpleName()
                                            + "<String>)");
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                        if (!MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                            throw parameterError(
                                    method,
                                    p,
                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                        }
                        return ParameterHandler.RawPart.INSTANCE.iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = rawParameterType.getComponentType();
                        if (!MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw parameterError(
                                    method,
                                    p,
                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                        }
                        return ParameterHandler.RawPart.INSTANCE.array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        return ParameterHandler.RawPart.INSTANCE;
                    } else {
                        throw parameterError(
                                method,
                                p,
                                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                    }
                } else {
                    Headers headers =
                            Headers.of(
                                    "Content-Disposition",
                                    "form-data; name=\"" + partName + "\"",
                                    "Content-Transfer-Encoding",
                                    part.encoding());

                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
                        if (!(type instanceof ParameterizedType)) {
                            throw parameterError(
                                    method,
                                    p,
                                    rawParameterType.getSimpleName()
                                            + " must include generic type (e.g., "
                                            + rawParameterType.getSimpleName()
                                            + "<String>)");
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                        if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                            throw parameterError(
                                    method,
                                    p,
                                    "@Part parameters using the MultipartBody.Part must not "
                                            + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                retrofit.requestBodyConverter(iterableType, annotations, methodAnnotations);
                        return new ParameterHandler.Part<>(method, p, headers, converter).iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                        if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw parameterError(
                                    method,
                                    p,
                                    "@Part parameters using the MultipartBody.Part must not "
                                            + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                retrofit.requestBodyConverter(arrayComponentType, annotations, methodAnnotations);
                        return new ParameterHandler.Part<>(method, p, headers, converter).array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        throw parameterError(
                                method,
                                p,
                                "@Part parameters using the MultipartBody.Part must not "
                                        + "include a part name in the annotation.");
                    } else {
                        Converter<?, RequestBody> converter =
                                retrofit.requestBodyConverter(type, annotations, methodAnnotations);
                        return new ParameterHandler.Part<>(method, p, headers, converter);
                    }
                }

            } else if (annotation instanceof PartMap) {
                validateResolvableType(p, type);
                if (!isMultipart) {
                    throw parameterError(
                            method, p, "@PartMap parameters can only be used with multipart encoding.");
                }
                gotPart = true;
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw parameterError(method, p, "@PartMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw parameterError(
                            method, p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;

                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw parameterError(method, p, "@PartMap keys must be of type String: " + keyType);
                }

                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
                    throw parameterError(
                            method,
                            p,
                            "@PartMap values cannot be MultipartBody.Part. "
                                    + "Use @Part List<Part> or a different value type instead.");
                }

                Converter<?, RequestBody> valueConverter =
                        retrofit.requestBodyConverter(valueType, annotations, methodAnnotations);

                PartMap partMap = (PartMap) annotation;
                return new ParameterHandler.PartMap<>(method, p, valueConverter, partMap.encoding());

            } else if (annotation instanceof Body) {
                validateResolvableType(p, type);
                if (isFormEncoded || isMultipart) {
                    throw parameterError(
                            method, p, "@Body parameters cannot be used with form or multi-part encoding.");
                }
                if (gotBody) {
                    throw parameterError(method, p, "Multiple @Body method annotations found.");
                }

                Converter<?, RequestBody> converter;
                try {
                    converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw parameterError(method, e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(method, p, converter);

            } else if (annotation instanceof Tag) {
                validateResolvableType(p, type);

                Class<?> tagType = Utils.getRawType(type);
                for (int i = p - 1; i >= 0; i--) {
                    ParameterHandler<?> otherHandler = parameterHandlers[i];
                    if (otherHandler instanceof ParameterHandler.Tag
                            && ((ParameterHandler.Tag) otherHandler).cls.equals(tagType)) {
                        throw parameterError(
                                method,
                                p,
                                "@Tag type "
                                        + tagType.getName()
                                        + " is duplicate of parameter #"
                                        + (i + 1)
                                        + " and would always overwrite its value.");
                    }
                }

                return new ParameterHandler.Tag<>(tagType);
            }

            return null; // Not a Retrofit annotation.
        }

        /**
         * 校验参数类型：不能是泛型参数以及通配符
         *
         * @param p    position
         * @param type type
         */
        private void validateResolvableType(int p, Type type) {
            if (Utils.hasUnresolvableType(type)) {
                throw parameterError(
                        method, p, "Parameter type must not include a type variable or wildcard: %s", type);
            }
        }

        //检验方法参数列表Path注解的value是否和方法相对路径上path名称一致
        private void validatePathName(int p, String name) {
            if (!PARAM_NAME_REGEX.matcher(name).matches()) {
                throw parameterError(
                        method,
                        p,
                        "@Path parameter name must match %s. Found: %s",
                        PARAM_URL_REGEX.pattern(),
                        name);
            }
            // Verify URL replacement name is actually present in the URL path.
            if (!relativeUrlParamNames.contains(name)) {
                throw parameterError(method, p, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
            }
        }

        /**
         * Gets the set of unique path parameters used in the given URI. If a parameter is used twice in
         * the URI, it will only show up once in the set.
         * <p>
         * 获取对目标路径中的{path}参数path名称，若是路径中出现多次比如：xxx{path}/aaa{path}/aaa这种
         * 通过集合保证path去重
         */
        static Set<String> parsePathParameters(String path) {
            Matcher m = PARAM_URL_REGEX.matcher(path);
            Set<String> patterns = new LinkedHashSet<>();
            while (m.find()) {
                patterns.add(m.group(1));
            }
            return patterns;
        }

        private static Class<?> boxIfPrimitive(Class<?> type) {
            if (boolean.class == type) return Boolean.class;
            if (byte.class == type) return Byte.class;
            if (char.class == type) return Character.class;
            if (double.class == type) return Double.class;
            if (float.class == type) return Float.class;
            if (int.class == type) return Integer.class;
            if (long.class == type) return Long.class;
            if (short.class == type) return Short.class;
            return type;
        }
    }
}
