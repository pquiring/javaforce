package jfcontrols.functions;

/** Runtime environment
 *
 * @author pquiring
 */

import java.util.Calendar;
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

  public void getdate(TagBase tags[]) {
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int day = cal.get(Calendar.DAY_OF_MONTH);
    TagBase tag = tags[1];
    if (tag.getType() != IDs.uid_date) {
      JFLog.log("Error:GET_DATE:wrong tag type");
      return;
    }
    int idx = -1;
    if (tag instanceof TagArray) {
      TagAddr org = ((TagArray)tag).getAddr();
      idx = org.idx;
    }
    TagAddr ta = new TagAddr();
    ta.idx = idx;
    ta.member = "year";
    TagArray tagyear = (TagArray)tag.getIndex(ta);
    tagyear.setInt(year);
    ta.member = "month";
    TagArray tagmonth = (TagArray)tag.getIndex(ta);
    tagmonth.setInt(month);
    ta.member = "day";
    TagArray tagday = (TagArray)tag.getIndex(ta);
    tagday.setInt(day);
  }

  public void gettime(TagBase tags[]) {
    Calendar cal = Calendar.getInstance();
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int minute = cal.get(Calendar.MINUTE);
    int second = cal.get(Calendar.SECOND);
    int milli = cal.get(Calendar.MILLISECOND);
    TagBase tag = tags[1];
    if (tag.getType() != IDs.uid_time) {
      JFLog.log("Error:GET_TIME:wrong tag type");
      return;
    }
    int idx = -1;
    if (tag instanceof TagArray) {
      TagAddr org = ((TagArray)tag).getAddr();
      idx = org.idx;
    }
    TagAddr ta = new TagAddr();
    ta.idx = idx;
    ta.member = "hour";
    TagArray taghour = (TagArray)tag.getIndex(ta);
    taghour.setInt(hour);
    ta.member = "minute";
    TagArray tagminute = (TagArray)tag.getIndex(ta);
    tagminute.setInt(minute);
    ta.member = "second";
    TagArray tagsecond = (TagArray)tag.getIndex(ta);
    tagsecond.setInt(second);
    ta.member = "milli";
    TagArray tagmilli = (TagArray)tag.getIndex(ta);
    tagmilli.setInt(milli);
  }
}
