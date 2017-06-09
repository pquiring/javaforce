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
    TagBase src = (TagBase)tags[1];
    int srcOff = tags[2].getInt();
    TagBase dst = (TagBase)tags[3];
    int dstOff = tags[4].getInt();
    if (srcOff > dstOff) {
      //forward copy
      for(int a=0;a<length;a++) {
        TagBase srctag = src.getIndex(srcOff);
        TagBase dsttag = dst.getIndex(dstOff);
        dsttag.setValue(srctag.getValue());
        srcOff++;
        dstOff++;
      }
    } else {
      //reverse copy
      srcOff += length - 1;
      dstOff += length - 1;
      for(int a=0;a<length;a++) {
        TagBase srctag = src.getIndex(srcOff);
        TagBase dsttag = dst.getIndex(dstOff);
        dsttag.setValue(srctag.getValue());
        srcOff--;
        dstOff--;
      }
    }
  }
  public void arraylength(TagBase tags[]) {
    TagBase tag = tags[1];
    boolean isMember = tag.isMember();
    String len;
    SQL sql = SQLService.getSQL();
    int tid = tag.getTagID();
    if (!isMember) {
      len = sql.select1value("select max(idx) from tagvalues where tid=" + tid);
    } else {
      int mid = tag.getMember();
      len = sql.select1value("select max(midx) from tagvalues where tid=" + tid + " and mid=" + mid);
    }
    if (len == null) len = "0";
    sql.close();
    tags[2].setValue(len);
  }

  public void getdate(TagBase tag) {
    if (tag.getType() != IDs.uid_date) {
      JFLog.log("Error:GET_DATE:wrong tag type");
      return;
    }
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int day = cal.get(Calendar.DAY_OF_MONTH);
    tag.getMember(IDs.date_mid_year).setInt(year);
    tag.getMember(IDs.date_mid_month).setInt(month);
    tag.getMember(IDs.date_mid_day).setInt(day);
  }

  public void gettime(TagBase tag) {
    if (tag.getType() != IDs.uid_time) {
      JFLog.log("Error:GET_TIME:wrong tag type");
      return;
    }
    Calendar cal = Calendar.getInstance();
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int minute = cal.get(Calendar.MINUTE);
    int second = cal.get(Calendar.SECOND);
    int milli = cal.get(Calendar.MILLISECOND);
    tag.getMember(IDs.time_mid_hour).setInt(hour);
    tag.getMember(IDs.time_mid_minute).setInt(minute);
    tag.getMember(IDs.time_mid_second).setInt(second);
    tag.getMember(IDs.time_mid_milli).setInt(milli);
  }
}
