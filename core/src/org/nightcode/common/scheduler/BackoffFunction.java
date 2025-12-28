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

package org.nightcode.common.scheduler;

import org.nightcode.common.scheduler.impl.ArithmeticSchedule;
import org.nightcode.common.scheduler.impl.ExponentialSchedule;
import org.nightcode.common.scheduler.impl.GeometricSchedule;
import org.nightcode.common.scheduler.impl.LinearSchedule;

public enum BackoffFunction {

  LINEAR {
    public RetrySchedule newSchedule(RetryConfig config) {
      return new LinearSchedule(config.getMinDelayMs());
    }
  },
  ARITHMETIC {
    @Override public RetrySchedule newSchedule(RetryConfig config) {
      return new ArithmeticSchedule(config.getMinDelayMs(), config.getMaxDelayMs());
    }
  },
  GEOMETRIC {
    @Override public RetrySchedule newSchedule(RetryConfig config) {
      return new GeometricSchedule(config.getMinDelayMs(), config.getMaxDelayMs());
    }
  },
  EXPONENTIAL {
    @Override public RetrySchedule newSchedule(RetryConfig config) {
      return new ExponentialSchedule(config.getMinDelayMs(), config.getMaxDelayMs());
    }
  };

  public abstract RetrySchedule newSchedule(RetryConfig config);
}
