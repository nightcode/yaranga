/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nightcode.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.nightcode.common.logging.Log;

/**
 * Reflect utils.
 */
public enum ReflectUtils {
  ;

  private static final class GenericArrayTypeImpl implements GenericArrayType {
    private final Type genericComponentType;

    GenericArrayTypeImpl(Type genericComponentType) {
      this.genericComponentType = genericComponentType;
    }

    public Type getGenericComponentType() {
      return this.genericComponentType;
    }
  }

  private static final class ParameterizedTypeImpl implements ParameterizedType {
    private final Type     ownerType;
    private final Class<?> rawType;
    private final Type[]   typeArguments;

    ParameterizedTypeImpl(Type ownerType, Class<?> rawType, Type[] typeArguments) {
      this.ownerType     = ownerType;
      this.rawType       = rawType;
      this.typeArguments = typeArguments;
    }

    @Override public Type[] getActualTypeArguments() {
      return typeArguments;
    }

    @Override public Type getOwnerType() {
      return ownerType;
    }

    @Override public Type getRawType() {
      return rawType;
    }
  }

  private static final class WildcardTypeImpl implements WildcardType {
    private final Type[] lowerBounds;
    private final Type[] upperBounds;

    WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
      this.lowerBounds = lowerBounds;
      this.upperBounds = upperBounds;
    }

    @Override public Type[] getLowerBounds() {
      return lowerBounds;
    }

    @Override public Type[] getUpperBounds() {
      return upperBounds;
    }
  }

  private static final Class<?>[] EMPTY_CLASS_ARRAY  = new Class<?>[0];
  private static final Object[]   EMPTY_OBJECT_ARRAY = new Object[0];

  private static Type resolveGenericArrayType(GenericArrayType type) {
    Type componentType         = type.getGenericComponentType();
    Type resolvedComponentType = resolveType(componentType);
    return new GenericArrayTypeImpl(resolvedComponentType);
  }

  private static Type resolveParameterizedType(ParameterizedType type) {
    Type owner = type.getOwnerType();

    Type   resolvedOwner    = (owner == null) ? null : resolveType(owner);
    Type   resolvedRawType  = resolveType(type.getRawType());
    Type[] resolvedTypeArgs = resolveTypes(type.getActualTypeArguments());

    return new ParameterizedTypeImpl(resolvedOwner, (Class<?>) resolvedRawType, resolvedTypeArgs);
  }

  private static Type resolveWildcardType(WildcardType type) {
    Type[] lowerBounds = resolveTypes(type.getLowerBounds());
    Type[] upperBounds = resolveTypes(type.getUpperBounds());

    return new WildcardTypeImpl(lowerBounds, upperBounds);
  }

  private static Type[] resolveTypes(Type[] types) {
    Type[] resolved = new Type[types.length];
    for (int i = 0; i < types.length; i++) {
      resolved[i] = resolveType(types[i]);
    }
    return resolved;
  }

  public static Function<Method, Annotation> annotationFunction(final Class<? extends Annotation> marker) {
    return method -> method.getAnnotation(marker);
  }

  public static Object invokeMethod(Method method, @Nullable Object target) {
    return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
  }

  public static Object invokeMethod(Method method, @Nullable Object target, @Nullable Object... args) {
    try {
      return method.invoke(target, args);
    } catch (Exception ex) {
      throw Throwables.rethrow(ex);
    }
  }

  public static boolean isClassPresent(String className, @Nullable ClassLoader classLoader) {
    try {
      Class.forName(className, false, classLoader);
    } catch (IllegalAccessError ex) {
      throw Throwables.rethrow(ex);
    } catch (Throwable th) {
      Log.trace().log(ReflectUtils.class, th, "unable to resolve class {} (ClassLoader {})", className, classLoader);
      return false;
    }
    return true;
  }

  public static Type resolveType(Type type) {
    if (type instanceof ParameterizedType) {
      return resolveParameterizedType((ParameterizedType) type);
    } else if (type instanceof GenericArrayType) {
      return resolveGenericArrayType((GenericArrayType) type);
    } else if (type instanceof WildcardType) {
      return resolveWildcardType((WildcardType) type);
    } else {
      return type;
    }
  }

  public static @Nullable Method findMethod(Class<?> clazz, String name) {
    return findMethod(clazz, name, EMPTY_CLASS_ARRAY);
  }

  public static @Nullable Method findMethod(Class<?> clazz, String name, @Nullable Class<?>... paramTypes) {
    Class<?> target = clazz;
    while (target != null) {
      Method[] methods = target.isInterface() ? target.getMethods() : getDeclaredMethods(target);
      for (Method method : methods) {
        if (name.equals(method.getName()) && (paramTypes == null || Arrays.equals(method.getParameterTypes(), paramTypes))) {
          return method;
        }
      }
      target = target.getSuperclass();
    }
    return null;
  }

  private static Method[] getDeclaredMethods(Class<?> clazz) {
    try {
      Method[]     declaredMethods = clazz.getDeclaredMethods();
      List<Method> defaultMethods  = getDefaultMethods(clazz);
      if (defaultMethods.isEmpty()) {
        return declaredMethods;
      }
      Method[] result = new Method[declaredMethods.length + defaultMethods.size()];
      System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
      int i = declaredMethods.length;
      for (Method method : defaultMethods) {
        result[i++] = method;
      }
      return result;
    } catch (Throwable ex) {
      throw Throwables.rethrow(ex);
    }
  }

  private static List<Method> getDefaultMethods(Class<?> clazz) {
    List<Method> result = new ArrayList<>();
    for (Class<?> iface : clazz.getInterfaces()) {
      for (Method method : iface.getMethods()) {
        if (!Modifier.isAbstract(method.getModifiers())) {
          result.add(method);
        }
      }
    }
    return result;
  }
}
