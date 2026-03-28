/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nightcode.common.logging.opentelemetry.appender;

import java.util.List;
import java.util.function.Supplier;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.Nullable;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitOldCodeSemconv;
import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableCodeSemconv;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * @param <T> context data
 */
public final class LogEventMapper<T> {

  private static final AttributeKey<String> CODE_FILEPATH      = AttributeKey.stringKey("code.filepath");
  public static final  AttributeKey<String> CODE_FILE_PATH     = AttributeKey.stringKey("code.file.path");
  private static final AttributeKey<String> CODE_FUNCTION      = AttributeKey.stringKey("code.function");
  public static final  AttributeKey<String> CODE_FUNCTION_NAME = AttributeKey.stringKey("code.function.name");
  private static final AttributeKey<String> CODE_NAMESPACE     = AttributeKey.stringKey("code.namespace");
  private static final AttributeKey<Long>   CODE_LINENO        = AttributeKey.longKey("code.lineno");
  public static final  AttributeKey<Long>   CODE_LINE_NUMBER   = AttributeKey.longKey("code.line.number");
  private static final AttributeKey<Long>   THREAD_ID          = AttributeKey.longKey("thread.id");
  private static final AttributeKey<String> THREAD_NAME        = AttributeKey.stringKey("thread.name");
  private static final AttributeKey<String> EVENT_NAME         = AttributeKey.stringKey("event.name");
  private static final AttributeKey<String> OTEL_EVENT_NAME    = AttributeKey.stringKey("otel.event.name");

  private static final String SPECIAL_MAP_MESSAGE_ATTRIBUTE = "message";

  private static final Cache<String, AttributeKey<String>> CONTEXT_DATA_ATTRIBUTE_KEY_CACHE = Cache.bounded(100);
  private static final Cache<String, AttributeKey<String>> MAP_MESSAGE_ATTRIBUTE_KEY_CACHE  = Cache.bounded(100);

  private static final AttributeKey<String> LOG_MARKER = AttributeKey.stringKey("log4j.marker");

  private final ContextDataAccessor<T> contextDataAccessor;

  private final boolean      captureExperimentalAttributes;
  private final boolean      captureCodeAttributes;
  private final boolean      captureMapMessageAttributes;
  private final boolean      captureMarkerAttribute;
  private final List<String> captureContextDataAttributes;
  private final boolean      captureAllContextDataAttributes;
  private final boolean      captureEventName;

  public LogEventMapper(
      ContextDataAccessor<T> contextDataAccessor,
      boolean captureExperimentalAttributes,
      boolean captureCodeAttributes,
      boolean captureMapMessageAttributes,
      boolean captureMarkerAttribute,
      List<String> captureContextDataAttributes,
      boolean captureEventName) {
    this.contextDataAccessor             = contextDataAccessor;
    this.captureCodeAttributes           = captureCodeAttributes;
    this.captureExperimentalAttributes   = captureExperimentalAttributes;
    this.captureMapMessageAttributes     = captureMapMessageAttributes;
    this.captureMarkerAttribute          = captureMarkerAttribute;
    this.captureAllContextDataAttributes = captureContextDataAttributes.size() == 1 && captureContextDataAttributes.getFirst().equals("*");
    this.captureContextDataAttributes    = captureContextDataAttributes;
    this.captureEventName                = captureEventName;
  }

  /**
   * Map the {@link LogEvent} data model onto the {@link LogRecordBuilder}. Unmapped fields include:
   *
   * <ul>
   *   <li>Fully qualified class name - {@link LogEvent#getLoggerFqcn()}
   *   <li>Thread priority - {@link LogEvent#getThreadPriority()}
   *   <li>Nested diagnostic context - {@link LogEvent#getContextStack()}
   * </ul>
   */
  @SuppressWarnings("TooManyParameters")
  public void mapLogEvent(
      LogRecordBuilder builder,
      Message message,
      Level level,
      @Nullable Marker marker,
      @Nullable Throwable throwable,
      T contextData,
      String threadName,
      long threadId,
      Supplier<StackTraceElement> sourceSupplier,
      Context context) {
    // Event name priority (last writer wins): MapMessage > context data.
    // Sources are called in ascending priority order.
    captureContextDataAttributes(builder, contextData);

    captureMessage(builder, message);

    if (captureMarkerAttribute) {
      if (marker != null) {
        String markerName = marker.getName();
        builder.setAttribute(LOG_MARKER, markerName);
      }
    }

    if (level != null) {
      builder.setSeverity(levelToSeverity(level));
      builder.setSeverityText(level.name());
    }

    if (throwable != null) {
      builder.setException(throwable);
    }

    if (captureExperimentalAttributes) {
      builder.setAttribute(THREAD_NAME, threadName);
      builder.setAttribute(THREAD_ID, threadId);
    }

    if (captureCodeAttributes) {
      StackTraceElement source = sourceSupplier.get();
      if (source != null) {
        String fileName = source.getFileName();
        if (fileName != null) {
          if (emitStableCodeSemconv()) {
            builder.setAttribute(CODE_FILE_PATH, fileName);
          }
          if (emitOldCodeSemconv()) {
            builder.setAttribute(CODE_FILEPATH, fileName);
          }
        }
        if (emitStableCodeSemconv()) {
          builder.setAttribute(CODE_FUNCTION_NAME, source.getClassName() + "." + source.getMethodName());
        }
        if (emitOldCodeSemconv()) {
          builder.setAttribute(CODE_NAMESPACE, source.getClassName());
          builder.setAttribute(CODE_FUNCTION, source.getMethodName());
        }

        int lineNumber = source.getLineNumber();
        if (lineNumber > 0) {
          if (emitStableCodeSemconv()) {
            builder.setAttribute(CODE_LINE_NUMBER, (long) lineNumber);
          }
          if (emitOldCodeSemconv()) {
            builder.setAttribute(CODE_LINENO, (long) lineNumber);
          }
        }
      }
    }

    builder.setContext(context);
  }

