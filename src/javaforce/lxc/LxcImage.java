package javaforce.lxc;

/** Linux Image
 *
 * example:
 * amd64/debian:trixie
 *   arch=amd64
 *   name=debian
 *   version=trixie
 *
 * @author pquiring
 */

public class LxcImage {

  public LxcImage() {}

  public LxcImage(String arch_name_version) {
    int idx = arch_name_version.indexOf('/');
    if (idx == -1) {
      arch = "";
    } else {
      arch = arch_name_version.substring(0, idx);
      arch_name_version = arch_name_version.substring(idx + 1);
    }
    idx = arch_name_version.indexOf(':');
    if (idx == -1) {
      name = arch_name_version;
      version = "latest";
    } else {
      name = arch_name_version.substring(0, idx);
      version = arch_name_version.substring(idx + 1);
    }
  }

  public LxcImage(String arch_name, String version) {
    int idx = arch_name.indexOf('/');
    if (idx == -1) {
      arch = "";
    } else {
      arch = arch_name.substring(0, idx);
      arch_name = arch_name.substring(idx + 1);
    }
    name = arch_name;
    this.version = version;
  }

  public LxcImage(String arch, String name, String version) {
    this.arch = arch;
    this.name = name;
    this.version = version;
  }

  public String id;

  public String arch;
  public String name;
  public String version;

  public LxcImage setID(String id) {
    this.id = id;
    return this;
  }

  public boolean delete() {
    return false;
  }

  /** Returns id, image. */
  public String[] getStates() {
    return new String[] {id, toString()};
  }

  public String toString() {
    return arch + "/" + name  + ":" + version;
  }
}
