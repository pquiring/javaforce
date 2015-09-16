/**
 *
 * @author pquiring
 */

import javaforce.gl.*;

public class Data {
  public static GLModel model;
  public static GLScene scene;
  public static GLWindow canvas;  //holds main OpenGL context (although only visible during startup)

  public static float x,y,z;  //cursor position
}
