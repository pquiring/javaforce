package jfcontrols.tags;

import javaforce.JFLog;

/** Tags cache
 *
 * Each function and panel has it's own tag cache.
 * IndexTags @nnn are unique to each tag cache.
 *
 * @author pquiring
 */

public class TagsCache {
  private IndexTags it = new IndexTags();

  public TagBase getTag(String name) {
    TagAddr ta = decode(name);
    return getTag(ta);
  }

  public TagBase getTag(TagAddr ta) {
    if (ta.name.startsWith("[@]")) {
      int idx = Integer.valueOf(ta.name.substring(1));
      if (it == null) return null;
      return it.getTag(idx);
    }
    if (ta.cid == 0) {
      LocalTag tag = (LocalTag)TagsService.getLocalTag(ta.name);
      if (tag == null) {
        JFLog.log("Error:Unable to find local tag:" + ta.name);
        return null;
      }
      if (tag.isArray()) {
        return tag.getIndex(ta);
      } else {
        return tag;
      }
    } else {
      TagBase tag = TagsService.getRemoteTag(ta.name);
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

  public TagAddr decode(String addr) {
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
}
