package javaforce.gl.model;

/** FBX is owned by AutoDesk
 *
 * https://en.wikipedia.org/wiki/FBX
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.gl.*;

public class ModelFBX implements Model_IO {

  public Model load(InputStream is) {
    return null;
  }
  public boolean save(Model model, OutputStream os) {
    return false;
  }

}
