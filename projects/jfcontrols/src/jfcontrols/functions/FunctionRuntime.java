package jfcontrols.functions;

/** Runtime environment
 *
 * @author pquiring
 */

import java.util.Calendar;
import javaforce.*;

import jfcontrols.tags.*;

public class FunctionRuntime extends TagsCache {
  public static long now;
  public static SQL sql;
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
    int tid = tag.getTagID();
    if (!isMember) {
      len = sql.select1value("select max(idx) from jfc_tagvalues where tid=" + tid);
    } else {
      int mid = tag.getMember();
      len = sql.select1value("select max(midx) from jfc_tagvalues where tid=" + tid + " and mid=" + mid);
    }
    if (len == null) {
      tags[2].setInt(0);
    } else {
      tags[2].setInt(Integer.valueOf(len) + 1);
    }
  }
  public void arraysize(TagBase tags[]) {
    TagBase tag = tags[1];
    boolean isMember = tag.isMember();
    String len;
    int tid = tag.getTagID();
    if (!isMember) {
      len = sql.select1value("select count(idx) from jfc_tagvalues where tid=" + tid);
    } else {
      int mid = tag.getMember();
      len = sql.select1value("select count(midx) from jfc_tagvalues where tid=" + tid + " and mid=" + mid);
    }
    if (len == null) len = "0";
    tags[2].setInt(Integer.valueOf(len));
  }
  public void arrayremove(TagBase tags[]) {
    TagBase tag = tags[1];
    boolean isMember = tag.isMember();
    int tid = tag.getTagID();
    if (!isMember) {
      int idx = tags[2].getInt();
      sql.execute("delete from jfc_tagvalues where tid=" + tid + " and idx=" + idx);
    } else {
      int mid = tag.getMember();
      int idx = tag.getIndex();
      int midx = tags[2].getInt();
      sql.execute("delete from jfc_tagvalues where tid=" + tid + " and idx=" + idx + " and mid=" + mid + " and midx=" + midx);
    }
  }
  public void arrayshift(TagBase tags[]) {
    TagBase tag = tags[1];
    boolean isMember = tag.isMember();
    int tid = tag.getTagID();
    if (!isMember) {
      int cnt = tags[2].getInt();
      for(int a=0;a<cnt;a++) {
        String zero = sql.select1value("select value from jfc_tagvalues where tid=" + tid + " and idx=0");
        if (zero != null) break;
        sql.execute("update jfc_tagvalues set idx=idx-1 where tid=" + tid);
      }
    } else {
      int mid = tag.getMember();
      int idx = tag.getIndex();
      int cnt = tags[2].getInt();
      for(int a=0;a<cnt;a++) {
        String zero = sql.select1value("select value from jfc_tagvalues where tid=" + tid + " and idx=" + idx + " and mid=" + mid + " and midx=0");
        if (zero != null) break;
        sql.execute("update jfc_tagvalues set midx=midx-1 where tid=" + tid + " and idx=" + idx + " and mid=" + mid);
      }
    }
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

  public boolean timer_on_delay(boolean enabled, TagBase tags[]) {
    TagBase timer = tags[1];
    long timeout = tags[2].getLong();
    TagBase leftTag = timer.getMember(IDs.timer_mid_time_left);
    long left = leftTag.getLong();
    TagBase lastTag = timer.getMember(IDs.timer_mid_time_last);
    long last = lastTag.getLong();
    TagBase runTag = timer.getMember(IDs.timer_mid_run);
    boolean run = runTag.getBoolean();
    TagBase doneTag = timer.getMember(IDs.timer_mid_done);
    boolean done = doneTag.getBoolean();
    TagBase enTag = timer.getMember(IDs.timer_mid_enabled);
    boolean en = doneTag.getBoolean();
    if (en != enabled) {
      enTag.setBoolean(enabled);
    }
    if (enabled) {
      if (!done) {
        if (!run) {
          //start timer
          last = now;
          left = timeout;
          run = true;
          runTag.setBoolean(run);
        } else {
          long delta = now - last;
          left -= delta;
          if (left < 0) left = 0;
        }
        leftTag.setLong(left);
        lastTag.setLong(now);
        if (left == 0) {
          done = true;
          doneTag.setBoolean(done);
          run = false;
          runTag.setBoolean(run);
        }
      }
    } else {
      if (run) {
        run = false;
        runTag.setBoolean(run);
      }
      if (done) {
        done = false;
        doneTag.setBoolean(done);
      }
    }
    return done;
  }

  public boolean timer_off_delay(boolean enabled, TagBase tags[]) {
    TagBase timer = tags[1];
    long timeout = tags[2].getLong();
    TagBase leftTag = timer.getMember(IDs.timer_mid_time_left);
    long left = leftTag.getLong();
    TagBase lastTag = timer.getMember(IDs.timer_mid_time_last);
    long last = lastTag.getLong();
    TagBase runTag = timer.getMember(IDs.timer_mid_run);
    boolean run = runTag.getBoolean();
    TagBase doneTag = timer.getMember(IDs.timer_mid_done);
    boolean done = doneTag.getBoolean();
    TagBase enTag = timer.getMember(IDs.timer_mid_enabled);
    boolean en = doneTag.getBoolean();
    if (enabled) {
      last = now;
      left = timeout;
      if (run == false) {
        run = true;
        runTag.setBoolean(run);
      }
      if (done == true) {
        done = false;
        doneTag.setBoolean(done);
      }
    } else if (left > 0) {
      long delta = now - last;
      left -= delta;
      if (left < 0) left = 0;
    }
    leftTag.setLong(left);
    lastTag.setLong(now);
    if (left == 0 && run) {
      done = true;
      doneTag.setBoolean(done);
      run = false;
      runTag.setBoolean(run);
    }
    if (en != enabled) {
      en = enabled;
      enTag.setBoolean(en);
    }
    return run;
  }
  public static void alarm_clear_ack() {
    //clear ack bit for inactive alarms (run once per scan)
    String tid = sql.select1value("select id from jfc_tags where name='alarms'");
    String active[] = sql.select1col("select idx from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_active + " and value='0'");
    String ack[] = sql.select1col("select idx from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_ack + " and value='1'");
    for(int a=0;a<active.length;a++) {
      String idx = active[a];
      for(int b=0;b<ack.length;b++) {
        if (ack[b].equals(idx)) {
          sql.execute("update jfc_tagvalues set value='0' where tid=" + tid + " and mid=" + IDs.alarm_mid_ack + " and idx=" + idx);
          break;
        }
      }
    }
  }
  public static boolean alarm_active() {
    String tid = sql.select1value("select id from jfc_tags where name='alarms'");
    String count = sql.select1value("select count(idx) from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_active + " and value='1'");
    return !count.equals("0");
  }
  public static boolean alarm_not_ack() {
    String tid = sql.select1value("select id from jfc_tags where name='alarms'");
    String active[] = sql.select1col("select idx from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_active + " and value='1'");
    String ack[] = sql.select1col("select idx from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_ack + " and value='1'");
    for(int a=0;a<active.length;a++) {
      String idx = active[a];
      for(int b=0;b<ack.length;b++) {
        if (ack[b].equals(idx)) {idx = null; break;}
      }
      if (idx != null) return true;
    }
    return false;
  }
  public void alarm_ack_all() {
    String tid = sql.select1value("select id from jfc_tags where name='alarms'");
    String active[] = sql.select1col("select idx from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_active + " and value='1'");
    String ack[] = sql.select1col("select idx from jfc_tagvalues where tid=" + tid + " and mid=" + IDs.alarm_mid_ack + " and value='1'");
    for(int a=0;a<active.length;a++) {
      String idx = active[a];
      for(int b=0;b<ack.length;b++) {
        if (ack[b].equals(idx)) {idx = null; break;}
      }
      if (idx == null) continue;
      TagBase tag = this.getTag("alarms[" + idx + "].ack");
      tag.setBoolean(true);
    }
  }
}
