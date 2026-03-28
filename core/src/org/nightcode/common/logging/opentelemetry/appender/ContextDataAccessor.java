/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nightcode.common.logging.opentelemetry.appender;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * @param <T> context data
 */
public interface ContextDataAccessor<T> {

  @Nullable String getValue(T contextData, String key);

  void forEach(T contextData, BiConsumer<String, String> action);
}
