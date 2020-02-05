package jfcontrols.db;

/** GroupRow
 *
 * @author pquiring
 */

public class GroupRow extends javaforce.db.Row {
  public static class Zone implements java.io.Serializable {
    public static final long serialVersionUID = 1;
    public int rid;  //reader id
    public int zid;  //timezone id
  }
  public String name;
  public Zone zones[] = new Zone[0];
}
