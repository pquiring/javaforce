package jfcontrols.tags;

/** Tag IDs
 *
 * @author pquiring
 */

public class IDs {

  //0x00 - 0xff = standard data types
  //0x100 - 0xfff = system data types
  public static final int uid_sdt = 0x100;
  public static final int uid_sys = 0x100;
  public static final int uid_io = 0x101;
  public static final int uid_date = 0x102;
  public static final int uid_time = 0x103;
  public static final int uid_timer = 0x104;
  //0x1000 - 0x11ff = user data types
  public static final int uid_user = 0x1000;
  public static final int uid_alarms = 0x1000;
  public static final int uid_user_end = 0x1100;

  //mids
  public static final int system_mid_scantime = 0;

  public static final int io_mid_di = 0;
  public static final int io_mid_do = 1;
  public static final int io_mid_ai = 2;
  public static final int io_mid_ao = 3;

  public static final int alarm_mid_name = 0;
  public static final int alarm_mid_active = 1;
  public static final int alarm_mid_ack = 2;

  public static final int date_mid_year = 0;
  public static final int date_mid_month = 1;
  public static final int date_mid_day = 2;

  public static final int time_mid_hour = 0;
  public static final int time_mid_minute = 1;
  public static final int time_mid_second = 2;
  public static final int time_mid_milli = 3;

  public static final int timer_mid_time_left = 0;
  public static final int timer_mid_time_last = 1;
  public static final int timer_mid_run = 2;
  public static final int timer_mid_done = 3;
  public static final int timer_mid_enabled = 4;
}
