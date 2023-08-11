package javaforce.utils;

/** Build Tools
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class BuildTools {
  private XML xml;
  private BuildTools versions;

  public boolean loadXML(String buildfile) {
    xml = new XML();
    try {
      xml.read(new FileInputStream(buildfile));
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return false;
  }

  public String getTag(String name) {
    XML.XMLTag tag = xml.getTag(new String[] {"project", name});
    if (tag == null) return "";
    return tag.content;
  }

  public String getProperty(String name) {
    //<project> <property name="name" value="value">
    int cnt = xml.root.getChildCount();
    for(int a=0;a<cnt;a++) {
      XML.XMLTag tag = xml.root.getChildAt(a);
      if (!tag.name.equals("property")) continue;
      int attrs = tag.attrs.size();
      String attrName = null;
      String attrValue = null;
      for(int b=0;b<attrs;b++) {
        XML.XMLAttr attr = tag.attrs.get(b);
        if (attr.name.equals("name")) {
          attrName = attr.value;
        }
        if (attr.name.equals("value")) {
          attrValue = attr.value;
        }
        if (attr.name.equals("location")) {
          attrValue = attr.value;
        }
      }
      if (attrName != null && attrName.equals(name)) {
        //TODO : expand ${} tags
        if (attrValue.equals("${javaforce-version}")) {
          if (versions == null) {
            versions = new BuildTools();
            versions.loadXML("versions.xml");
          }
          attrValue = versions.getProperty("javaforce-version");
        }
        return attrValue;
      }
    }
    return "";
  }
}
