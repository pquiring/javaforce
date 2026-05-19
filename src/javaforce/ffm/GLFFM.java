package javaforce.ffm;

/** GL FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.gl.*;

public class GLFFM implements GL {

  private FFM ffm;

  private static GLFFM instance;
  public static GLFFM getInstance() {
    if (instance == null) {
      instance = new GLFFM();
      if (!instance.ffm_init()) {
        JFLog.log("GLFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle glActiveTexture;
  public void glActiveTexture(int i1) { try { glActiveTexture.invokeExact(i1); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glAlphaFunc;
  public void glAlphaFunc(int i1,int i2) { try { glAlphaFunc.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glAttachShader;
  public void glAttachShader(int i1,int i2) { try { glAttachShader.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glBindBuffer;
  public void glBindBuffer(int i1,int i2) { try { glBindBuffer.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glBindFramebuffer;
  public void glBindFramebuffer(int i1,int i2) { try { glBindFramebuffer.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glBindRenderbuffer;
  public void glBindRenderbuffer(int i1,int i2) { try { glBindRenderbuffer.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glBindTexture;
  public void glBindTexture(int i1,int i2) { try { glBindTexture.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glBlendFunc;
  public void glBlendFunc(int i1,int i2) { try { glBlendFunc.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glBufferData;
  public void glBufferData(int i1,int i2,float[] i3,int i4) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i3 = FFM.toMemory(arena, i3);glBufferData.invokeExact(i1,i2,_array_i3,i4);FFM.copyBack(_array_i3,i3); } catch (Throwable t) { JFLog.log(t); } }

  public void glBufferData(int i1,int i2,short[] i3,int i4) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i3 = FFM.toMemory(arena, i3);glBufferData.invokeExact(i1,i2,_array_i3,i4);FFM.copyBack(_array_i3,i3); } catch (Throwable t) { JFLog.log(t); } }

  public void glBufferData(int i1,int i2,int[] i3,int i4) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i3 = FFM.toMemory(arena, i3);glBufferData.invokeExact(i1,i2,_array_i3,i4);FFM.copyBack(_array_i3,i3); } catch (Throwable t) { JFLog.log(t); } }

  public void glBufferData(int i1,int i2,byte[] i3,int i4) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i3 = FFM.toMemory(arena, i3);glBufferData.invokeExact(i1,i2,_array_i3,i4);FFM.copyBack(_array_i3,i3); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glClear;
  public void glClear(int flags) { try { glClear.invokeExact(flags); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glClearColor;
  public void glClearColor(float r,float g,float b,float a) { try { glClearColor.invokeExact(r,g,b,a); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glColorMask;
  public void glColorMask(boolean r,boolean g,boolean b,boolean a) { try { glColorMask.invokeExact(r,g,b,a); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glClearStencil;
  public void glClearStencil(int s) { try { glClearStencil.invokeExact(s); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glCompileShader;
  public void glCompileShader(int id) { try { glCompileShader.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glCreateProgram;
  public int glCreateProgram() { try { int _ret_value_ = (int)glCreateProgram.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glCreateShader;
  public int glCreateShader(int type) { try { int _ret_value_ = (int)glCreateShader.invokeExact(type);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glCullFace;
  public void glCullFace(int id) { try { glCullFace.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDeleteBuffers;
  public void glDeleteBuffers(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glDeleteBuffers.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDeleteFramebuffers;
  public void glDeleteFramebuffers(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glDeleteFramebuffers.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDeleteRenderbuffers;
  public void glDeleteRenderbuffers(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glDeleteRenderbuffers.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDeleteTextures;
  public void glDeleteTextures(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glDeleteTextures.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDrawElements;
  public void glDrawElements(int i1,int i2,int i3,int i4) { try { glDrawElements.invokeExact(i1,i2,i3,i4); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDepthFunc;
  public void glDepthFunc(int i1) { try { glDepthFunc.invokeExact(i1); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDisable;
  public void glDisable(int id) { try { glDisable.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDisableVertexAttribArray;
  public void glDisableVertexAttribArray(int id) { try { glDisableVertexAttribArray.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glDepthMask;
  public void glDepthMask(boolean state) { try { glDepthMask.invokeExact(state); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glEnable;
  public void glEnable(int id) { try { glEnable.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glEnableVertexAttribArray;
  public void glEnableVertexAttribArray(int id) { try { glEnableVertexAttribArray.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glFlush;
  public void glFlush() { try { glFlush.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glFramebufferTexture2D;
  public void glFramebufferTexture2D(int i1,int i2,int i3,int i4,int i5) { try { glFramebufferTexture2D.invokeExact(i1,i2,i3,i4,i5); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glFramebufferRenderbuffer;
  public void glFramebufferRenderbuffer(int i1,int i2,int i3,int i4) { try { glFramebufferRenderbuffer.invokeExact(i1,i2,i3,i4); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glFrontFace;
  public void glFrontFace(int id) { try { glFrontFace.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glGetAttribLocation;
  public int glGetAttribLocation(int i1,String str) { try { Arena arena = Arena.ofAuto(); int _ret_value_ = (int)glGetAttribLocation.invokeExact(i1,arena.allocateFrom(str));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glGetError;
  public int glGetError() { try { int _ret_value_ = (int)glGetError.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glGetProgramInfoLog;
  public String glGetProgramInfoLog(int id) { try { String _ret_value_ = FFM.getString((MemorySegment)glGetProgramInfoLog.invokeExact(id));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle glGetShaderInfoLog;
  public String glGetShaderInfoLog(int id) { try { String _ret_value_ = FFM.getString((MemorySegment)glGetShaderInfoLog.invokeExact(id));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle glGetString;
  public String glGetString(int type) { try { String _ret_value_ = FFM.getString((MemorySegment)glGetString.invokeExact(type));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle glGetIntegerv;
  public void glGetIntegerv(int type,int[] i) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i = FFM.toMemory(arena, i);glGetIntegerv.invokeExact(type,_array_i);FFM.copyBack(_array_i,i); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glGenBuffers;
  public void glGenBuffers(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glGenBuffers.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glGenFramebuffers;
  public void glGenFramebuffers(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glGenFramebuffers.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glGenRenderbuffers;
  public void glGenRenderbuffers(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glGenRenderbuffers.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glGenTextures;
  public void glGenTextures(int i1,int[] i2) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_i2 = FFM.toMemory(arena, i2);glGenTextures.invokeExact(i1,_array_i2);FFM.copyBack(_array_i2,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glGetUniformLocation;
  public int glGetUniformLocation(int i1,String str) { try { Arena arena = Arena.ofAuto(); int _ret_value_ = (int)glGetUniformLocation.invokeExact(i1,arena.allocateFrom(str));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glLinkProgram;
  public void glLinkProgram(int id) { try { glLinkProgram.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glPixelStorei;
  public void glPixelStorei(int i1,int i2) { try { glPixelStorei.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glReadPixels;
  public void glReadPixels(int i1,int i2,int i3,int i4,int i5,int i6,int[] px) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_px = FFM.toMemory(arena, px);glReadPixels.invokeExact(i1,i2,i3,i4,i5,i6,_array_px);FFM.copyBack(_array_px,px); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glRenderbufferStorage;
  public void glRenderbufferStorage(int i1,int i2,int i3,int i4) { try { glRenderbufferStorage.invokeExact(i1,i2,i3,i4); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glShaderSource;
  public int glShaderSource(int type,int count,String[] src,int[] src_lengths) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_src = FFM.toMemory(arena, src);MemorySegment _array_src_lengths = FFM.toMemory(arena, src_lengths);int _ret_value_ = (int)glShaderSource.invokeExact(type,count,_array_src,_array_src_lengths);FFM.copyBack(_array_src,src);FFM.copyBack(_array_src_lengths,src_lengths);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glStencilFunc;
  public int glStencilFunc(int func,int ref,int mask) { try { int _ret_value_ = (int)glStencilFunc.invokeExact(func,ref,mask);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glStencilMask;
  public int glStencilMask(int mask) { try { int _ret_value_ = (int)glStencilMask.invokeExact(mask);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glStencilOp;
  public int glStencilOp(int sfail,int dpfail,int dppass) { try { int _ret_value_ = (int)glStencilOp.invokeExact(sfail,dpfail,dppass);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle glTexImage2D;
  public void glTexImage2D(int i1,int i2,int i3,int i4,int i5,int i6,int i7,int i8,int[] px) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_px = FFM.toMemory(arena, px);glTexImage2D.invokeExact(i1,i2,i3,i4,i5,i6,i7,i8,_array_px);FFM.copyBack(_array_px,px); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glTexSubImage2D;
  public void glTexSubImage2D(int i1,int i2,int i3,int i4,int i5,int i6,int i7,int i8,int[] px) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_px = FFM.toMemory(arena, px);glTexSubImage2D.invokeExact(i1,i2,i3,i4,i5,i6,i7,i8,_array_px);FFM.copyBack(_array_px,px); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glTexParameteri;
  public void glTexParameteri(int i1,int i2,int i3) { try { glTexParameteri.invokeExact(i1,i2,i3); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUseProgram;
  public void glUseProgram(int id) { try { glUseProgram.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniformMatrix4fv;
  public void glUniformMatrix4fv(int i1,int i2,int i3,float[] m) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_m = FFM.toMemory(arena, m);glUniformMatrix4fv.invokeExact(i1,i2,i3,_array_m);FFM.copyBack(_array_m,m); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform4fv;
  public void glUniform4fv(int i1,int i2,float[] f) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_f = FFM.toMemory(arena, f);glUniform4fv.invokeExact(i1,i2,_array_f);FFM.copyBack(_array_f,f); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform3fv;
  public void glUniform3fv(int i1,int i2,float[] f) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_f = FFM.toMemory(arena, f);glUniform3fv.invokeExact(i1,i2,_array_f);FFM.copyBack(_array_f,f); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform2fv;
  public void glUniform2fv(int i1,int i2,float[] f) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_f = FFM.toMemory(arena, f);glUniform2fv.invokeExact(i1,i2,_array_f);FFM.copyBack(_array_f,f); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform1f;
  public void glUniform1f(int i1,float f) { try { glUniform1f.invokeExact(i1,f); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform4iv;
  public void glUniform4iv(int i1,int i2,int[] v) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_v = FFM.toMemory(arena, v);glUniform4iv.invokeExact(i1,i2,_array_v);FFM.copyBack(_array_v,v); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform3iv;
  public void glUniform3iv(int i1,int i2,int[] v) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_v = FFM.toMemory(arena, v);glUniform3iv.invokeExact(i1,i2,_array_v);FFM.copyBack(_array_v,v); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform2iv;
  public void glUniform2iv(int i1,int i2,int[] v) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_v = FFM.toMemory(arena, v);glUniform2iv.invokeExact(i1,i2,_array_v);FFM.copyBack(_array_v,v); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glUniform1i;
  public void glUniform1i(int i1,int i2) { try { glUniform1i.invokeExact(i1,i2); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glVertexAttribPointer;
  public void glVertexAttribPointer(int i1,int i2,int i3,int i4,int i5,int i6) { try { glVertexAttribPointer.invokeExact(i1,i2,i3,i4,i5,i6); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle glViewport;
  public void glViewport(int x,int y,int w,int h) { try { glViewport.invokeExact(x,y,w,h); } catch (Throwable t) { JFLog.log(t); } }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("GLinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    glActiveTexture = ffm.getFunctionPtr("_glActiveTexture", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glAlphaFunc = ffm.getFunctionPtr("_glAlphaFunc", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glAttachShader = ffm.getFunctionPtr("_glAttachShader", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glBindBuffer = ffm.getFunctionPtr("_glBindBuffer", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glBindFramebuffer = ffm.getFunctionPtr("_glBindFramebuffer", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glBindRenderbuffer = ffm.getFunctionPtr("_glBindRenderbuffer", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glBindTexture = ffm.getFunctionPtr("_glBindTexture", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glBlendFunc = ffm.getFunctionPtr("_glBlendFunc", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glBufferData = ffm.getFunctionPtr("_glBufferData", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS,JAVA_INT));
    glClear = ffm.getFunctionPtr("_glClear", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glClearColor = ffm.getFunctionPtr("_glClearColor", ffm.getFunctionDesciptorVoid(JAVA_FLOAT,JAVA_FLOAT,JAVA_FLOAT,JAVA_FLOAT));
    glColorMask = ffm.getFunctionPtr("_glColorMask", ffm.getFunctionDesciptorVoid(JAVA_BOOLEAN,JAVA_BOOLEAN,JAVA_BOOLEAN,JAVA_BOOLEAN));
    glClearStencil = ffm.getFunctionPtr("_glClearStencil", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glCompileShader = ffm.getFunctionPtr("_glCompileShader", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glCreateProgram = ffm.getFunctionPtr("_glCreateProgram", ffm.getFunctionDesciptor(JAVA_INT));
    glCreateShader = ffm.getFunctionPtr("_glCreateShader", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT));
    glCullFace = ffm.getFunctionPtr("_glCullFace", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glDeleteBuffers = ffm.getFunctionPtr("_glDeleteBuffers", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glDeleteFramebuffers = ffm.getFunctionPtr("_glDeleteFramebuffers", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glDeleteRenderbuffers = ffm.getFunctionPtr("_glDeleteRenderbuffers", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glDeleteTextures = ffm.getFunctionPtr("_glDeleteTextures", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glDrawElements = ffm.getFunctionPtr("_glDrawElements", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glDepthFunc = ffm.getFunctionPtr("_glDepthFunc", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glDisable = ffm.getFunctionPtr("_glDisable", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glDisableVertexAttribArray = ffm.getFunctionPtr("_glDisableVertexAttribArray", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glDepthMask = ffm.getFunctionPtr("_glDepthMask", ffm.getFunctionDesciptorVoid(JAVA_BOOLEAN));
    glEnable = ffm.getFunctionPtr("_glEnable", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glEnableVertexAttribArray = ffm.getFunctionPtr("_glEnableVertexAttribArray", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glFlush = ffm.getFunctionPtr("_glFlush", ffm.getFunctionDesciptorVoid());
    glFramebufferTexture2D = ffm.getFunctionPtr("_glFramebufferTexture2D", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glFramebufferRenderbuffer = ffm.getFunctionPtr("_glFramebufferRenderbuffer", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glFrontFace = ffm.getFunctionPtr("_glFrontFace", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glGetAttribLocation = ffm.getFunctionPtr("_glGetAttribLocation", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT,ADDRESS));
    glGetError = ffm.getFunctionPtr("_glGetError", ffm.getFunctionDesciptor(JAVA_INT));
    glGetProgramInfoLog = ffm.getFunctionPtr("_glGetProgramInfoLog", ffm.getFunctionDesciptor(ADDRESS,JAVA_INT));
    glGetShaderInfoLog = ffm.getFunctionPtr("_glGetShaderInfoLog", ffm.getFunctionDesciptor(ADDRESS,JAVA_INT));
    glGetString = ffm.getFunctionPtr("_glGetString", ffm.getFunctionDesciptor(ADDRESS,JAVA_INT));
    glGetIntegerv = ffm.getFunctionPtr("_glGetIntegerv", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glGenBuffers = ffm.getFunctionPtr("_glGenBuffers", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glGenFramebuffers = ffm.getFunctionPtr("_glGenFramebuffers", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glGenRenderbuffers = ffm.getFunctionPtr("_glGenRenderbuffers", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glGenTextures = ffm.getFunctionPtr("_glGenTextures", ffm.getFunctionDesciptorVoid(JAVA_INT,ADDRESS));
    glGetUniformLocation = ffm.getFunctionPtr("_glGetUniformLocation", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT,ADDRESS));
    glLinkProgram = ffm.getFunctionPtr("_glLinkProgram", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glPixelStorei = ffm.getFunctionPtr("_glPixelStorei", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glReadPixels = ffm.getFunctionPtr("_glReadPixels", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,ADDRESS));
    glRenderbufferStorage = ffm.getFunctionPtr("_glRenderbufferStorage", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glShaderSource = ffm.getFunctionPtr("_glShaderSource", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT,JAVA_INT,ADDRESS,ADDRESS));
    glStencilFunc = ffm.getFunctionPtr("_glStencilFunc", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glStencilMask = ffm.getFunctionPtr("_glStencilMask", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT));
    glStencilOp = ffm.getFunctionPtr("_glStencilOp", ffm.getFunctionDesciptor(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glTexImage2D = ffm.getFunctionPtr("_glTexImage2D", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,ADDRESS));
    glTexSubImage2D = ffm.getFunctionPtr("_glTexSubImage2D", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,ADDRESS));
    glTexParameteri = ffm.getFunctionPtr("_glTexParameteri", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT));
    glUseProgram = ffm.getFunctionPtr("_glUseProgram", ffm.getFunctionDesciptorVoid(JAVA_INT));
    glUniformMatrix4fv = ffm.getFunctionPtr("_glUniformMatrix4fv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,ADDRESS));
    glUniform4fv = ffm.getFunctionPtr("_glUniform4fv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS));
    glUniform3fv = ffm.getFunctionPtr("_glUniform3fv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS));
    glUniform2fv = ffm.getFunctionPtr("_glUniform2fv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS));
    glUniform1f = ffm.getFunctionPtr("_glUniform1f", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_FLOAT));
    glUniform4iv = ffm.getFunctionPtr("_glUniform4iv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS));
    glUniform3iv = ffm.getFunctionPtr("_glUniform3iv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS));
    glUniform2iv = ffm.getFunctionPtr("_glUniform2iv", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,ADDRESS));
    glUniform1i = ffm.getFunctionPtr("_glUniform1i", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT));
    glVertexAttribPointer = ffm.getFunctionPtr("_glVertexAttribPointer", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    glViewport = ffm.getFunctionPtr("_glViewport", ffm.getFunctionDesciptorVoid(JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    return true;
  }
}
