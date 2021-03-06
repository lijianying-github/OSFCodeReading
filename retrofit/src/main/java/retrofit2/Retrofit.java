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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Url;

import static java.util.Collections.unmodifiableList;

/**
 * Retrofit设计模式是外观设计模式
 * build 是Builder设计模式
 * create方法是动态代理设计模式
 * <p>
 * Retrofit adapts a Java interface to HTTP calls by using annotations on the declared methods to
 * define how requests are made. Create instances using {@linkplain Builder the builder} and pass
 * your interface to {@link #create} to generate an implementation.
 *
 * <p>For example,
 *
 * <pre><code>
 * Retrofit retrofit = new Retrofit.Builder()
 *     .baseUrl("https://api.example.com/")
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build();
 *
 * MyApi api = retrofit.create(MyApi.class);
 * Response&lt;User&gt; user = api.getUser().execute();
 * </code></pre>
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class Retrofit {
    //接口方法解析缓存，目的减少解析反射注解解析性能损耗以及复用
    private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();

    //实现是OkHttpClient 默认只支持OkHttp请求
    final okhttp3.Call.Factory callFactory;
    /**
     * 所有请求的基本地址
     */
    final HttpUrl baseUrl;
    //转换工厂集合，将用户数据实体转换成request中参数以及将response原始数据转换成用户自定义实体类型
    final List<Converter.Factory> converterFactories;
    //call适配器工厂集合，将retrofit默认call转换成其他call如rx方式
    final List<CallAdapter.Factory> callAdapterFactories;
    //默认callAdapter call enqueue异步回调执行器，，android 平台默认实现是一个主线程的handler,其他平台是null，可以重写成线程池
    final @Nullable
    Executor callbackExecutor;
    //立刻验证retrofit接口标志位
    final boolean validateEagerly;

    Retrofit(
            okhttp3.Call.Factory callFactory,
            HttpUrl baseUrl,
            List<Converter.Factory> converterFactories,
            List<CallAdapter.Factory> callAdapterFactories,
            @Nullable Executor callbackExecutor,
            boolean validateEagerly) {
        this.callFactory = callFactory;
        this.baseUrl = baseUrl;
        this.converterFactories = converterFactories; // Copy+unmodifiable at call site.
        this.callAdapterFactories = callAdapterFactories; // Copy+unmodifiable at call site.
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
    }

    /**
     * 创建Retrofit 类型接口实现
     *
     * <p>请求方法类型注解中包含了请求的相对地址. 内置请求方法有 {@link retrofit2.http.GET GET}, {@link
     * retrofit2.http.PUT PUT}, {@link retrofit2.http.POST POST}, {@link retrofit2.http.PATCH PATCH},
     * {@link retrofit2.http.HEAD HEAD}, {@link retrofit2.http.DELETE DELETE} and {@link
     * retrofit2.http.OPTIONS OPTIONS}. 也可以通过Http注解就行自定义网络请求方法 {@link HTTP @HTTP}.
     * 对于动态请求地址，请求方法注解中可以省略path,但是需要在接口方法中第一个参数需要添加{@link
     * Url @Url}注解 ，调用时可以实现动态地址请求
     *
     * <p>Method parameters can be used to replace parts of the URL by annotating them with {@link
     * retrofit2.http.Path @Path}. Replacement sections are denoted by an identifier surrounded by
     * curly braces (e.g., "{foo}"). To add items to the query string of a URL use {@link
     * retrofit2.http.Query @Query}.
     *
     * <p>The body of a request is denoted by the {@link retrofit2.http.Body @Body} annotation. The
     * object will be converted to request representation by one of the {@link Converter.Factory}
     * instances. A {@link RequestBody} can also be used for a raw representation.
     *
     * <p>Alternative request body formats are supported by method annotations and corresponding
     * parameter annotations:
     *
     * <ul>
     *   <li>{@link retrofit2.http.FormUrlEncoded @FormUrlEncoded} - Form-encoded data with key-value
     *       pairs specified by the {@link retrofit2.http.Field @Field} parameter annotation.
     *   <li>{@link retrofit2.http.Multipart @Multipart} - RFC 2388-compliant multipart data with
     *       parts specified by the {@link retrofit2.http.Part @Part} parameter annotation.
     * </ul>
     *
     * <p>Additional static headers can be added for an endpoint using the {@link
     * retrofit2.http.Headers @Headers} method annotation. For per-request control over a header
     * annotate a parameter with {@link Header @Header}.
     *
     * <p>By default, methods return a {@link Call} which represents the HTTP request. The generic
     * parameter of the call is the response body type and will be converted by one of the {@link
     * Converter.Factory} instances. {@link ResponseBody} can also be used for a raw representation.
     * {@link Void} can be used if you do not care about the body contents.
     *
     * <p>For example:
     *
     * <pre>
     * public interface CategoryService {
     *   &#64;POST("category/{cat}/")
     *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    public <T> T create(final Class<T> service) {
        //校验retrofit接口
        validateServiceInterface(service);
        return (T)
                Proxy.newProxyInstance(
                        service.getClassLoader(),
                        new Class<?>[]{service},
                        new InvocationHandler() {
                            private final Platform platform = Platform.get();
                            private final Object[] emptyArgs = new Object[0];

                            @Override
                            public @Nullable
                            Object invoke(Object proxy, Method method, @Nullable Object[] args)
                                    throws Throwable {
                                //调用接口的具体方法，实际中是返回一个保证的okhttp的call
                                // If the method is a method from Object then defer to normal invocation.
                                //获取方法声明的地方，若是超类Object的方法比如notify这些方法，则直接调用方法
                                if (method.getDeclaringClass() == Object.class) {
                                    return method.invoke(this, args);
                                }
                                args = args != null ? args : emptyArgs;
                                //若是接口的java8默认方法则直接调用，否则解析方法注解并调用
                                return platform.isDefaultMethod(method)
                                        ? platform.invokeDefaultMethod(method, service, proxy, args)
                                        : loadServiceMethod(method).invoke(args);
                            }
                        });
    }

    private void validateServiceInterface(Class<?> service) {
        if (!service.isInterface()) {
            //retrofit api必须定义为接口
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }

        //array双队列缓存接口service 接口 class对象 ArrayDeque：双端队列有头有尾 容量超出时会自动扩容
        Deque<Class<?>> check = new ArrayDeque<>(1);
        check.add(service);

        //判断service以及service继承父类接口中有无泛型，若有，终止service代理创建
        while (!check.isEmpty()) {
            //双端队列取出头部class对象
            Class<?> candidate = check.removeFirst();
            //获取接口类的泛型声明个数，若有泛型声明则不能实例化接口
            if (candidate.getTypeParameters().length != 0) {
                //retrofit 接口类声明中不能声明泛型
                StringBuilder message =
                        new StringBuilder("Type parameters are unsupported on ").append(candidate.getName());
                if (candidate != service) {
                    //当前循环接口class是service接口的父接口
                    message.append(" which is an interface of ").append(service.getName());
                }
                //异常终止吵架呢
                throw new IllegalArgumentException(message.toString());
            }
            //获取service 接口实现的的父接口的class添加到双端队列中，继续循环判断父接口
            Collections.addAll(check, candidate.getInterfaces());
        }

        //是否立即解析方法注解
        if (validateEagerly) {
            //根据运行虚拟机获取调用平台，虚拟机类型为Dalvik：调用Android
            Platform platform = Platform.get();
            //遍历service接口类中所有声明方法，包括私有，静态
            for (Method method : service.getDeclaredMethods()) {
                //过滤java8中默认方法以及静态方法
                if (!platform.isDefaultMethod(method) && !Modifier.isStatic(method.getModifiers())) {
                    //加载service接口中的方法
                    loadServiceMethod(method);
                }
            }
        }
    }

    /**
     * 解析方法注解生成retrofit call
     *
     * @param method 待解析方法
     * @return HttpServiceMethod
     */
    ServiceMethod<?> loadServiceMethod(Method method) {
        //先获取serviceMethodCache缓存中的方法，若有直接返回缓存中结果
        ServiceMethod<?> result = serviceMethodCache.get(method);
        if (result != null) return result;

        //同步锁 解析接口方法创建ServiceMethod实例并加入缓存
        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                //方法解析并创建ServiceMethod实例
                result = ServiceMethod.parseAnnotations(this, method);
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }

    /**
     * The factory used to create {@linkplain okhttp3.Call OkHttp calls} for sending a HTTP requests.
     * Typically an instance of {@link OkHttpClient}.
     */
    public okhttp3.Call.Factory callFactory() {
        return callFactory;
    }

    /**
     * The API base URL.
     */
    public HttpUrl baseUrl() {
        return baseUrl;
    }

    /**
     * Returns a list of the factories tried when creating a {@linkplain #callAdapter(Type,
     * Annotation[])} call adapter}.
     */
    public List<CallAdapter.Factory> callAdapterFactories() {
        return callAdapterFactories;
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    public CallAdapter<?, ?> nextCallAdapter(
            @Nullable CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {
        Objects.requireNonNull(returnType, "returnType == null");
        Objects.requireNonNull(annotations, "annotations == null");

        int start = callAdapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }

        StringBuilder builder =
                new StringBuilder("Could not locate call adapter for ").append(returnType).append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns an unmodifiable list of the factories tried when creating a {@linkplain
     * #requestBodyConverter(Type, Annotation[], Annotation[]) request body converter}, a {@linkplain
     * #responseBodyConverter(Type, Annotation[]) response body converter}, or a {@linkplain
     * #stringConverter(Type, Annotation[]) string converter}.
     */
    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<T, RequestBody> requestBodyConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<T, RequestBody> nextRequestBodyConverter(
            @Nullable Converter.Factory skipPast,
            Type type,
            Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(parameterAnnotations, "parameterAnnotations == null");
        Objects.requireNonNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, RequestBody>) converter;
            }
        }

        StringBuilder builder =
                new StringBuilder("Could not locate RequestBody converter for ").append(type).append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    public <T> Converter<ResponseBody, T> nextResponseBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(annotations, "annotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<ResponseBody, T>) converter;
            }
        }

        StringBuilder builder =
                new StringBuilder("Could not locate ResponseBody converter for ")
                        .append(type)
                        .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link String} from the available {@linkplain
     * #converterFactories() factories}.
     * 获取converterFactories中实现stringConverter方法的Converter
     */
    public <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    /**
     * The executor used for {@link Callback} methods on a {@link Call}. This may be {@code null}, in
     * which case callbacks should be made synchronously on the background thread.
     */
    public @Nullable
    Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Build a new {@link Retrofit}.
     *
     * <p>Calling {@link #baseUrl} is required before calling {@link #build()}. All other methods are
     * optional.
     */
    public static final class Builder {
        private final Platform platform;
        private @Nullable
        okhttp3.Call.Factory callFactory;
        private @Nullable
        HttpUrl baseUrl;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        private @Nullable
        Executor callbackExecutor;
        private boolean validateEagerly;

        Builder(Platform platform) {
            this.platform = platform;
        }

        public Builder() {
            this(Platform.get());
        }

        Builder(Retrofit retrofit) {
            platform = Platform.get();
            callFactory = retrofit.callFactory;
            baseUrl = retrofit.baseUrl;

            // Do not add the default BuiltIntConverters and platform-aware converters added by build().
            for (int i = 1,
                 size = retrofit.converterFactories.size() - platform.defaultConverterFactoriesSize();
                 i < size;
                 i++) {
                converterFactories.add(retrofit.converterFactories.get(i));
            }

            // Do not add the default, platform-aware call adapters added by build().
            for (int i = 0,
                 size =
                 retrofit.callAdapterFactories.size() - platform.defaultCallAdapterFactoriesSize();
                 i < size;
                 i++) {
                callAdapterFactories.add(retrofit.callAdapterFactories.get(i));
            }

            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
        }

        /**
         * The HTTP client used for requests.
         *
         * <p>This is a convenience method for calling {@link #callFactory}.
         */
        public Builder client(OkHttpClient client) {
            return callFactory(Objects.requireNonNull(client, "client == null"));
        }

        /**
         * Specify a custom call factory for creating {@link Call} instances.
         *
         * <p>Note: Calling {@link #client} automatically sets this value.
         */
        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = Objects.requireNonNull(factory, "factory == null");
            return this;
        }

        /**
         * Set the API base URL.
         *
         * @see #baseUrl(HttpUrl)
         */
        public Builder baseUrl(URL baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl == null");
            return baseUrl(HttpUrl.get(baseUrl.toString()));
        }

        /**
         * Set the API base URL.
         *
         * @see #baseUrl(HttpUrl)
         */
        public Builder baseUrl(String baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl == null");
            return baseUrl(HttpUrl.get(baseUrl));
        }

        /**
         * Set the API base URL.
         *
         * <p>The specified endpoint values (such as with {@link GET @GET}) are resolved against this
         * value using {@link HttpUrl#resolve(String)}. The behavior of this matches that of an {@code
         * <a href="">} link on a website resolving on the current URL.
         *
         * <p><b>Base URLs should always end in {@code /}.</b>
         *
         * <p>A trailing {@code /} ensures that endpoints values which are relative paths will correctly
         * append themselves to a base which has path components.
         *
         * <p><b>Correct:</b><br>
         * Base URL: http://example.com/api/<br>
         * Endpoint: foo/bar/<br>
         * Result: http://example.com/api/foo/bar/
         *
         * <p><b>Incorrect:</b><br>
         * Base URL: http://example.com/api<br>
         * Endpoint: foo/bar/<br>
         * Result: http://example.com/foo/bar/
         *
         * <p>This method enforces that {@code baseUrl} has a trailing {@code /}.
         *
         * <p><b>Endpoint values which contain a leading {@code /} are absolute.</b>
         *
         * <p>Absolute values retain only the host from {@code baseUrl} and ignore any specified path
         * components.
         *
         * <p>Base URL: http://example.com/api/<br>
         * Endpoint: /foo/bar/<br>
         * Result: http://example.com/foo/bar/
         *
         * <p>Base URL: http://example.com/<br>
         * Endpoint: /foo/bar/<br>
         * Result: http://example.com/foo/bar/
         *
         * <p><b>Endpoint values may be a full URL.</b>
         *
         * <p>Values which have a host replace the host of {@code baseUrl} and values also with a scheme
         * replace the scheme of {@code baseUrl}.
         *
         * <p>Base URL: http://example.com/<br>
         * Endpoint: https://github.com/square/retrofit/<br>
         * Result: https://github.com/square/retrofit/
         *
         * <p>Base URL: http://example.com<br>
         * Endpoint: //github.com/square/retrofit/<br>
         * Result: http://github.com/square/retrofit/ (note the scheme stays 'http')
         */
        public Builder baseUrl(HttpUrl baseUrl) {
            Objects.requireNonNull(baseUrl, "baseUrl == null");
            List<String> pathSegments = baseUrl.pathSegments();
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
            }
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Add converter factory for serialization and deserialization of objects.
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(Objects.requireNonNull(factory, "factory == null"));
            return this;
        }

        /**
         * Add a call adapter factory for supporting service method return types other than {@link
         * Call}.
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(Objects.requireNonNull(factory, "factory == null"));
            return this;
        }

        /**
         * The executor on which {@link Callback} methods are invoked when returning {@link Call} from
         * your service method.
         *
         * <p>Note: {@code executor} is not used for {@linkplain #addCallAdapterFactory custom method
         * return types}.
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = Objects.requireNonNull(executor, "executor == null");
            return this;
        }

        /**
         * Returns a modifiable list of call adapter factories.
         */
        public List<CallAdapter.Factory> callAdapterFactories() {
            return this.callAdapterFactories;
        }

        /**
         * Returns a modifiable list of converter factories.
         */
        public List<Converter.Factory> converterFactories() {
            return this.converterFactories;
        }

        /**
         * When calling {@link #create} on the resulting {@link Retrofit} instance, eagerly validate the
         * configuration of all methods in the supplied interface.
         */
        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        /**
         * Create the {@link Retrofit} instance using the configured values.
         *
         * <p>Note: If neither {@link #client} nor {@link #callFactory} is called a default {@link
         * OkHttpClient} will be created and used.
         */
        public Retrofit build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }

            okhttp3.Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                //默认 OkHttp client
                callFactory = new OkHttpClient();
            }

            Executor callbackExecutor = this.callbackExecutor;
            if (callbackExecutor == null) {
                //默认回调执行器，内部实现是主线程handler post runnable+
                callbackExecutor = platform.defaultCallbackExecutor();
            }

            // Make a defensive copy of the adapters and add the default Call adapter.
            //默认的call 适配器是将一个包装的okhttp call的retrofit call和主线程handler回调绑定在一起
            List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
            callAdapterFactories.addAll(platform.defaultCallAdapterFactories(callbackExecutor));

            // Make a defensive copy of the converters.
            //length=内置转换器1+用户自定义转换器+java 8Optional泛型类转换器
            List<Converter.Factory> converterFactories =
                    new ArrayList<>(
                            1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize());

            // Add the built-in converter factory first. This prevents overriding its behavior but also
            // ensures correct behavior when using converters that consume all types.
            //添加内置转换器
            converterFactories.add(new BuiltInConverters());

            //用户自定义类型转换器
            converterFactories.addAll(this.converterFactories);

            //若是java 8 添加Optional泛型类参数转换器
            converterFactories.addAll(platform.defaultConverterFactories());

            return new Retrofit(
                    callFactory,
                    baseUrl,
                    unmodifiableList(converterFactories),
                    unmodifiableList(callAdapterFactories),
                    callbackExecutor,
                    validateEagerly);
        }
    }
}
