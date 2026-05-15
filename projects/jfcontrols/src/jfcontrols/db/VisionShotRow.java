package jfcontrols.db;

/** VisionShotRow
 *
 * @author pquiring
 */

public class VisionShotRow extends javaforce.db.Row {
  public static final long serialVersionUID = 1L;
  public int pid;  //program id
  public int cid;  //camera id
  public int offset;  //offset (mm)
}
