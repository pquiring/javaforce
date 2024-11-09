package javaforce;

/**
*
* JSON parser (JavaScript Object Notation)
*
* https://en.wikipedia.org/wiki/JSON
*
* Does NOT Support multi-dimension arrays.
*
*/

import java.util.*;
import java.io.*;

/*

example:

{
  "key": "value",
  "key2" : ["v1", "v2"],
  "key3" : {
    "key" : "value",
    "etc" : "value"
  }
  "key4" : [ "v1", "v2" ],
  "key5" : [ { "t1": "v1"}, {"t2": "v2", "t2b": "v2b"}]
}

*/

public class JSON {
  public static boolean debug = false;
  public static class Element {
    public String key;
    public String value;
    public ArrayList<Element> children = new ArrayList<Element>();

    public Element getChild(String name) {
      for(Element child : children) {
        if (child.key.equals(name)) {
          return child;
        }
      }
      if (debug) JFLog.log("JSON:Child not found:" + name);
      return null;
    }
    public Element getChild(int idx) {
      return children.get(idx);
    }
    public int getChildCount() {
      return children.size();
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append(key + "=" + value);
      if (children.size() > 0) {
        sb.append("[");
        boolean first = true;
        for(Element child : children) {
          if (first) {
            first = false;
          } else {
            sb.append(",");
          }
          sb.append(child.toString());
        }
        sb.append("]");
      }
      sb.append("}\r\n");
      return sb.toString();
    }

