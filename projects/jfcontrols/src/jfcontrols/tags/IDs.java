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
  //0x1000 - 0x11ff = user data types
  public static final int uid_user = 0x1000;
  public static final int uid_alarms = 0x1000;
  public static final int uid_user_end = 0x1100;

  //mids
  public static final int system_mid_scantime = 0;

  public static final int alarm_mid_name = 0;
  public static final int alarm_mid_active = 1;
  public static final int alarm_mid_ack = 2;
}
