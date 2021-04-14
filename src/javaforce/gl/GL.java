package javaforce.gl;

/**
 * OpenGL binding for Java
 *
 * @author pquiring
 *
 * Created : Sept 16, 2013
 *
 * Notes:
 *   - only supports OpenGL 2.0 or better (1.x not supported)
 *   - only call GL functions from the EDT (event dispatching thread)
 *   - Supports Windows, Linux and MacOSX.VI or better (aka SnowLeopard)
 *   - if there are functions or constants missing feel free to add them
 *     - add constants to end of "GL Constants" list
 *     - open a bug report and I will add it
 */

import javaforce.*;

public class GL {
  static {
    if (JF.isWindows()) {
      os = OS.WINDOWS;
    } else if (JF.isMac()) {
      os = OS.MAC;
    } else {
      os = OS.LINUX;
    }
  }

  /** Loads OpenGL functions.
   *
   * Windows : must call only when a valid OpenGL Context is set
   * Linux : not sure?
   * Mac : Call anytime.
   *
   */
  public static native boolean glInit();

  private static final boolean debug = false;

  //common data
  private enum OS {WINDOWS, LINUX, MAC};
  private static OS os;

  //GL constants
  public static final int GL_VERSION = 0x1F02;
  public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
  public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
  public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
  public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
  public static final int GL_CW = 0x900;
  public static final int GL_CCW = 0x0901;
  public static final int GL_CULL_FACE = 0x0b44;
  public static final int GL_BLEND = 0x0be2;
  public static final int GL_DEPTH_TEST = 0x0b71;

  public static final int GL_NEVER = 0x0200;
  public static final int GL_LESS = 0x0201;
  public static final int GL_EQUAL = 0x0202;
  public static final int GL_LEQUAL = 0x0203;
  public static final int GL_GREATER = 0x0204;
  public static final int GL_NOTEQUAL = 0x0205;
  public static final int GL_GEQUAL = 0x0206;
  public static final int GL_ALWAYS = 0x0207;

  public static final int GL_SRC_COLOR = 0x0300;
  public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
  public static final int GL_SRC_ALPHA = 0x0302;
  public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
  public static final int GL_DST_ALPHA = 0x0304;
  public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
  public static final int GL_DST_COLOR = 0x0306;
  public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
  public static final int GL_SRC_ALPHA_SATURATE = 0x0308;

  public static final int GL_UNPACK_ALIGNMENT = 0x0cf5;
  public static final int GL_TEXTURE_2D = 0x0de1;
  public static final int GL_TEXTURE_WRAP_S = 0x2802;
  public static final int GL_TEXTURE_WRAP_T = 0x2803;
  public static final int GL_REPEAT = 0x2901;
  public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
  public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
  public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
  public static final int GL_NEAREST = 0x2600;
  public static final int GL_TEXTURE_ENV = 0x2300;
  public static final int GL_TEXTURE_ENV_MODE = 0x2200;
  public static final int GL_MODULATE = 0x2100;
  public static final int GL_RGBA = 0x1908;
  public static final int GL_BGRA = 0x80e1;
  public static final int GL_COLOR_BUFFER_BIT = 0x4000;
  public static final int GL_DEPTH_BUFFER_BIT= 0x0100;
  public static final int GL_STENCIL_BUFFER_BIT = 0x0400;
  public static final int GL_STENCIL_TEST = 0x0B90;
  public static final int GL_ARRAY_BUFFER = 0x8892;
  public static final int GL_STATIC_DRAW = 0x88e4;
  public static final int GL_STREAM_DRAW = 0x88e0;
  public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
  public static final int GL_FLOAT = 0x1406;
  public static final int GL_FALSE = 0;
  public static final int GL_TRUE = 1;
  public static final int GL_ZERO = 0;
  public static final int GL_ONE = 1;
  public static final int GL_UNSIGNED_BYTE = 0x1401;
  public static final int GL_UNSIGNED_SHORT = 0x1403;
  public static final int GL_UNSIGNED_INT = 0x1405;

  public static final int GL_POINTS = 0x0000;
  public static final int GL_LINES = 0x0001;
  public static final int GL_LINE_LOOP = 0x0002;
  public static final int GL_LINE_STRIP = 0x0003;
  public static final int GL_TRIANGLES = 0x0004;
  public static final int GL_TRIANGLE_STRIP = 0x0005;
  public static final int GL_TRIANGLE_FAN = 0x0006;
  public static final int GL_QUADS = 0x0007;
  public static final int GL_QUAD_STRIP = 0x0008;
  public static final int GL_POLYGON = 0x0009;

  public static final int GL_FRAGMENT_SHADER = 0x8b30;
  public static final int GL_VERTEX_SHADER = 0x8b31;
  public static final int GL_TEXTURE0 = 0x84c0;
  public static final int GL_FRAMEBUFFER = 0x8d40;
  public static final int GL_READ_FRAMEBUFFER = 0x8ca8;
  public static final int GL_DRAW_FRAMEBUFFER = 0x8ca9;
  public static final int GL_COLOR_ATTACHMENT0 = 0x8ce0;
  public static final int GL_DEPTH_COMPONENT16 = 0x81a5;
  public static final int GL_DEPTH_COMPONENT24 = 0x81a6;
  public static final int GL_DEPTH_COMPONENT32 = 0x81a7;
  public static final int GL_DEPTH_ATTACHMENT = 0x8d00;
  public static final int GL_RENDERBUFFER = 0x8d41;

