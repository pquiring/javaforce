package javaforce.vm;

/** Location.
 *
 * Specifies where a VirtualMachine is stored.
 *
 * @author pquiring
 */

import java.io.*;

public class Location implements Serializable {
  private static final long serialVersionUID = 1L;

  public Location(String pool, String folder, String name) {
    this.pool = pool;
    this.folder = folder;
    this.name = name;
  }

  public String pool;
  public String folder;
  public String name;
}
