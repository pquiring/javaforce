package javaforce.jni;

import javaforce.ffm.*;
import javaforce.gl.*;

/** JNI implementation of GL. */

public class GLJNI implements GL {

  private static GLJNI instance;

  public static synchronized GL getInstance() {
    if (instance == null) {
      instance = new GLJNI();
      instance.init();
    }
    return instance;
  }

  private native boolean init();

  public native void glActiveTexture(int i1);
  public native void glAlphaFunc(int i1, int i2);
  public native void glAttachShader(int i1, int i2);
  public native void glBindBuffer(int i1, int i2);
  public native void glBindFramebuffer(int i1, int i2);
  public native void glBindRenderbuffer(int i1, int i2);
  public native void glBindTexture(int i1, int i2);
  public native void glBlendFunc(int i1, int i2);
  public native void glBufferData(int i1, int i2, float[] i3, int i4);
  public native void glBufferData(int i1, int i2, short[] i3, int i4);
  public native void glBufferData(int i1, int i2, int[] i3, int i4);
  public native void glBufferData(int i1, int i2, byte[] i3, int i4);
  public native void glClear(int flags);
  public native void glClearColor(float r, float g, float b, float a);
  public native void glColorMask(boolean r, boolean g, boolean b, boolean a);
  public native void glClearStencil(int s);
  public native void glCompileShader(int id);
  public native int glCreateProgram();
  public native int glCreateShader(int type);
  public native void glCullFace(int id);
  public native void glDeleteBuffers(int i1, int[] i2);
  public native void glDeleteFramebuffers(int i1, int[] i2);
  public native void glDeleteRenderbuffers(int i1, int[] i2);
  public native void glDeleteTextures(int i1, int[] i2);
  public native void glDrawElements(int i1, int i2, int i3, int i4);
  public native void glDepthFunc(int i1);
  public native void glDisable(int id);
  public native void glDisableVertexAttribArray(int id);
  public native void glDepthMask(boolean state);
  public native void glEnable(int id);
  public native void glEnableVertexAttribArray(int id);
  public native void glFlush();
  public native void glFramebufferTexture2D(int i1, int i2, int i3, int i4, int i5);
  public native void glFramebufferRenderbuffer(int i1, int i2, int i3, int i4);
  public native void glFrontFace(int id);
  public native int glGetAttribLocation(int i1, String str);
  public native int glGetError();
  public native String glGetProgramInfoLog(int id);
  public native String glGetShaderInfoLog(int id);
  @NoFreeString
  public native String glGetString(int type);
  public native void glGetIntegerv(int type, int[] i);
  public native void glGenBuffers(int i1, int[] i2);
  public native void glGenFramebuffers(int i1, int[] i2);
  public native void glGenRenderbuffers(int i1, int[] i2);
  public native void glGenTextures(int i1, int[] i2);
  public native int glGetUniformLocation(int i1, String str);
  public native void glLinkProgram(int id);
  public native void glPixelStorei(int i1, int i2);
  public native void glReadPixels(int i1, int i2, int i3, int i4, int i5, int i6, int[] px);
  public native void glRenderbufferStorage(int i1, int i2, int i3, int i4);
  public native int glShaderSource(int type, int count, String[] src, int[] src_lengths);
  public native int glStencilFunc(int func, int ref, int mask);
  public native int glStencilMask(int mask);
  public native int glStencilOp(int sfail, int dpfail, int dppass);
  public native void glTexImage2D(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int[] px);
  public native void glTexSubImage2D(int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int[] px);
  public native void glTexParameteri(int i1, int i2, int i3);
  public native void glUseProgram(int id);
  public native void glUniformMatrix4fv(int i1, int i2, int i3, float[] m);
  public native void glUniform4fv(int i1, int i2, float[] f);
  public native void glUniform3fv(int i1, int i2, float[] f);
  public native void glUniform2fv(int i1, int i2, float[] f);
  public native void glUniform1f(int i1, float f);
  public native void glUniform4iv(int i1, int i2, int[] v);
  public native void glUniform3iv(int i1, int i2, int[] v);
  public native void glUniform2iv(int i1, int i2, int[] v);
  public native void glUniform1i(int i1, int i2);
  public native void glVertexAttribPointer(int i1, int i2, int i3, int i4, int i5, int i6);
  public native void glViewport(int x,int y,int w,int h);
}
