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

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Protobuf message formatter.
 */
public enum ProtoFormatter {
  ;

  private static final class Cell {
    private final String[] lines;
    private final int      maxCellWidth;

    Cell(String[] lines) {
      this.lines = lines;
      int width = 0;
      for (String line : lines) {
        if (line != null && line.length() > width) {
          width = line.length();
        }
      }
      maxCellWidth = width;
    }

    Optional<String> line(int number) {
      if (number < 0 || number >= lines.length) {
        return Optional.empty();
      }
      return Optional.of(lines[number]);
    }

    String[] lines() {
      return lines;
    }

    int maxCellWidth() {
      return maxCellWidth;
    }
  }

  private static final class Row {
    private final Cell[] cells;
    private final int    maxRowHeight;

    Row(Cell[] cells) {
      this.cells = cells;
      int height = 0;
      for (Cell cell : cells) {
        if (cell != null && cell.lines().length > height) {
          height = cell.lines().length;
        }
      }
      maxRowHeight = height;
    }

    Cell[] cells() {
      return cells;
    }

    int maxRowHeight() {
      return maxRowHeight;
    }
  }

  public static void formatAsTable(List<? extends GeneratedMessage> values, Consumer<String> consumer) {
    if (values.isEmpty()) {
      consumer.accept("empty set");
      return;
    }

    List<Row> rows = splitToRows(values);

    int[]         widthList = maxColumnWidthList(rows);
    StringBuilder border    = new StringBuilder();

    for (int width : widthList) {
      border.append("+");
      border.append("-".repeat(width + 2));
    }
    border.append("+");

    StringBuilder format = new StringBuilder("| %-");
    for (int j = 0; j < widthList.length; j++) {
      format.append(widthList[j]);
      if (j == widthList.length - 1) {
        format.append("s |");
      } else {
        format.append("s | %-");
      }
    }

    consumer.accept(border.toString());
    for (Row row : rows) {
      for (int i = 0; i < row.maxRowHeight(); i++) {
        consumer.accept(String.format(format.toString(), (Object[]) getValues(row, i, widthList.length)));
      }
      consumer.accept(border.toString());
    }
  }

  private static String[] getValues(Row message, int line, int minLength) {
    String[] res = Arrays.stream(message.cells()).map(f -> f.line(line).orElse("")).toArray(String[]::new);
    if (res.length < minLength) {
      String[] padded = new String[minLength];
      System.arraycopy(res, 0, padded, 0, res.length);
      for (int i = res.length; i < padded.length; i++) {
        padded[i] = "";
      }
      return padded;
    } else {
      return res;
    }
  }

  private static int[] maxColumnWidthList(List<Row> rows) {
    int[] result = new int[rows.getFirst().cells().length];

    for (Row row : rows) {
      int i = 0;
      for (Cell cell : row.cells()) {
        if (cell.maxCellWidth() > result[i]) {
          result[i] = cell.maxCellWidth();
        }
        i++;
      }
    }
    return result;
  }

  private static List<Row> splitToRows(List<? extends GeneratedMessage> keys) {
    List<Row> rows = new ArrayList<>();

    Message message     = keys.getFirst();
    int     columnCount = message.getDescriptorForType().getFields().size();

    // add header row
    Cell[] cells = message.getDescriptorForType().getFields().stream().map(f -> new Cell(new String[] {f.getName()})).toArray(Cell[]::new);
    rows.add(new Row(cells));

    for (Message key : keys) {
      cells = new Cell[columnCount];
      int i = 0;
      for (Descriptors.FieldDescriptor descriptor : key.getDescriptorForType().getFields()) {
        String value = key.getField(descriptor).toString();
        cells[i++] = new Cell(value.split("\n|\r\n|\r"));
      }
      rows.add(new Row(cells));
    }
    return rows;
  }
}