  //glCullFace constants
  public static final int GL_FRONT = 0x0404;
  public static final int GL_BACK = 0x0405;
  public static final int GL_FRONT_AND_BACK = 0x0408;

  /** Returns OpenGL version. ie: {3,3,0} */
  public static int[] getVersion() {
    String str = glGetString(GL_VERSION);
    if (str == null) {
      JFLog.log("Error:glGetString returned NULL");
      return new int[] {0,0};
    }
    int idx = str.indexOf(" ");
    if (idx != -1) str = str.substring(0, idx);
    String parts[] = str.split("[.]");
    int ret[] = new int[parts.length];
    for(int a=0;a<parts.length;a++) {
      ret[a] = Integer.valueOf(parts[a]);
    }
    return ret;
  }

  public static void printError(String msg) {
    int err;
    do {
      err = glGetError();
      System.out.println(msg + "=" + String.format("%x", err));
    } while (err != 0);
  }

  public static void printError() {
    printError("err");
  }

  /** Clears viewport */
  public static void clear(int clr, int width, int height) {
    float r = (clr & 0xff0000) >> 16;
    r /= 256.0f;
    float g = (clr & 0xff00) >> 8;
    g /= 256.0f;
    float b = (clr & 0xff);
    b /= 256.0f;
    glViewport(0, 0, width, height);
    glClearColor(r, g, b, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
  }

  public static native void glActiveTexture(int i1);
  public static native void glAttachShader(int i1, int i2);
  public static native void glBindBuffer(int i1, int i2);
  public static native void glBindFramebuffer(int i1, int i2);
  public static native void glBindRenderbuffer(int i1, int i2);
  public static native void glBindTexture(int i1, int i2);
  public static native void glBlendFunc(int i1, int i2);
  public static native void glBufferData(int i1, int i2, float i3[], int i4);
  public static native void glBufferData(int i1, int i2, short i3[], int i4);
  public static native void glBufferData(int i1, int i2, int i3[], int i4);
  public static native void glBufferData(int i1, int i2, byte i3[], int i4);
  public static native void glClear(int flags);
  public static native void glClearColor(float r, float g, float b, float a);
  public static native void glColorMask(boolean r, boolean g, boolean b, boolean a);
  public static native void glCompileShader(int id);
  public static native int glCreateProgram();
  public static native int glCreateShader(int type);
  public static native void glCullFace(int id);
  public static native void glDeleteBuffers(int i1, int i2[]);
  public static native void glDeleteFramebuffers(int i1, int i2[]);
  public static native void glDeleteRenderbuffers(int i1, int i2[]);
  public static native void glDeleteTextures(int i1, int i2[]);
  public static native void glDrawElements(int i1, int i2, int i3, int i4);
  public static native void glDepthFunc(int i1);
  public static native void glDisable(int id);
  public static native void glDisableVertexAttribArray(int id);
  public static native void glDepthMask(boolean state);
  public static native void glEnable(int id);
  public static native void glEnableVertexAttribArray(int id);
  public static native void glFlush();
  public static native void glFramebufferTexture2D(int i1, int i2, int i3, int i4, int i5);
  public static native void glFramebufferRenderbuffer(int i1, int i2, int i3, int i4);
  public static native void glFrontFace(int id);
  public static native int glGetAttribLocation(int i1, String str);
  public static native int glGetError();
  public static native String glGetProgramInfoLog(int id);
  public static native String glGetShaderInfoLog(int id);
  public static native String glGetString(int type);
  public static native void glGetIntegerv(int type, int i[]);
  public static native void glGenBuffers(int i1, int i2[]);
  public static native void glGenFramebuffers(int i1, int i2[]);
  public static native void glGenRenderbuffers(int i1, int i2[]);
  public static native void glGenTextures(int i1, int i2[]);
  public static native int glGetUniformLocation(int i1, String str);
  public static native void glLinkProgram(int id);
  public static native void glPixelStorei(int i1, int i2);
  public static native void glReadPixels(int i1, int i2, int i3, int i4, int i5, int i6, int px[]);
  public static native void glRenderbufferStorage(int i1, int i2, int i3, int i4);
  public static native int glShaderSource(int type, int count, String src[], int src_lengths[]);
  public static native int glStencilFunc(int func, int ref, int mask);
  public static native int glStencilMask(int mask);
  public static native int glStencilOp(int sfail, int dpfail, int dppass);
  public static native void glTexImage2D(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int px[]);
  public static native void glTexSubImage2D(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int px[]);
  public static native void glTexParameteri(int i1, int i2, int i3);
  public static native void glUseProgram(int id);
  public static native void glUniformMatrix4fv(int i1, int i2, int i3, float m[]);
  public static native void glUniform4fv(int i1, int i2, float f[]);
  public static native void glUniform3fv(int i1, int i2, float f[]);
  public static native void glUniform2fv(int i1, int i2, float f[]);
  public static native void glUniform1f(int i1, float f);
  public static native void glUniform4iv(int i1, int i2, int v[]);
  public static native void glUniform3iv(int i1, int i2, int v[]);
  public static native void glUniform2iv(int i1, int i2, int v[]);
  public static native void glUniform1i(int i1, int i2);
  public static native void glVertexAttribPointer(int i1, int i2, int i3, int i4, int i5, int i6);
  public static native void glViewport(int x,int y,int w,int h);
}
