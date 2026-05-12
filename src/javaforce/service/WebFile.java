package javaforce.service;

/** Web File uploaded to server.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class WebFile {
  private FileInputStream is;
  /** Uploaded file. */
  public File file;
  /** Name of file (excluding any path info) */
  public String name;
  /** Content Type. */
  public String contentType;
  /** Moves uploaded file to dest file. (optional) */
  public boolean move(File dest) throws Exception {
    if (dest.exists()) return false;
    return JF.moveFile(file, dest);
  }
  public String getName() {return name;}
  public String getContentType() {return contentType;}
  public InputStream getInputStream() {
    if (is == null) {
      try {
        is = new FileInputStream(file);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    return is;
  }
}
