package javaforce.webui.tasks;

/** Task Event
 *
 * @author pquiring
 */

import javaforce.*;

public class TaskEvent {
  public long time_start;
  public long time_complete;
  public long time_duration;

  public String action = "";
  public String result = "";

  public String user = "";
  public String ip = "";

  public static final TaskEvent[] ArrayType = new TaskEvent[0];
  public static byte VERSION = 1;

  public static TaskEvent create(String action, String user, String ip) {
    TaskEvent event = new TaskEvent();
    event.action = action;
    event.user = user;
    event.ip = ip;
    return event;
  }

  public static TaskEvent fromByteArray(byte[] data, int offset, int length) {
    TaskEvent event = new TaskEvent();

    byte version = data[offset]; offset++;

    event.time_start = BE.getuint64(data, offset); offset += 8;
    event.time_complete = BE.getuint64(data, offset); offset += 8;
    event.time_duration = BE.getuint64(data, offset); offset += 8;

    int action_length = BE.getuint32(data, offset); offset += 4;
    event.action = BE.getString(data, offset, action_length); offset += action_length;

    int result_length = BE.getuint32(data, offset); offset += 4;
    event.result = BE.getString(data, offset, result_length); offset += result_length;

    int user_length = BE.getuint32(data, offset); offset += 4;
    event.user = BE.getString(data, offset, user_length); offset += user_length;

    int ip_length = BE.getuint32(data, offset); offset += 4;
    event.ip = BE.getString(data, offset, ip_length); offset += ip_length;

    return event;
  }

  public byte[] toByteArray() {
    int action_length = action.length();
    int result_length = result.length();
    int user_length = user.length();
    int ip_length = ip.length();
    int length = 1 + (3 * 8) + (4 + action_length) + (4 + result_length) + (4 + user_length) + (4 + ip_length);
    byte[] data = new byte[4 + length];
    int offset = 0;

    BE.setuint32(data, offset, length); offset += 4;

    data[offset] = VERSION; offset++;

    BE.setuint64(data, offset, time_start); offset += 8;
    BE.setuint64(data, offset, time_complete); offset += 8;
    BE.setuint64(data, offset, time_duration); offset += 8;

    BE.setuint32(data, offset, action_length); offset += 4;
    BE.setString(data, offset, action_length, action); offset += action_length;

    BE.setuint32(data, offset, result_length); offset += 4;
    BE.setString(data, offset, result_length, result); offset += result_length;

    BE.setuint32(data, offset, user_length); offset += 4;
    BE.setString(data, offset, user_length, user); offset += user_length;

    BE.setuint32(data, offset, ip_length); offset += 4;
    BE.setString(data, offset, ip_length, ip); offset += ip_length;

    return data;
  }
}
