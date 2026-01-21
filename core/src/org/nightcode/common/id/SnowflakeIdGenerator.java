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

package org.nightcode.common.id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IdGenerator implementation based on Snowflake ID approach.
 * +------+----------------------+--------------+-----------+
 * | sign | time in milliseconds |   shard id   | sequence  |
 * +------+----------------------+--------------+-----------+
 * | 1bit |        41bits        |    10bits    |   12bits  |
 * +------+----------------------+--------------+-----------+
 */
public class SnowflakeIdGenerator implements IdGenerator {

  private static final long EPOCH = LocalDateTime.of(LocalDate.of(2010, 1, 1), LocalTime.MIDNIGHT)
      .atZone(ZoneId.of("UTC"))
      .toInstant()
      .toEpochMilli();

  private static final int SHARD_BITS    = 10;
  private static final int SEQUENCE_BITS = 12;

  private final int timestampShift;
  private final int shardShift;
  private final int sequenceMod;

  private final long shardId;

  private final AtomicInteger sequence = new AtomicInteger(0);

  public SnowflakeIdGenerator(long shardId) {
    if (shardId > ~(-1L << SHARD_BITS)) {
      throw new IllegalArgumentException("illegal shardId value: " + shardId + ", should be less than " + SHARD_BITS + "bits number");
    }
    this.shardId        = shardId;
    this.timestampShift = SHARD_BITS + SEQUENCE_BITS;
    this.shardShift     = SEQUENCE_BITS;
    this.sequenceMod    = 1 << SEQUENCE_BITS;
  }

  @Override public long nextId() {
    long now        = System.currentTimeMillis();
    long sequenceId = sequence.incrementAndGet() % sequenceMod;

    return (now - EPOCH) << timestampShift | (shardId << shardShift) | sequenceId;
  }
}
