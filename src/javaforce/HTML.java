package javaforce;

/** HTML.
 *
 * Helper functions for generating HTML.
 *
 * @author pquiring
 */

public class HTML {

  public static final boolean debug = false;

  /** Generate html.form.input
   * @param name = element name (optional)
   * @param id = element id (optional)
   * @param value = default value (optional)
   */
  public static String input_field(String name, String id, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append("<input");
    if (name != null) {
      sb.append(" name='");
      sb.append(name);
      sb.append("'");
    }
    if (id != null) {
      sb.append(" id='");
      sb.append(id);
      sb.append("'");
    }
    if (value != null) {
      sb.append(" value='");
      sb.append(value);
      sb.append("'");
    }
    sb.append(">");
    return sb.toString();
  }

  /** Generate html.form.input[type=checkbox]
   * @param name = element name (optional)
   * @param id = element id (optional)
   * @param checked = checkbox is initial checked
   */
  public static String input_checkbox(String name, String id, boolean checked) {
    StringBuilder sb = new StringBuilder();
    sb.append("<input type=checkbox");
    if (name != null) {
      sb.append(" name='");
      sb.append(name);
      sb.append("'");
    }
    if (id != null) {
      sb.append(" id='");
      sb.append(id);
      sb.append("'");
    }
    if (checked) sb.append(" checked");
    sb.append(">");
    return sb.toString();
  }

  /** Generate html.form.select.
   * @param name = element name (optional)
   * @param id = element id (optional)
   * @param options = array[value, text]
   * @param selected_value = selected value (optional)
   * @param selected_text = selected text (optional)
   * @param show_default = include a null 'Select...' option
  */
  public static String select(String name, String id, String[][] options, String selected_value, String selected_text, boolean show_default) {
    StringBuilder sb = new StringBuilder();
    sb.append("<select");
    if (name != null) {
      sb.append(" name='");
      sb.append(name);
      sb.append("'");
    }
    if (id != null) {
      sb.append(" id='");
      sb.append(id);
      sb.append("'");
    }
    sb.append(">");
    if (show_default) {
      sb.append("<option value='null'>Select...</option>");
    }
    boolean found = false;
    for(int a=0;a<options.length;a++) {
      sb.append("<option value='");
      sb.append(options[a][0]);
      sb.append("'");
      if (selected_value != null && selected_value.equals(options[a][0])) {
        sb.append(" selected");
        found = true;
      }
      sb.append(">");
      sb.append(options[a][1]);
      sb.append("</option>");
    }
    if (!found && selected_value != null && selected_text != null) {
      sb.append("<option value='");
      sb.append(selected_value);
      sb.append("' selected>");
      sb.append(selected_text);
      sb.append("</option>");
    }
    sb.append("</select>");
    return sb.toString();
  }
  /** Generate html.form.select.
   * Invokes select() with show_default = true
   */
  public static String select(String name, String id, String[][] options, String selected_value, String selected_text) {
    return select(name, id, options, selected_value, selected_text, true);
  }
  /** Generate HTML stack trace of exception.
   */
  public static String toString(Exception ex) {
    StringBuilder sb = new StringBuilder();
    sb.append("Exception:");
    sb.append(ex.toString());
    sb.append("<br>");
    StackTraceElement[] stes = ex.getStackTrace();
    for(StackTraceElement ste : stes) {
      sb.append(ste.toString());
      sb.append("<br>");
    }
    return sb.toString();
  }
  /** Adds css file. */
  public static String addCSSfile(String file) {
    return "<link type='stylesheet' href='" + file + "'>";
  }
  /** Adds css styles inline. */
  public static String addCSSinline(String styles) {
    return "<style>" + styles + "</style>";
  }
  /** Adds js file. */
  public static String addJSfile(String file) {
    return "<script src='" + file + "'></script>";
  }
  /** Adds js code inline. */
  public static String addJSinline(String script) {
    return "<script>" + script + "</script>";
  }

  /** Converts HTML to text/plain. */
  public static String toText(String html) {
    StringBuilder txt = new StringBuilder();
    int html_len = html.length();
    if (debug) JFLog.log("len=" + html_len);
    int html_off = 0;
    while (html_off < html_len) {
      int i1 = html.indexOf('<', html_off);
      if (debug) JFLog.log("i1=" + i1);
      if (i1 == -1) {
        txt.append(html.substring(html_off, html_len - html_off));
        html_off = html_len;
      } else {
        if (i1 > 0) {
          if (debug) JFLog.log("substring=" + html_off + "," + i1);
          txt.append(html.substring(html_off, i1));
        }
        int i2 = html.indexOf('>', i1);
        if (debug) JFLog.log("i2=" + i2);
        if (i2 == -1) {
          JFLog.log("HTML.toText() : tag left open");
          break;
        } else {
          String tag = html.substring(i1 + 1, i2);
          switch (tag) {
            case "br": txt.append("\r\n"); break;
          }
          html_off = i2 + 1;
        }
      }
    }
    return txt.toString();
  }

  public static void main(String[] args) {
    String html = "<h1>This is HTML</h1><br>Converted to text!<br>";
    System.out.println(html);
    String txt = toText(html);
    System.out.println(txt);
  }
}
