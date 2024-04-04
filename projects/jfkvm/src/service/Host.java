package service;

/**
 *
 * @author pquiring
 */

import java.io.*;

public class Host implements Serializable {
  private static final long serialVersionUID = 1L;

  public String token;
  public String host;

  //transient data
  public transient boolean online;
  public transient boolean valid;
  public transient float version;

  public String[] getState() {
    return new String[] {host, String.format("%.1f", version), Boolean.toString(online), Boolean.toString(valid)};
  }

  public boolean isValid(float min_ver) {
    return online && valid && version >= min_ver;
  }

  public boolean isValid() {
    return isValid(0);
  }
}
