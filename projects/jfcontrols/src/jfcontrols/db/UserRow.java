package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class UserRow extends javaforce.db.Row {
  public static final long serialVersionUID = 1L;
  public String name;
  public String pass;
  public int gid;  //group id
}
