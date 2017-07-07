package jfcontrols.tags;

/** Tags cache
 *
 * Each function and panel has it's own tag cache.
 * IndexTags @nnn are unique to each tag cache.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

public class TagsCache {
  private IndexTags it = new IndexTags();

  public TagBase getTag(String name) {
    TagAddr ta = decode(name);
    return getTag(ta);
  }

  public TagBase getTag(TagAddr ta) {
    if (ta == null) return null;
    char ch = ta.name.charAt(0);
    if (ch == '@') {
      int idx = Integer.valueOf(ta.name.substring(1));
      if (it == null) return null;
      return it.getTag(idx);
    }
    if ((ch >= '0' && ch <= '9') || ch == '-') {
      if (ta.name.indexOf(".") == -1)
        return new TagTemp(TagType.int32, ta.name);
      else
        return new TagTemp(TagType.float32, ta.name);
    }
    if (ta.cid == 0) {
      TagBase tag = TagsService.getLocalTag(ta.name);
      TagBase ret = tag;
      if (tag == null) {
        JFLog.log("Error:Unable to find local tag:" + ta.name);
        return null;
      }
      if (tag.isArray() && ta.idx != -1) {
        ret = tag.getIndex(ta.idx);
      }
      if (tag.isUDT() && ta.member != null) {
        int mid = tag.getMember(ta.member);
        if (mid == -1) return null;
        ret = ret.getMember(mid);
        if (tag.isArray() && ta.midx != -1) {
          ret = ret.getIndex(ta.midx);
        }
      }
      return ret;
    } else {
      TagBase tag = TagsService.getRemoteTag("c" + ta.cid + "#" + ta.name);
      if (tag == null) {
        JFLog.log("Error:Unable to find remote tag:" + ta.name + ":cid=" + ta.cid);
        return null;
      }
      return tag;
    }
  }

  public String read(String name) {
    TagAddr ta = decode(name);
    TagBase tag = getTag(ta);
    return tag.getValue();
  }

  public void write(String name, String value) {
    TagAddr ta = decode(name);
    TagBase tag = getTag(ta);
    tag.setValue(value);
  }

  private boolean isFloat(String str) {
    int len = str.length();
    boolean dot = false;
    for(int a=0;a<len;a++) {
      char ch = str.charAt(a);
      if (a == 0 && ch == '-') continue;
      if (ch == '.') {
        if (dot) return false;
        dot = true;
        continue;
      }
      if (ch < '0' || ch > '9') return false;
    }
    return true;
  }

  private boolean isInteger(String str) {
    int len = str.length();
    for(int a=0;a<len;a++) {
      char ch = str.charAt(a);
      if (a == 0 && ch == '-') continue;
      if (ch < '0' || ch > '9') return false;
    }
    return true;
  }

  public TagAddr decode(String addr) {
    // addr = {c# '#'} name {[idx]} {. member {[idx]}}
    // {} = optional
    if (addr == null) return null;
    TagAddr ta = new TagAddr();
    int idx;
    char ch;
    idx = addr.indexOf('#');
    if (idx == 0) {
      idx = -1;
      addr = addr.substring(1);
    }
    if (idx != -1) {
      if (addr.charAt(0) != 'c') {
        JFLog.log("Error:Invalid Tag:" + addr);
        return null;
      }
      String cid = addr.substring(1, idx);
      if (cid.length() == 0) {
        JFLog.log("Error:Invalid Tag:" + addr);
        return null;
      }
      ta.cid = Integer.valueOf(cid);
      addr = addr.substring(idx+1);
      if (ta.cid > 0) {
        //TODO : support remote arrays if type=JFC
        ta.name = addr;
        return ta;
      }
    }
    idx = -1;
    int len = addr.length();
    if (len == 0) return null;
    boolean inidx = false;
    for(int a=0;a<len;a++) {
      ch = addr.charAt(a);
      switch (ch) {
        case '[': if (inidx) return null; inidx = true; break;
        case ']': if (!inidx) return null; inidx = false; break;
        case '.': if (!inidx) {if (idx != -1) return null; idx = a;} break;
      }
    }
    if (idx != -1) {
      ta.name = addr.substring(0, idx);
      ta.member = addr.substring(idx + 1);
    } else {
      ta.name = addr;
    }
    idx = ta.name.indexOf('[');
    if (idx != -1) {
      if (ta.name.charAt(ta.name.length() - 1) != ']') {
        return null;
      }
      String tagidx = ta.name.substring(idx+1, ta.name.length() - 1);
      if (tagidx.length() == 0) return null;
      if (tagidx.charAt(0) == '@') {
        ta.idx = it.getIndex(Integer.valueOf(tagidx.substring(1)));
      } else {
        ch = tagidx.charAt(0);
        if (ch >= '0' && ch <= '9') {
          ta.idx = Integer.valueOf(tagidx);
        } else {
          TagBase tag = getTag(tagidx);
          if (tag == null) return null;
          ta.idx = tag.getInt();
        }
      }
      ta.name = ta.name.substring(0, idx);
    }
    if (ta.member != null) {
      idx = ta.member.indexOf('[');
      if (idx != -1) {
        if (ta.member.charAt(ta.member.length() - 1) != ']') {
          return null;
        }
        String tagidx = ta.member.substring(idx+1, ta.member.length() - 1);
        if (tagidx.length() == 0) return null;
        if (tagidx.charAt(0) == '@') {
          ta.midx = it.getIndex(Integer.valueOf(tagidx.substring(1)));
        } else {
          ch = tagidx.charAt(0);
          if (ch >= '0' && ch <= '9') {
            ta.midx = Integer.valueOf(tagidx);
          } else {
            TagBase tag = getTag(tagidx);
            if (tag == null) return null;
            ta.midx = tag.getInt();
          }
        }
        ta.member = ta.member.substring(0, idx);
      }
    }
    //validate tag
    if (ta.name.length() == 0) return null;
    ch = Character.toLowerCase(ta.name.charAt(0));
/*
    if ((ch < 'a' || ch > 'z') && (ch != '_')) {
      return null;
    }
*/
    if (ta.member != null) {
      if (ta.member.length() == 0) return null;
      ch = Character.toLowerCase(ta.member.charAt(0));
/*
      if ((ch < 'a' || ch > 'z') && (ch != '_')) {
        return null;
      }
*/
    }
    return ta;
  }

  /** Clears Index Tags. */
  public void clear() {
    it.clear();
  }
}
