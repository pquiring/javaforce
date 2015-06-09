package jffile;

/**
 *
 * @author pquiring
 */

public class FileEntry {
  public int x, y;
  public String name, icon, file;

  public transient JFileIcon button;
  public transient javax.swing.JLabel details_date, details_time, details_size;
  public transient boolean isLink, isDir;
}
