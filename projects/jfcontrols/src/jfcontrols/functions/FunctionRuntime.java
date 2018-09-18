package jfcontrols.functions;

/** Runtime environment
 *
 * @author pquiring
 */

import jfcontrols.tags.TagBase;
import java.util.Calendar;

import javaforce.*;

import jfcontrols.tags.*;

public class FunctionRuntime {
  public static long now;
  public static SQL sql;
  public IndexTags it = new IndexTags(256);
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
        dst.setValue(src.getValue(srcOff), dstOff);
        srcOff++;
        dstOff++;
      }
    } else {
      //reverse copy
      srcOff += length - 1;
      dstOff += length - 1;
      for(int a=0;a<length;a++) {
        dst.setValue(src.getValue(srcOff), dstOff);
        srcOff--;
        dstOff--;
      }
    }
  }
  public void arraylength(TagBase tags[]) {
    TagBase tag = tags[1];
    int len = tag.getLength();
    if (len == -1) {
      tags[2].setInt(0);
    } else {
      tags[2].setInt(Integer.valueOf(len) + 1);
    }
  }
  public void arraysize(TagBase tags[]) {
    arraylength(tags);
  }
  public void arrayshift(TagBase tags[]) {
    TagBase tag = tags[1];
    //TODO
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
    //TODO


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
    //TODO

  }

  public boolean timer_on_delay(boolean enabled, TagBase tags[]) {
    TagUDT timer_udt = (TagUDT)tags[1];
    TagBase[] timer = timer_udt.fields[0];  //TODO : index ???
    TagBase time_left_tag = (TagBase)timer[0];
    TagBase time_last_tag = (TagBase)timer[1];
    TagBase run_tag = (TagBase)timer[2];
    TagBase done_tag = (TagBase)timer[3];
    TagBase enabled_tag = (TagBase)timer[4];

    long timeout = tags[2].getLong();

    long left = time_left_tag.getLong(0);
    long last = time_last_tag.getLong(0);
    boolean run = run_tag.getBoolean(0);
    boolean done = done_tag.getBoolean(0);
    boolean en = enabled_tag.getBoolean(0);
    if (en != enabled) {
      enabled_tag.setBoolean(0, enabled);
    }
    if (enabled) {
      if (!done) {
        if (!run) {
          //start timer
          last = now;
          left = timeout;
          run = true;
          run_tag.setBoolean(0, run);
        } else {
          long delta = now - last;
          left -= delta;
          if (left < 0) left = 0;
        }
        time_left_tag.setLong(0, left);
        time_last_tag.setLong(0, now);
        if (left == 0) {
          done = true;
          done_tag.setBoolean(0, done);
          run = false;
          run_tag.setBoolean(0, run);
        }
      }
    } else {
      if (run) {
        run = false;
          run_tag.setBoolean(0, run);
      }
      if (done) {
        done = false;
        done_tag.setBoolean(0, done);
      }
    }
    return done;
  }

  public boolean timer_off_delay(boolean enabled, TagBase tags[]) {
    TagUDT timer_udt = (TagUDT)tags[1];
    TagBase[] timer = timer_udt.fields[0];  //TODO : ???
    TagBase time_left_tag = (TagBase)timer[0];
    TagBase time_last_tag = (TagBase)timer[1];
    TagBase run_tag = (TagBase)timer[2];
    TagBase done_tag = (TagBase)timer[3];
    TagBase enabled_tag = (TagBase)timer[4];

    long timeout = tags[2].getLong();

    long left = time_left_tag.getLong(0);
    long last = time_last_tag.getLong(0);
    boolean run = run_tag.getBoolean(0);
    boolean done = done_tag.getBoolean(0);
    boolean en = enabled_tag.getBoolean(0);
    if (enabled) {
      last = now;
      left = timeout;
      if (run == false) {
        run = true;
        run_tag.setBoolean(0, run);
      }
      if (done == true) {
        done = false;
        done_tag.setBoolean(0, done);
      }
    } else if (left > 0) {
      long delta = now - last;
      left -= delta;
      if (left < 0) left = 0;
    }
    time_left_tag.setLong(0, left);
    time_last_tag.setLong(0, now);
    if (left == 0 && run) {
      done = true;
        done_tag.setBoolean(0, done);
      run = false;
      run_tag.setBoolean(0, run);
    }
    if (en != enabled) {
      en = enabled;
      enabled_tag.setBoolean(0, en);
    }
    return run;
  }
  public static int alarm_active_count() {
    TagUDT alarms = (TagUDT)TagsService.getTag("alarms");
    int length = alarms.getLength();
    int count = 0;
    for(int a=0;a<length;a++) {
      TagBase fields[] = alarms.getFields(a);
      if (fields[IDs.fid_alarm_active].getBoolean()) count++;
    }
    return count;
  }
  public static void alarm_clear_ack() {
    TagBase alarms = TagsService.getTag("alarms");
    //clear ack bit for inactive alarms (run once per scan)
    int length = alarms.getLength();
    for(int a=0;a<length;a++) {
      TagBase fields[] = alarms.getFields(a);
      if (fields[IDs.fid_alarm_ack].getBoolean()) {
        if (!fields[IDs.fid_alarm_active].getBoolean()) {
          fields[IDs.fid_alarm_ack].setBoolean(false);
        }
      }
    }
  }
  public static boolean alarm_active() {
    TagUDT alarms = (TagUDT)TagsService.getTag("alarms");
    int length = alarms.getLength();
    for(int a=0;a<length;a++) {
      TagBase fields[] = alarms.getFields(a);
      if (fields[IDs.fid_alarm_active].getBoolean()) return true;
    }
    return false;
  }
  public static boolean alarm_not_ack() {
    TagUDT alarms = (TagUDT)TagsService.getTag("alarms");
    int length = alarms.getLength();
    for(int a=0;a<length;a++) {
      TagBase fields[] = alarms.getFields(a);
      if (fields[IDs.fid_alarm_active].getBoolean() && !fields[IDs.fid_alarm_ack].getBoolean()) return true;
    }
    return false;
  }
  public void alarm_ack_all() {
    TagUDT alarms = (TagUDT)TagsService.getTag("alarms");
    int length = alarms.getLength();
    for(int a=0;a<length;a++) {
      TagBase fields[] = alarms.getFields(a);
      if (fields[IDs.fid_alarm_active].getBoolean()) {
        if (!fields[IDs.fid_alarm_ack].getBoolean()) {
          fields[IDs.fid_alarm_ack].setBoolean(true);
        }
      }
    }
  }
}
