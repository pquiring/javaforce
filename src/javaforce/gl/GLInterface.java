package javaforce.gl;

/**
 *
 * @author pquiring
 *
 * Created : Sept 18, 2013
 */

import java.awt.*;

public interface GLInterface {
  public void init(GL gl, Component comp);
  public void render(GL gl);
  public void resize(GL gl, int width, int height);
}
