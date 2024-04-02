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
  public transient float version;
}
