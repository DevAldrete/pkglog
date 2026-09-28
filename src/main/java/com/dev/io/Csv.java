package com.dev.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 style CSV helper: quotes fields that need it and parses
 * quoted fields (including doubled quotes) back. Kept dependency-free so bulk
 * import/export works from any spreadsheet.
 */
public final class Csv {

  private Csv() {
  }

  /** Formats one row, escaping every field. */
  public static String line(Object... values) {
    StringBuilder row = new StringBuilder();

    for (int index = 0; index < values.length; index++) {
      if (index > 0) {
        row.append(',');
      }

      row.append(escape(values[index]));
    }

    return row.toString();
  }

  public static String escape(Object value) {
    String text = value == null ? "" : String.valueOf(value);

    if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0) {
      return '"' + text.replace("\"", "\"\"") + '"';
    }

    return text;
  }

  /** Parses a whole document into rows of fields, ignoring trailing blanks. */
  public static List<String[]> parse(String content) {
    List<String[]> rows = new ArrayList<>();
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;

    for (int index = 0; index < content.length(); index++) {
      char character = content.charAt(index);

      if (quoted) {
        if (character == '"') {
          if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
            field.append('"');
            index++;
          } else {
            quoted = false;
          }
        } else {
          field.append(character);
        }
      } else if (character == '"') {
        quoted = true;
      } else if (character == ',') {
        fields.add(field.toString());
        field.setLength(0);
      } else if (character == '\n') {
        fields.add(field.toString());
        field.setLength(0);
        rows.add(fields.toArray(new String[0]));
        fields = new ArrayList<>();
      } else if (character != '\r') {
        field.append(character);
      }
    }

    if (field.length() > 0 || !fields.isEmpty()) {
      fields.add(field.toString());
      rows.add(fields.toArray(new String[0]));
    }

    return rows;
  }
}
