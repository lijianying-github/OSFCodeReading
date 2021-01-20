/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.gson.internal.bind;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.reflect.ReflectionAccessor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Type adapter that reflects over the fields and methods of a class.
 */
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
  private final ConstructorConstructor constructorConstructor;
  private final FieldNamingStrategy fieldNamingPolicy;
  private final Excluder excluder;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
  private final ReflectionAccessor accessor = ReflectionAccessor.getInstance();

  public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor,
      FieldNamingStrategy fieldNamingPolicy, Excluder excluder,
      JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory) {
    this.constructorConstructor = constructorConstructor;
    this.fieldNamingPolicy = fieldNamingPolicy;
    this.excluder = excluder;
    this.jsonAdapterFactory = jsonAdapterFactory;
  }

  public boolean excludeField(Field f, boolean serialize) {
    return excludeField(f, serialize, excluder);
  }

  static boolean excludeField(Field f, boolean serialize, Excluder excluder) {
    return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
  }

  /** first element holds the default name */
  private List<String> getFieldNames(Field f) {
    SerializedName annotation = f.getAnnotation(SerializedName.class);
    if (annotation == null) {
      String name = fieldNamingPolicy.translateName(f);
      return Collections.singletonList(name);
    }

    String serializedName = annotation.value();
    String[] alternates = annotation.alternate();
    if (alternates.length == 0) {
      return Collections.singletonList(serializedName);
    }

    List<String> fieldNames = new ArrayList<String>(alternates.length + 1);
    fieldNames.add(serializedName);
    for (String alternate : alternates) {
      fieldNames.add(alternate);
    }
    return fieldNames;
  }

  @Override public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();

    if (!Object.class.isAssignableFrom(raw)) {
      return null; // it's a primitive!
    }

    ObjectConstructor<T> constructor = constructorConstructor.get(type);
    return new Adapter<T>(constructor, getBoundFields(gson, type, raw));
  }

  private BoundField createBoundField(
      final Gson context, final Field field, final String name,
      final TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
    // 是否是原始数据类型 （int,boolean,float...）
    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
    // special casing primitives here saves ~5% on Android...
    JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
    TypeAdapter<?> mapped = null;
    if (annotation != null) {
      // Gson尝试获取该类型的TypeAdapter，这个方法我们后面也会继续提到。
      mapped = jsonAdapterFactory.getTypeAdapter(
          constructorConstructor, context, fieldType, annotation);
    }
    final boolean jsonAdapterPresent = mapped != null;
    //获取当前json串的一个name/value的name对应JavaBean里面的变量类型type,或者这个type对应的TypeAdpater类
    if (mapped == null) mapped = context.getAdapter(fieldType);;//获取对应的基本类型的
// final变量，便于内部类使用
    final TypeAdapter<?> typeAdapter = mapped;
    return new BoundField(name, serialize, deserialize) {
      @SuppressWarnings({"unchecked", "rawtypes"}) // the type adapter and field type always agree
      @Override void write(JsonWriter writer, Object value)
          throws IOException, IllegalAccessException {
        Object fieldValue = field.get(value);
        TypeAdapter t = jsonAdapterPresent ? typeAdapter
            : new TypeAdapterRuntimeTypeWrapper(context, typeAdapter, fieldType.getType());
        t.write(writer, fieldValue);
      }
      // ReflectiveTypeAdapter委托的Json读操作会调用到这里
      @Override void read(JsonReader reader, Object value)
          throws IOException, IllegalAccessException {
        // 通过该属性的类型对应的TypeAdapter尝试读取json串
        //如果是基础类型，则直接读取，
        //如果是复合类型则递归之前的流程
        Object fieldValue = typeAdapter.read(reader); // 获取下一个jsontoken而不消耗它
        if (fieldValue != null || !isPrimitive) {
          field.set(value, fieldValue);//更新filed值
        }
      }
      @Override public boolean writeField(Object value) throws IOException, IllegalAccessException {
        if (!serialized) return false;
        Object fieldValue = field.get(value);
        return fieldValue != value; // avoid recursion for example for Throwable.cause
      }
    };
  }

  private Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
    Map<String, BoundField> result = new LinkedHashMap<String, BoundField>();// 创建一个Map结构，存放所有的BoundField
    if (raw.isInterface()) {
      return result;
    }

    Type declaredType = type.getType();
    while (raw != Object.class) { // 如果类型是Object则结束循环
      Field[] fields = raw.getDeclaredFields(); // 获取该类型的所有的内部属性
      for (Field field : fields) {
        boolean serialize = excludeField(field, true);
        boolean deserialize = excludeField(field, false);
        if (!serialize && !deserialize) {
          continue;
        }
        accessor.makeAccessible(field);
        Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
        List<String> fieldNames = getFieldNames(field);// 获取该Filed的名字（Gson通过注解可以给一个属性多个解析名）
        BoundField previous = null;
        for (int i = 0, size = fieldNames.size(); i < size; ++i) {
          String name = fieldNames.get(i);
          // 多个解析名，第一作为默认的序列化名称
          if (i != 0) serialize = false; // only serialize the default name
          // 创建BoundField
          //根据filed、type、以及是否支持序列化和反序列化来创建一个BoundField对象
          BoundField boundField = createBoundField(context, field, name,
              TypeToken.get(fieldType), serialize, deserialize);
          // 将BoundField放入Map中，获取被替换掉的value(如果有的话)
          //将属性名称作为key，boundField作为value保存到result中
          BoundField replaced = result.put(name, boundField);
          //如果之前解析过对应的这个值的话，这里就会导致previous不为空，从而报错
          if (previous == null) previous = replaced;
        }
        // 做好记录
        if (previous != null) {
          // 如果previous != null证明出现了两个相同的Filed name，直接抛出错误
          // 注：Gson不允许定义两个相同的名称的属性（父类和子类之间可能出现）
          throw new IllegalArgumentException(declaredType
              + " declares multiple JSON fields named " + previous.name);
        }
      }
      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();// 获取父类类型，最终会索引到Object.因为Object是所有对象的父类
    }
    return result;
  }

  static abstract class BoundField {
    final String name;
    final boolean serialized;
    final boolean deserialized;

    protected BoundField(String name, boolean serialized, boolean deserialized) {
      this.name = name;
      this.serialized = serialized;
      this.deserialized = deserialized;
    }
    abstract boolean writeField(Object value) throws IOException, IllegalAccessException;
    abstract void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException;
    abstract void read(JsonReader reader, Object value) throws IOException, IllegalAccessException;
  }

  public static final class Adapter<T> extends TypeAdapter<T> {
    private final ObjectConstructor<T> constructor;// 该复合类型的构造器，用于反射创建对象
    private final Map<String, BoundField> boundFields;// 该类型内部的所有的Filed属性，都通过map存储起来

    Adapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
      this.constructor = constructor;
      this.boundFields = boundFields;
    }
    //JsonReader是Gson封装的对Json相关的操作类，可以依次读取json数据
    @Override public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      T instance = constructor.construct();

      try {
        in.beginObject();
        while (in.hasNext()) {
          String name = in.nextName();//开始逐个读取json串中的key
          BoundField field = boundFields.get(name); // 通过key寻找对应的属性
          if (field == null || !field.deserialized) {
            in.skipValue();
          } else {
            field.read(in, instance);
          }
        }
      } catch (IllegalStateException e) {
        throw new JsonSyntaxException(e);
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
      in.endObject();
      return instance;
    }

    @Override public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }

      out.beginObject();
      try {
        for (BoundField boundField : boundFields.values()) {
          if (boundField.writeField(value)) {
            out.name(boundField.name);
            boundField.write(out, value);
          }
        }
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
      out.endObject();
    }
  }
}
