package javaforce.gl.model;

/** Base interface for Model import/export functions.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.gl.*;

public interface Model_IO {
  public Model load(InputStream is);
  public boolean save(Model model, OutputStream os);
}
