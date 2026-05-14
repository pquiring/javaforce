package jfcontrols.db;

/** TimezoneRow
 *
 * @author pquirnig
 */

public class TimezoneRow extends javaforce.db.Row {
  //format 00:00 to 23:59
  public static class Time extends javaforce.db.Row {
    public int hour;
    public int min;
  }
  public String name;
  public Time begin[] = new Time[7];
  public Time end[] = new Time[7];
}
