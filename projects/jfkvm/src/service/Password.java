package service;

/** Password
 *
 * stored in /root/secret
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Password implements Serializable {
  private static final long serialVersionUID = 1L;

  public Password(int type, String name, String password) {
    this.type = type;
    this.name = name;
    this.password = password;
  }

  public int type;
  public String name;
  public String password;

  public static final int TYPE_SYSTEM = 0;
  public static final int TYPE_STORAGE = 1;

  private static String getTypeString(int type) {
    switch (type) {
      case TYPE_SYSTEM: return "system-";
      case TYPE_STORAGE: return "storage-";
    }
    return null;
  }

  public boolean save() {
    try {
      String file = Paths.secretPath + "/" + getTypeString(type) + name;
      return Compression.serialize(file, this);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public static Password load(int type, String name) {
    try {
      String file = Paths.secretPath + "/" + getTypeString(type) + name;
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
