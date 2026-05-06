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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link SnowflakeIdGenerator}.
 */
public class SnowflakeIdGeneratorTest {

  @Test public void nextId() {
    IdGenerator generator = new SnowflakeIdGenerator(1);

    long id1 = generator.nextId();
    long id2 = generator.nextId();

    assertNotEquals(id1, id2);
  }

  @Test public void wrongShardId() {
    try {
      new SnowflakeIdGenerator(Long.MAX_VALUE);
      fail("should throw IllegalArgumentException");
    } catch (Exception ex) {
      assertInstanceOf(IllegalArgumentException.class, ex);
      assertEquals("illegal shardId value: " + Long.MAX_VALUE + ", should be less than 10bits number", ex.getMessage());
    }
  }
}
