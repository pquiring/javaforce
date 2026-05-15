package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class AlarmRow extends javaforce.db.Row {
  public static final long serialVersionUID = 1L;
  public AlarmRow() {}
  public AlarmRow(int id) {
    this.aid = id;
  }
  public int aid;
}
