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

package org.nightcode.common.terminal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.nightcode.common.terminal.value.ValueConverter;
import org.nightcode.common.terminal.value.ValueConverterService;
import org.nightcode.common.util.ReflectUtils;

import static java.lang.String.format;

/**
 * Command argument.
 *
 * @param name the name
 * @param annotation the annotation
 * @param converter the converter
 */
public record Argument(String name, Arg annotation, ValueConverter<?> converter) {

  static final class Continuation<T> {
    public static <T> Continuation<T> of(T result) {
      return new Continuation<>(result, null);
    }

    public static <T> Continuation<T> of(T result, @Nullable Function<T, Continuation<T>> next) {
      return new Continuation<>(result, next);
    }

    private final T                            result;
    private final Function<T, Continuation<T>> next;

    private Continuation(T result, @Nullable Function<T, Continuation<T>> next) {
      this.result = result;
      this.next   = next;
    }

    public boolean hasNext() {
      return next != null;
    }

    public Function<T, Continuation<T>> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return next;
    }

    public T result() {
      return result;
    }
  }

  private static final class GeneralStage implements Function<ResourceContext, Continuation<ResourceContext>> {
    @Override public Continuation<ResourceContext> apply(ResourceContext input) {
      Continuation<ResourceContext> next;
      if (input.hasNext()) {
        input.addArgument(input.next());
        next = Continuation.of(input, GENERAL_STAGE);
      } else {
        next = Continuation.of(input);
      }
      return next;
    }
  }

  private static final class ResourceContext {
    private final Method                method;
    private final ValueConverterService valueConverterService;

    private final Type[]         types;
    private final Annotation[][] annotations;
    private final int            length;
    private       int            index;

    private final List<Argument> arguments = new ArrayList<>();

    ResourceContext(Method method) {
      this(method, ValueConverterService.def());
    }

    ResourceContext(Method method, ValueConverterService valueConverterService) {
      this.method                = method;
      this.valueConverterService = valueConverterService;

      types       = method.getGenericParameterTypes();
      annotations = method.getParameterAnnotations();
      length      = method.getParameterAnnotations().length;
    }

    void addArgument(Argument argument) {
      arguments.add(argument);
    }

    List<Argument> getArguments() {
      return arguments;
    }

    boolean hasNext() {
      return index < length;
    }

    Argument next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      try {
        Type parameterType = ReflectUtils.resolveType(types[index]);
        if (parameterType instanceof ParameterizedType pt) {
          parameterType = pt.getActualTypeArguments()[0];
        }
        ValueConverter<?> converter = valueConverterService.getConverter(parameterType);
        return Argument.newInstance(annotations[index], converter);
      } catch (IllegalStateException ex) {
        throw new IllegalStateException(format("could not instantiate parameter of %s.%s with index %s because of %s"
            , method.getDeclaringClass().getSimpleName(), method.getName(), index, ex.getMessage()));
      } finally {
        index++;
      }
    }
  }

  private static final Function<ResourceContext, Continuation<ResourceContext>> GENERAL_STAGE = new GeneralStage();

  public static List<Argument> newInstances(Method method) {
    ResourceContext resourceContext = new ResourceContext(method);
    if (resourceContext.hasNext()) {
      var stage        = GENERAL_STAGE;
      var continuation = Continuation.of(resourceContext, stage);
      while (continuation.hasNext()) {
        stage        = continuation.next();
        continuation = stage.apply(continuation.result());
      }
    }
    return Collections.unmodifiableList(resourceContext.getArguments());
  }

  static <T> Argument newInstance(Annotation[] annotations, ValueConverter<T> converter) {
    Arg    arg  = null;
    String name = null;
    for (Annotation annotation : annotations) {
      if (Arg.class.equals(annotation.annotationType())) {
        arg  = (Arg) annotation;
        name = ((Arg) annotation).name();
      }
    }

    if (arg == null) {
      throw new IllegalStateException("method attribute should have @Arg annotations");
    }

    return new Argument(name, arg, converter);
  }

  @Override public String toString() {
    return "Argument{name:" + name + "}";
  }
}
