package javaforce.gl.model;

import java.io.*;

import javaforce.gl.*;

/** Model_IO is base interface for Model import/export functions.
 *
 * @author pquiring
 */

public interface Model_IO {
  public Model load(InputStream is);
  public boolean save(Model model, OutputStream os);
}