    public void print() {
      System.out.println(toString());
    }
  }
  /** Parses a JSON string. */
  public static Element parse(String str) throws Exception {
    Element root = new Element();
    root.key = "root";
    JSON json = new JSON();
    json.json = str.trim().toCharArray();
    json.len = json.json.length;
    json.readElement(root);
    return root;
  }
  /** Parses a JSON string from InputStream. */
  public static Element parseStream(InputStream is) throws Exception {
    try {
      byte[] data = is.readAllBytes();
      return parse(new String(data, "UTF-8"));
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
  /** Parses a JSON string from a File. */
  public static Element parseFile(String file) throws Exception {
    try {
      FileInputStream fis = new FileInputStream(file);
      Element root = parseStream(fis);
      fis.close();
      return root;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
  private char[] json;
  private int len;
  private int pos;
  private void trim() {
    while (pos < len && Character.isWhitespace(json[pos])) {
      pos++;
    }
  }
  private void readKey(Element e) throws Exception {
    if (debug) JFLog.log("readKey:pos=" + pos);
    boolean quote = false;
    e.key = "";
    while (true) {
      char ch = json[pos++];
      if (ch == '\"') {
        if (quote) {
          quote = false;
        } else {
          if (e.key.length() > 0) throw new Exception("bad key name:" + e.key + ":pos=" + pos);
          quote = true;
        }
        continue;
      }
      if (quote) {
        e.key += ch;
      } else {
        switch (ch) {
          case ' ':
          case '\r':
          case '\n':
          case '\t':
          case ',':
            if (e.key.length() > 0) return;
            break;
          case '}':
            pos--;
            return;
          case ':':
            if (e.key.length() == 0) throw new Exception("no key name:pos=" + pos);
            pos--;
            return;
          case '{':
          case '[':
          case ']':
            throw new Exception("bad key name:" + ch + ":" + e.key + ":pos=" + pos);
          default:
            e.key += ch;
        }
      }
    }
  }
  private void readArray(Element parent) throws Exception {
    if (debug) JFLog.log("readArray [");
    char open = readToken();
    if (open != '[') {
      throw new Exception("bad array:pos=" + pos);
    }
    while (true) {
      trim();
      char ch = json[pos];
      if (ch == ']') break;
      Element child = new Element();
      child.key = "";
      readValue(child, true);
      if ((child.value.length() > 0) || (child.children.size() > 0)) {
        if (child.value.length() > 0) {
          if (debug) JFLog.log("value=" + child.value);
        }
        parent.children.add(child);
      }
      char next = readToken();
      if (next == ']') break;
      if (next == ',') continue;
      throw new Exception("bad array:pos=" + pos);
    }
    if (debug) JFLog.log("] //end of array");
  }
  private void readValue(Element e, boolean array) throws Exception {
    if (debug) JFLog.log("readValue:array=" + array + ":pos=" + pos);
    boolean quote = false, escape = false;
    e.value = "";
    while (true) {
      trim();
      char ch = json[pos++];
      if ((ch == '\"') && (!escape)) {
        if (quote) {
          return;
        } else {
          if (e.value.length() > 0) throw new Exception("bad value:pos=" + pos);
          quote = true;
        }
        continue;
      }
      if ((ch == '\\') && (!escape)) {
        escape = true;
      }
      if (quote) {
        if (escape) {
          switch (ch) {
            case 'n': e.value += "\n"; break;
            case 'r': e.value += "\r"; break;
            case 't': e.value += "\t"; break;
            case 'b': e.value += "\b"; break;
            case 'f': e.value += "\f"; break;
            default:  e.value += ch; break;
          }
          escape = false;
        } else {
          e.value += ch;
        }
      } else {
        switch (ch) {
          case ',':
            pos--;
            return;
          case ':':
            if (e.value.length() > 0) throw new Exception("bad value:pos=" + pos);
            break;
          case '{':
            if (e.value.length() > 0) throw new Exception("bad value:pos=" + pos);
            pos--;
            readElement(e);
            return;
          case '}':
            if (array) throw new Exception("bad array:pos=" + pos);
            pos--;
            return;
          case '[':
            if (e.value.length() > 0) throw new Exception("bad value:pos=" + pos);
            pos--;
            readArray(e);
            return;
          case ']':
            if (!array) throw new Exception("bad array:pos=" + pos);
            pos--;
            return;
          default: {
            if (escape) {
              switch (ch) {
                case 'n': e.value += "\n"; break;
                case 'r': e.value += "\r"; break;
                case 't': e.value += "\t"; break;
                case 'b': e.value += "\b"; break;
                case 'f': e.value += "\f"; break;
                default:  e.value += ch; break;
              }
              escape = false;
            } else {
              e.value += ch;
            }
          }
        }
      }
    }
  }
  private char readToken() throws Exception {
    trim();
    return json[pos++];
  }
  private void readElement(Element e) throws Exception {
    //str = { ... }
    if (debug) JFLog.log("readElement {");
    char open = readToken();
    if (open != '{') {
      throw new Exception("bad element:pos=" + pos);
    }
    while (pos < len) {
      trim();
      char ch = json[pos];
      if (ch == '}') break;
      Element child = new Element();
      readKey(child);
      if (debug) JFLog.log("key=" + child.key);
      char eq = readToken();
      if (eq != ':') {
        throw new Exception("bad element:pos=" + pos);
      }
      readValue(child, false);
      if (debug) JFLog.log("value=" + child.value);
      e.children.add(child);
      char next = readToken();
      if (next == '}') break;
      if (next == ',') continue;
      throw new Exception("bad element:next=" + next + ":pos=" + pos);
    }
    if (debug) JFLog.log("} //end of element");
  }
  /** Escape string to valid json text. */
  public static String escape(String in) {
    if (in == null) return "null";
    StringBuilder sb = new StringBuilder();
    char ca[] = in.toCharArray();
    for(char ch : ca) {
      if (ch < ' ') {
        sb.append(String.format("\\u%04x", (int)ch));  //UTF-16
      } else if (ch == '"') {
        sb.append("\\\"");
      } else if (ch == '\\') {
        sb.append("\\\\");
      } else {
        sb.append(ch);
      }
    }
    return sb.toString();
  }
/* //remove JSON to XML support for now
  public static XML toXML(Element e) {
    XML xml = new XML();
    xml.root.setName(e.key);
    xml.root.setContent(e.value);
    addXML(e, xml, xml.root);
    return xml;
  }
  private static void addXML(Element e, XML xml, XML.XMLTag tag) {
    for(int a=0;a<e.children.size();a++) {
      Element c = e.children.get(a);
      XML.XMLTag child = xml.addTag(tag, c.key, "", c.value);
      addXML(c, xml, child);
    }
  }
*/
  public static void main(String[] args) {
    try {
      if (args.length > 1) {
        if (args[1].equals("debug")) {
          JSON.debug = true;
        }
      }
      Element test = JSON.parseFile(args[0]);
      test.print();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
