package jfcontrols.functions;

/** Runtime environment
 *
 * @author pquiring
 */

import javaforce.*;

import jfcontrols.sql.*;
import jfcontrols.tags.*;

public class FunctionRuntime extends TagsCache {
  public IndexTags it = new IndexTags();
  public void arraycopy(TagBase tags[]) {
    //tags = src srcOff dst dstOff length
    int length = tags[5].getInt();
    if (length <= 0) return;
    TagArray src = (TagArray)tags[1];
    int srcOff = tags[2].getInt();
    TagArray dst = (TagArray)tags[3];
    int dstOff = tags[4].getInt();
    TagAddr srcaddr = src.getAddr();
    TagAddr srcpos = new TagAddr(srcaddr);
    boolean srcidx = srcpos.midx == -1;
    TagAddr dstaddr = dst.getAddr();
    TagAddr dstpos = new TagAddr(dstaddr);
    boolean dstidx = dstpos.midx == -1;
    //check if we need to copy in reverse
    boolean fwd = true;
    if (srcaddr.name.equals(dstaddr.name)) {
      if (srcaddr.member != null && dstaddr.member != null) {
        if (srcaddr.member.equals(dstaddr.member)) {
          if (srcOff < dstOff) fwd = false;  //reverse copy
        }
      } else {
        if (srcOff < dstOff) fwd = false;  //reverse copy
      }
    }
    if (fwd) {
      if (srcidx) srcpos.idx = srcOff; else srcpos.midx = srcOff;
      if (dstidx) dstpos.idx = dstOff; else dstpos.midx = dstOff;
      for(int a=0;a<length;a++) {
        TagBase srctag = src.getIndex(srcpos);
        TagBase dsttag = dst.getIndex(dstpos);
        dsttag.setValue(srctag.getValue());
        if (srcidx) srcpos.idx++; else srcpos.midx++;
        if (dstidx) dstpos.idx++; else dstpos.midx++;
      }
    } else {
      srcOff += length - 1;
      dstOff += length - 1;
      if (srcidx) srcpos.idx = srcOff; else srcpos.midx = srcOff;
      if (dstidx) dstpos.idx = dstOff; else dstpos.midx = dstOff;
      for(int a=0;a<length;a++) {
        TagBase srctag = src.getIndex(srcpos);
        TagBase dsttag = dst.getIndex(dstpos);
        dsttag.setValue(srctag.getValue());
        if (srcidx) srcpos.idx--; else srcpos.midx--;
        if (dstidx) dstpos.idx--; else dstpos.midx--;
      }
    }
  }
  public void arraylength(TagBase tags[]) {
    TagArray tag = (TagArray)tags[1];
    TagAddr addr = tag.getAddr();
    boolean idx = addr.midx == -1;
    String len;
    SQL sql = SQLService.getSQL();
    String tid = sql.select1value("select id from tags where name=" + addr.name);
    if (idx) {
      len = sql.select1value("select max(idx) from tagvalues where tid=" + tid);
    } else {
      String type = sql.select1value("select type from tags where id=" + tid);
      String uid = sql.select1value("select uid from udts where type=" + type);
      String mid = sql.select1value("select mid from udtmems where name=" + addr.member + " and uid=" + uid);
      len = sql.select1value("select max(midx) from tagvalues where tid=" + tid + " and mid=" + mid);
    }
    if (len == null) len = "0";
    sql.close();
    tags[2].setValue(len);
  }
}
