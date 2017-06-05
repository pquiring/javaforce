package jfcontrols.tags;

/** Tag Address
 *
 * @author pquiring
 */

import javaforce.*;

public class TagAddr {
  public int cid = 0;
  public String name;
  public int idx = 0;
  public String member;
  public int midx = 0;
  public String tempValue;
  public static TagAddr tempValue(String value) {
    TagAddr ta = new TagAddr();
    ta.tempValue = value;
    return ta;
  }
  public static TagAddr decode(String addr, IndexTags it) {
    // addr = {c# '#'} name {[idx]} {. member {[idx]}}
    // {} = optional
    TagAddr ta = new TagAddr();
    int idx;
    idx = addr.indexOf('#');
    if (idx != -1) {
      if (addr.charAt(0) != 'c') {
        JFLog.log("Error:Invalid Tag:" + addr);
        return null;
      }
      ta.cid = Integer.valueOf(addr.substring(1, idx));
      addr = addr.substring(idx+1);
      if (ta.cid > 0) {
        //TODO : support remote arrays if type=JFC
        ta.name = addr;
        return ta;
      }
    }
    idx = addr.indexOf('.');
    if (idx != -1) {
      ta.name = addr.substring(0, idx);
      ta.member = addr.substring(idx + 1);
    } else {
      ta.name = addr;
    }
    idx = ta.name.indexOf('[');
    if (idx != -1) {
      String tagidx = ta.name.substring(idx+1, ta.name.length() - 1);
      if (tagidx.startsWith("[@]")) {
        ta.idx = it.getIndex(Integer.valueOf(tagidx.substring(1)));
      } else {
        ta.idx = Integer.valueOf(tagidx);
      }
      ta.name = ta.name.substring(0, idx);
    }
    if (ta.member != null) {
      idx = ta.member.indexOf('[');
      if (idx != -1) {
        String tagidx = ta.member.substring(idx+1, ta.member.length() - 1);
        if (tagidx.startsWith("[@]")) {
          ta.midx = it.getIndex(Integer.valueOf(tagidx.substring(1)));
        } else {
          ta.midx = Integer.valueOf(tagidx);
        }
        ta.member = ta.member.substring(0, idx);
      }
    }
    return ta;
  }

  public String toString() {
    if (member != null) {
      return "TagAddr:" + name + "[" + idx + "]." + member;
    } else {
      return "TagAddr:" + name + "[" + idx + "]";
    }
  }
}
