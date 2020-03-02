/**
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Camera implements Serializable {
  public static final long serialVersionUID = 1;
  public String name;
  public String url;
  public boolean record_motion;
  public int record_motion_threshold;
  public int record_motion_after;
  public int max_file_size;  //in MBs
  public int max_folder_size;  //in GBs

  public transient volatile float motion_value;
  public transient volatile byte[] preview;  //png image
  public transient volatile boolean viewing;
  public transient volatile boolean update_preview;
}
