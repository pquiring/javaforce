package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class ListRow extends javaforce.db.Row {
  public ListRow(int idx, String value) {
    this.idx = idx;
    this.value = value;
  }
  public int idx;
  public String value;
}