  // visible for testing
  void captureMessage(LogRecordBuilder builder, Message message) {
    if (message == null) {
      return;
    }
    if (!(message instanceof MapMessage<?, ?> mapMessage)) {
      builder.setBody(message.getFormattedMessage());
      return;
    }

    String  body                            = mapMessage.getFormat();
    boolean checkSpecialMapMessageAttribute = (body == null || body.isEmpty());
    if (checkSpecialMapMessageAttribute) {
      body = mapMessage.get(SPECIAL_MAP_MESSAGE_ATTRIBUTE);
    }

    if (body != null && !body.isEmpty()) {
      builder.setBody(body);
    }

    String eventName = mapMessage.get(OTEL_EVENT_NAME.getKey());
    if (eventName != null) {
      builder.setEventName(eventName);
    }

    if (captureMapMessageAttributes) {
      // TODO (trask) this could be optimized in 2.9 and later by calling MapMessage.forEach()
      mapMessage
          .getData()
          .forEach(
              (key, value) -> {
                if (value != null
                    && !key.equals(OTEL_EVENT_NAME.getKey())
                    && (!checkSpecialMapMessageAttribute
                    || !key.equals(SPECIAL_MAP_MESSAGE_ATTRIBUTE))) {
                  builder.setAttribute(getMapMessageAttributeKey(key), value.toString());
                }
              });
    }
  }

  // visible for testing
  void captureContextDataAttributes(LogRecordBuilder builder, T contextData) {

    // otel.event.name takes priority over event.name
    String otelEventName = contextDataAccessor.getValue(contextData, OTEL_EVENT_NAME.getKey());
    if (otelEventName != null) {
      builder.setEventName(otelEventName);
    } else if (captureEventName) {
      String eventName = contextDataAccessor.getValue(contextData, EVENT_NAME.getKey());
      if (eventName != null) {
        builder.setEventName(eventName);
      }
    }

    if (captureAllContextDataAttributes) {
      contextDataAccessor.forEach(
          contextData,
          (key, value) -> {
            if (!OTEL_EVENT_NAME.getKey().equals(key) && !(captureEventName && EVENT_NAME.getKey().equals(key))) {
              if (value != null) {
                builder.setAttribute(getContextDataAttributeKey(key), value);
              }
            }
          });
      return;
    }

    for (String key : captureContextDataAttributes) {
      if (!OTEL_EVENT_NAME.getKey().equals(key) && !(captureEventName && EVENT_NAME.getKey().equals(key))) {
        String value = contextDataAccessor.getValue(contextData, key);
        if (value != null) {
          builder.setAttribute(getContextDataAttributeKey(key), value);
        }
      }
    }
  }

  public static AttributeKey<String> getContextDataAttributeKey(String key) {
    return CONTEXT_DATA_ATTRIBUTE_KEY_CACHE.computeIfAbsent(key, AttributeKey::stringKey);
  }

  public static AttributeKey<String> getMapMessageAttributeKey(String key) {
    return MAP_MESSAGE_ATTRIBUTE_KEY_CACHE.computeIfAbsent(key, k -> AttributeKey.stringKey("log4j.map_message." + k));
  }

  private static Severity levelToSeverity(Level level) {
    return switch (level.getStandardLevel()) {
      case ALL, TRACE -> Severity.TRACE;
      case DEBUG -> Severity.DEBUG;
      case INFO -> Severity.INFO;
      case WARN -> Severity.WARN;
      case ERROR -> Severity.ERROR;
      case FATAL -> Severity.FATAL;
      case OFF -> Severity.UNDEFINED_SEVERITY_NUMBER;
    };
  }
}
