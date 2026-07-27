import java.util.*;

import javaforce.controls.*;

/** Read/Write PLC Clock.
 *
 * @author pquiring
 */

public class Clock {
  private static void usage() {
    System.out.println("jfplcclock PLC_URL read|write [YYYY-MM-DD HH:MM:SS]");
    System.out.println("  URL = S7:IP  //Siemens");
    System.out.println("  URL = AB:IP  //Allen Bradley");
    System.exit(1);
  }
  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
    }
    try {
      Controller c = new Controller();
      if (!c.connect(args[0])) {
        throw new Exception("Error:Controller.connect() failed");
      }
      switch (args[1]) {
        case "read": {
          Calendar cal = c.readTime();
          if (cal == null) throw new Exception("read failed");
          int year = cal.get(Calendar.YEAR);
          int month = cal.get(Calendar.MONTH) + 1;
          int day = cal.get(Calendar.DAY_OF_MONTH);
          int hour = cal.get(Calendar.HOUR_OF_DAY);
          int min = cal.get(Calendar.MINUTE);
          int sec = cal.get(Calendar.SECOND);
          System.out.println(String.format("%4d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec));
          break;
        }
        case "write": {
          Calendar cal = Calendar.getInstance();
          if (args.length >= 3) {
            String[] date = args[2].split("[-]");
            int year = Integer.valueOf(date[0]);
            int month = Integer.valueOf(date[1]);
            int day = Integer.valueOf(date[2]);
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
          }
          if (args.length >= 4) {
            String[] time = args[3].split("[:]");
            int hour = Integer.valueOf(time[0]);
            int min = Integer.valueOf(time[1]);
            int sec = Integer.valueOf(time[2]);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, sec);
          }
          boolean success = c.writeTime(cal);
          if (!success) throw new Exception("write failed");
          System.out.println("PLC Clock updated!");
          break;
        }
        default: {
          usage();
          break;
        }
      }
      c.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
