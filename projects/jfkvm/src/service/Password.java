package service;

/** Password
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Password implements Serializable {
  private static final long serialVersionUID = 1L;

  public Password(String name, String password) {
    this.name = name;
    this.password = password;
  }

  public String name;
  public String password;

  public boolean save() {
    try {
      String file = Paths.secretPath + "/" + name;
      return Compression.serialize(file, this);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public static Password load(String name) {
    try {
      String file = Paths.secretPath + "/" + name;
      Password password = (Password)Compression.deserialize(file);
      return password;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  public static void delete(String name) {
    try {
      String file = Paths.secretPath + "/" + name;
      new File(file).delete();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
