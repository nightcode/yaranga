/*
 * Copyright (C) 2012 The NightCode Open Source Project
 *
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

package org.nightcode.common.util.monitoring;

import org.nightcode.common.annotations.Beta;
import org.nightcode.common.base.Objects;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An object that measures the magnitude of speed.
 */
@Beta
public final class Speedometer {

  // Constants for conversion
  private static final long C0 = 1000L;
  private static final long C1 = C0 * 1000L;

  private static final long ZERO_TIME = 0L;
  private static final double ZERO_UNITS = 0.0;

  private final long averagingPeriod;
  private final ReentrantLock lock = new ReentrantLock();

  private volatile double cachedSpeed = 0.0;
  private volatile long elapsedTime = ZERO_TIME;
  private volatile long lastUpdateTime = System.nanoTime();
  private volatile double quantity = ZERO_UNITS;

  /**
   * Creates a new speedometer, using the predefined averaging period equals to 1 second.
   */
  public Speedometer() {
    this(1L, TimeUnit.SECONDS);
  }

  /**
   * Creates a new speedometer, using the specified averaging period.
   *
   * @param averagingPeriod the averaging period
   * @param timeUnit the time unit of the averaging period argument
   */
  public Speedometer(long averagingPeriod, TimeUnit timeUnit) {
    Objects.validArgument(averagingPeriod > 0L, "averaging period <%s> must be greater than 0"
        , averagingPeriod);
    java.util.Objects.requireNonNull(timeUnit, "time unit");
    this.averagingPeriod = timeUnit.toMicros(averagingPeriod);
  }

  /**
   * Returns the value of current speed (units per second).
   *
   * @return the value of current speed
   */
  public double getSpeed() {
    return update(ZERO_UNITS);
  }

  /**
   * Updates speed value.
   *
   * @param units units
   * @return the current speed value (units per second)
   */
  public double update(final double units) {
    final double speed;
    lock.lock();
    try {
      final long currentTime = System.nanoTime();
      final long timeDifference = (currentTime - lastUpdateTime) / C1; // nanoseconds to micros
      if (timeDifference >= averagingPeriod) {
        speed = units / averagingPeriod;
        cachedSpeed = speed;
        lastUpdateTime = currentTime;
        elapsedTime = ZERO_TIME;
        quantity = ZERO_UNITS;
      } else {
        if (timeDifference > ZERO_TIME) {
          lastUpdateTime = currentTime;
          elapsedTime += timeDifference;
        }
        if (units != ZERO_UNITS) {
          quantity += units;
        }
        if (elapsedTime >= averagingPeriod) {
          speed = quantity / elapsedTime;
          cachedSpeed = speed;
          elapsedTime = ZERO_TIME;
          quantity = ZERO_UNITS;
        } else {
          speed = (cachedSpeed * (averagingPeriod - elapsedTime) + quantity) / averagingPeriod;
        }
      }
    } finally {
      lock.unlock();
    }
    return speed * C0; // units per micro to units per second
  }
}
