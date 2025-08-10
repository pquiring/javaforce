package javaforce.lxc;

/** Linux Container interface
 *
 * @author pquiring
 */

import javaforce.jni.lnx.*;

public abstract class LxcContainer {

  public LxcContainer(String id) {this.id = id;}

  public String id;
  public LxcImage image;

  public abstract LnxPty attach();
  public abstract boolean detach();
  public abstract boolean close();
  public abstract boolean restart();
  public abstract boolean delete();

  public LxcContainer setImage(LxcImage image) {
    this.image = image;
    return this;
  }

  /** Returns id, image. */
  public String[] getStates() {
    return new String[] {id, image.toString()};
  }

  public String toString() {
    return "LcxContainter:" + id;
  }
}
