package javaforce.lxc;

/** Linux Image
 *
 * example:
 * [repo/]amd64/debian:trixie
 *   arch=amd64
 *   name=debian
 *   version=trixie
 *
 * @author pquiring
 */

public class LxcImage {

  public LxcImage() {}

  public LxcImage(String mtag) {
    int idx = mtag.lastIndexOf(':');
    if (idx == -1) {
      version = "latest";
    } else {
      mtag = mtag.substring(0, idx);
      version = mtag.substring(idx + 1);
    }
    String[] fs = mtag.split("[/]");
    int cnt = fs.length - 1;
    if (cnt >= 0) name = fs[cnt--];
    if (cnt >= 0) arch = fs[cnt--];
    if (cnt >= 0) repo = fs[cnt--];
  }

  public LxcImage(String mtag, String version) {
    this.version = version;
    String[] fs = mtag.split("[/]");
    int cnt = fs.length - 1;
    if (cnt >= 0) name = fs[cnt--];
    if (cnt >= 0) arch = fs[cnt--];
    if (cnt >= 0) repo = fs[cnt--];
  }

  public LxcImage(String arch, String name, String version) {
    this.arch = arch;
    this.name = name;
    this.version = version;
  }

  public LxcImage(String repo, String arch, String name, String version) {
    this.repo = repo;
    this.arch = arch;
    this.name = name;
    this.version = version;
  }

  public String id;

  public String repo;  //default = hub.docker.com
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
    if (repo != null) {
      return repo + "/" + arch + "/" + name  + ":" + version;
    } else {
      return arch + "/" + name  + ":" + version;
    }
  }
}
