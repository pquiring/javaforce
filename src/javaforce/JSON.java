package javaforce;

/**

JSON parser (JavaScript Object Notation)

Does NOT Support multi-dimension arrays.

*/

import java.util.*;

/*

example:

{
  "key": "value",
  "key2" : ["v1", "v2"],
  "key3" : {
    "key" : "value",
    "etc" : "value"
  }
  "key4" : [ "v1", "v2" ]
}

*/

public class JSON {
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
      return null;
    }
  }
  /** Parses a JSON string. */
  public static Element parse(String str) throws Exception {
    Element root = new Element();
    root.key = "root";
    parseElement(root, str.trim());
    return root;
  }
  /* reads key : returns str left over */
  private static String readKey(Element e, String str) throws Exception {
    boolean quote = false;
    int pos = 0;
    e.key = "";
    while (true) {
      char ch = str.charAt(pos++);
      if (ch == '\"') {
        if (quote) {
          quote = false;
        } else {
          if (e.key.length() > 0) throw new Exception("bad key name");
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
            if (e.key.length() > 0) return str.substring(pos);
            break;
          case '}':
            return str.substring(pos-1);
          case ':':
            if (e.key.length() == 0) throw new Exception("no key name");
            return str.substring(pos);
          case '{':
          case '[':
          case ']':
            throw new Exception("bad key name:" + ch);
          default:
            e.key += ch;
        }
      }
    }
  }
  private static String readArray(Element e, String key, String str) throws Exception {
    while (true) {
      Element child = new Element();
      child.key = key;  //repeat key for each array element
      str = readValue(child, str, true);
      if ((child.value.length() > 0) || (child.children.size() > 0)) {
//        JFLog.log(child.key + "=" + child.value);
        e.children.add(child);
      }
      if (str.startsWith("]")) return str.substring(1);
    }
  }
  /* reads value : returns str left over */
  private static String readValue(Element e, String str, boolean array) throws Exception {
    boolean quote = false, escape = false;
    int pos = 0;
    e.value = "";
    while (true) {
      char ch = str.charAt(pos++);
      if ((ch == '\"') && (!escape)) {
        if (quote) {
          return str.substring(pos);
        } else {
          if (e.value.length() > 0) throw new Exception("bad value");
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
          case ' ':
          case '\r':
          case '\n':
          case '\t':
          case ',':
            if (e.value.length() > 0) {
              return str.substring(pos);
            }
            break;
          case ':':
            if (e.value.length() > 0) throw new Exception("bad value");
            break;
          case '{':
            if (e.value.length() > 0) throw new Exception("bad value");
            return parseElement(e, str.substring(pos-1));
          case '}':
            if (e.value.length() > 0) {
              return str.substring(pos);
            }
            throw new Exception("bad value");
          case '[':
            if (e.value.length() > 0) throw new Exception("bad value");
            return str.substring(pos-1);
          case ']':
            if (!array) throw new Exception("bad array");
            return str.substring(pos-1);
          default:
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
  private static String readOpen(String str) throws Exception {
    int pos = 0;
    while (true) {
      char ch = str.charAt(pos++);
      switch (ch) {
        case '{': return str.substring(pos);
        case ' ':
        case 9:
          continue;
      }
      throw new Exception("bad json string");
    }
  }
  private static String parseElement(Element e, String str) throws Exception {
    //str = { ... }
    str = readOpen(str);
    while (str.length() > 0) {
      Element child = new Element();
      str = readKey(child, str);
      if (str.startsWith("}")) return str.substring(1);
      str = readValue(child, str, false);
      if (str.startsWith("[")) {
        str = readArray(e, child.key, str.substring(1));
        continue;
      }
//      JFLog.log(child.key + "=" + child.value);
      e.children.add(child);
    }
    return str;
  }
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
}
