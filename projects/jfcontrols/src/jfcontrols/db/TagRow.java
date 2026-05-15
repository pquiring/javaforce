package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class TagRow extends javaforce.db.Row {
  public static final long serialVersionUID = 1L;
  public int cid;
  public String name;
  public int type;
  public int length;
  public String comment;
  public boolean builtin;
}
