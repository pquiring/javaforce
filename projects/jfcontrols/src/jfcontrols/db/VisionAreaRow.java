package jfcontrols.db;

/** VisionAreaRow
 *
 * @author pquiring
 */

public class VisionAreaRow extends javaforce.db.Row {
  public static final long serialVersionUID = 1L;
  public int pid;  //program
  public int sid;  //shot
  public String name;
  public int x1,y1,x2,y2;
}
