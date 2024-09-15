//OpenGL functions

#include "register.h"

enum GLNAMES {
  GLACTIVETEXTURE,
  GLATTACHSHADER,
  GLBINDBUFFER,
  GLBINDFRAMEBUFFER,
  GLBINDRENDERBUFFER,
  GLBINDTEXTURE,
  GLBLENDFUNC,
  GLBUFFERDATA,
  GLCLEAR,
  GLCLEARCOLOR,
  GLCOLORMASK,
  GLCOMPILESHADER,
  GLCREATEPROGRAM,
  GLCREATESHADER,
  GLCULLFACE,
  GLDELETEBUFFERS,
  GLDELETEFRAMEBUFFERS,
  GLDELETERENDERBUFFERS,
  GLDELETETEXTURES,
  GLDRAWELEMENTS,
  GLDEPTHFUNC,
  GLDISABLE,
  GLDISABLEVERTEXATTRIBARRAY,
  GLDEPTHMASK,
  GLENABLE,
  GLENABLEVERTEXATTRIBARRAY,
  GLFLUSH,
  GLFRAMEBUFFERTEXTURE2D,
  GLFRAMEBUFFERRENDERBUFFER,
  GLFRONTFACE,
  GLGETATTRIBLOCATION,
  GLGETERROR,
  GLGETPROGRAMINFOLOG,
  GLGETSHADERINFOLOG,
  GLGETSTRING,
  GLGETINTEGERV,
  GLGENBUFFERS,
  GLGENFRAMEBUFFERS,
  GLGENRENDERBUFFERS,
  GLGENTEXTURES,
  GLGETUNIFORMLOCATION,
  GLLINKPROGRAM,
  GLPIXELSTOREI,
  GLREADPIXELS,
  GLRENDERBUFFERSTORAGE,
  GLSHADERSOURCE,
  GLSTENCILFUNC,
  GLSTENCILMASK,
  GLSTENCILOP,
  GLTEXIMAGE2D,
  GLTEXSUBIMAGE2D,
  GLTEXPARAMETERI,
  GLUSEPROGRAM,
  GLUNIFORMMATRIX4FV,
  GLUNIFORM4FV,
  GLUNIFORM3FV,
  GLUNIFORM2FV,
  GLUNIFORM1F,
  GLUNIFORM4IV,
  GLUNIFORM3IV,
  GLUNIFORM2IV,
  GLUNIFORM1I,
  GLVERTEXATTRIBPOINTER,
  GLVIEWPORT,

  GL_NO_FUNCS
};

struct GLFunc {
  const char *name;
  void *func;
};

GLFunc funcs[] = {
  {"glActiveTexture", NULL},
  {"glAttachShader", NULL},
  {"glBindBuffer", NULL},
  {"glBindFramebuffer", NULL},
  {"glBindRenderbuffer", NULL},
  {"glBindTexture", NULL},
  {"glBlendFunc", NULL},
  {"glBufferData", NULL},
  {"glClear", NULL},
  {"glClearColor", NULL},
  {"glColorMask", NULL},
  {"glCompileShader", NULL},
  {"glCreateProgram", NULL},
  {"glCreateShader", NULL},
  {"glCullFace", NULL},
  {"glDeleteBuffers", NULL},
  {"glDeleteFramebuffers", NULL},
  {"glDeleteRenderbuffers", NULL},
  {"glDeleteTextures", NULL},
  {"glDrawElements", NULL},
  {"glDepthFunc", NULL},
  {"glDisable", NULL},
  {"glDisableVertexAttribArray", NULL},
  {"glDepthMask", NULL},
  {"glEnable", NULL},
  {"glEnableVertexAttribArray", NULL},
  {"glFlush", NULL},
  {"glFramebufferTexture2D", NULL},
  {"glFramebufferRenderbuffer", NULL},
  {"glFrontFace", NULL},
  {"glGetAttribLocation", NULL},
  {"glGetError", NULL},
  {"glGetProgramInfoLog", NULL},
  {"glGetShaderInfoLog", NULL},
  {"glGetString", NULL},
  {"glGetIntegerv", NULL},
  {"glGenBuffers", NULL},
  {"glGenFramebuffers", NULL},
  {"glGenRenderbuffers", NULL},
  {"glGenTextures", NULL},
  {"glGetUniformLocation", NULL},
  {"glLinkProgram", NULL},
  {"glPixelStorei", NULL},
  {"glReadPixels", NULL},
  {"glRenderbufferStorage", NULL},
  {"glShaderSource", NULL},
  {"glStencilFunc", NULL},
  {"glStencilMask", NULL},
  {"glStencilOp", NULL},
  {"glTexImage2D", NULL},
  {"glTexSubImage2D", NULL},
  {"glTexParameteri", NULL},
  {"glUseProgram", NULL},
  {"glUniformMatrix4fv", NULL},
  {"glUniform4fv", NULL},
  {"glUniform3fv", NULL},
  {"glUniform2fv", NULL},
  {"glUniform1f", NULL},
  {"glUniform4iv", NULL},
  {"glUniform3iv", NULL},
  {"glUniform2iv", NULL},
  {"glUniform1i", NULL},
  {"glVertexAttribPointer", NULL},
  {"glViewport", NULL}
};

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glActiveTexture
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLACTIVETEXTURE].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glAttachShader
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLATTACHSHADER].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindBuffer
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLBINDBUFFER].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindFramebuffer
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLBINDFRAMEBUFFER].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindRenderbuffer
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLBINDRENDERBUFFER].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindTexture
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLBINDTEXTURE].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBlendFunc
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLBLENDFUNC].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3FI
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3, jint i4)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*(void (*)(int,void*,float *,int))funcs[GLBUFFERDATA].func)(i1, (void*)(jlong)i2, f3ptr, i4);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3SI
  (JNIEnv *e, jclass c, jint i1, jint i2, jshortArray s3, jint i4)
{
  jshort *s3ptr = (jshort*)e->GetPrimitiveArrayCritical(s3,NULL);
  (*(void (*)(int,void*,jshort *,int))funcs[GLBUFFERDATA].func)(i1, (void*)(jlong)i2, s3ptr, i4);
  e->ReleasePrimitiveArrayCritical(s3, s3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3II
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3, jint i4)
{
  jint *i3ptr = (jint*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*(void (*)(int,void*,jint *,int))funcs[GLBUFFERDATA].func)(i1, (void*)(jlong)i2, i3ptr, i4);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3BI
  (JNIEnv *e, jclass c, jint i1, jint i2, jbyteArray b3, jint i4)
{
  jbyte *b3ptr = (jbyte*)e->GetPrimitiveArrayCritical(b3,NULL);
  (*(void (*)(int,void*,jbyte *,int))funcs[GLBUFFERDATA].func)(i1, (void*)(jlong)i2, b3ptr, i4);
  e->ReleasePrimitiveArrayCritical(b3, b3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glClear
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLCLEAR].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glClearColor
  (JNIEnv *e, jclass c, jfloat f1, jfloat f2, jfloat f3, jfloat f4)
{
  (*(void (*)(float,float,float,float))funcs[GLCLEARCOLOR].func)(f1,f2,f3,f4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glColorMask
  (JNIEnv *e, jclass c, jboolean b1, jboolean b2, jboolean b3, jboolean b4)
{
  (*(void (*)(jboolean,jboolean,jboolean,jboolean))funcs[GLCOLORMASK].func)(b1,b2,b3,b4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glCompileShader
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLCOMPILESHADER].func)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glCreateProgram
  (JNIEnv *e, jclass c)
{
  return (*(jint (*)())funcs[GLCREATEPROGRAM].func)();
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glCreateShader
  (JNIEnv *e, jclass c, jint i1)
{
  return (*(jint (*)(int))funcs[GLCREATESHADER].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glCullFace
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLCULLFACE].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteBuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLDELETEBUFFERS].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteFramebuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLDELETEFRAMEBUFFERS].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteRenderbuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLDELETERENDERBUFFERS].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteTextures
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLDELETETEXTURES].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDrawElements
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*(void (*)(int,int,int,void*))funcs[GLDRAWELEMENTS].func)(i1,i2,i3,(void*)(jlong)i4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDepthFunc
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLDEPTHFUNC].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDisable
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLDISABLE].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDisableVertexAttribArray
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLDISABLEVERTEXATTRIBARRAY].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDepthMask
  (JNIEnv *, jclass, jboolean b1)
{
  (*(void (*)(jboolean))funcs[GLDEPTHMASK].func)(b1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glEnable
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLENABLE].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glEnableVertexAttribArray
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLENABLEVERTEXATTRIBARRAY].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFlush
  (JNIEnv *e, jclass c)
{
  (*(void (*)())funcs[GLFLUSH].func)();
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFramebufferTexture2D
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5)
{
  (*(void (*)(int,int,int,int,int))funcs[GLFRAMEBUFFERTEXTURE2D].func)(i1,i2,i3,i4,i5);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFramebufferRenderbuffer
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*(void (*)(int,int,int,int))funcs[GLFRAMEBUFFERRENDERBUFFER].func)(i1,i2,i3,i4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFrontFace
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLFRONTFACE].func)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glGetAttribLocation
  (JNIEnv *e, jclass c, jint i1, jstring s2)
{
  const char *s2ptr = e->GetStringUTFChars(s2,NULL);
  jint ret = (*(jint (*)(int,const char *))funcs[GLGETATTRIBLOCATION].func)(i1, s2ptr);
  e->ReleaseStringUTFChars(s2, s2ptr);
  return ret;
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glGetError
  (JNIEnv *e , jclass c)
{
  return (*(jint (*)())funcs[GLGETERROR].func)();
}

JNIEXPORT jstring JNICALL Java_javaforce_gl_GL_glGetProgramInfoLog
  (JNIEnv *e, jclass c, jint i1)
{
  char * log = (char*)malloc(1024);
  (*(const char* (*)(int,int,int*,char*))funcs[GLGETPROGRAMINFOLOG].func)(i1, 1024, NULL, log);
  jstring str = e->NewStringUTF(log);
  free(log);
  return str;
}

JNIEXPORT jstring JNICALL Java_javaforce_gl_GL_glGetShaderInfoLog
  (JNIEnv *e, jclass c, jint i1)
{
  char * log = (char*)malloc(1024);
  (*(const char* (*)(int,int,int*,char*))funcs[GLGETSHADERINFOLOG].func)(i1, 1024, NULL, log);
  jstring str = e->NewStringUTF(log);
  free(log);
  return str;
}

JNIEXPORT jstring JNICALL Java_javaforce_gl_GL_glGetString
  (JNIEnv *e, jclass c, jint i1)
{
  const char * cstr = (*(const char* (*)(int))funcs[GLGETSTRING].func)(i1);
  jstring str = e->NewStringUTF(cstr);
  return str;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGetIntegerv
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLGETINTEGERV].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenBuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLGENBUFFERS].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenFramebuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLGENFRAMEBUFFERS].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenRenderbuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLGENRENDERBUFFERS].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenTextures
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  jint *i2ptr = (jint*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*(void (*)(int,jint *))funcs[GLGENTEXTURES].func)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glGetUniformLocation
  (JNIEnv *e, jclass c, jint i1, jstring s2)
{
  const char *s2ptr = e->GetStringUTFChars(s2,NULL);
  jint ret = (*(jint (*)(int,const char *))funcs[GLGETUNIFORMLOCATION].func)(i1, s2ptr);
  e->ReleaseStringUTFChars(s2, s2ptr);
  return ret;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glLinkProgram
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLLINKPROGRAM].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glPixelStorei
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int,int))funcs[GLPIXELSTOREI].func)(i1,i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glReadPixels
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jintArray i7)
{
  jint *i7ptr = (jint*)e->GetPrimitiveArrayCritical(i7,NULL);
  (*(void (*)(int,int,int,int,int,int,jint *))funcs[GLREADPIXELS].func)(i1, i2, i3, i4, i5, i6, i7ptr);
  e->ReleasePrimitiveArrayCritical(i7, i7ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glRenderbufferStorage
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*(void (*)(int,int,int,int))funcs[GLRENDERBUFFERSTORAGE].func)(i1,i2,i3,i4);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glShaderSource
  (JNIEnv *e, jclass c, jint i1, jint i2, jobjectArray s3, jintArray i4)
{
  jint *i4ptr = NULL;
  if (i4 != NULL) i4ptr = (jint*)e->GetPrimitiveArrayCritical(i4,NULL);
  int s3size = e->GetArrayLength(s3);
  const char **s3ptr = (const char **)malloc(s3size * sizeof(void*));
  for(int a=0;a<s3size;a++) {
    jobject s3e = e->GetObjectArrayElement(s3, a);
    s3ptr[a] = e->GetStringUTFChars((jstring)s3e, NULL);
  }
  jint ret = (*(jint (*)(int,int,const char**,jint *))funcs[GLSHADERSOURCE].func)(i1, i2, s3ptr, i4ptr);
  for(int a=0;a<s3size;a++) {
    jobject s3e = e->GetObjectArrayElement(s3, a);
    e->ReleaseStringUTFChars((jstring)s3e, s3ptr[a]);
  }
  if (i4 != NULL) e->ReleasePrimitiveArrayCritical(i4, i4ptr, JNI_ABORT);
  free(s3ptr);
  return ret;
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glStencilFunc
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3)
{
  return (*(jint (*)(int,int,int))funcs[GLSTENCILFUNC].func)(i1,i2,i3);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glStencilMask
  (JNIEnv *e, jclass c, jint i1)
{
  return (*(jint (*)(int))funcs[GLSTENCILMASK].func)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glStencilOp
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3)
{
  return (*(jint (*)(int,int,int))funcs[GLSTENCILOP].func)(i1,i2,i3);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glTexImage2D
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jint i7, jint i8, jintArray i9)
{
  jint *i9ptr = (jint*)e->GetPrimitiveArrayCritical(i9,NULL);
  (*(void (*)(int,int,int,int,int,int,int,int,jint *))funcs[GLTEXIMAGE2D].func)(i1, i2, i3, i4, i5, i6, i7, i8, i9ptr);
  e->ReleasePrimitiveArrayCritical(i9, i9ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glTexSubImage2D
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jint i7, jint i8, jintArray i9)
{
  jint *i9ptr = (jint*)e->GetPrimitiveArrayCritical(i9,NULL);
  (*(void (*)(int,int,int,int,int,int,int,int,jint *))funcs[GLTEXSUBIMAGE2D].func)(i1, i2, i3, i4, i5, i6, i7, i8, i9ptr);
  e->ReleasePrimitiveArrayCritical(i9, i9ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glTexParameteri
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3)
{
  (*(void (*)(int,int,int))funcs[GLTEXPARAMETERI].func)(i1,i2,i3);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUseProgram
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))funcs[GLUSEPROGRAM].func)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniformMatrix4fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jfloatArray f4)
{
  float *f4ptr = (jfloat*)e->GetPrimitiveArrayCritical(f4,NULL);
  (*(void (*)(int,int,int,float *))funcs[GLUNIFORMMATRIX4FV].func)(i1, i2, i3, f4ptr);
  e->ReleasePrimitiveArrayCritical(f4, f4ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform4fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*(void (*)(int,int,float *))funcs[GLUNIFORM4FV].func)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform3fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*(void (*)(int,int,float *))funcs[GLUNIFORM3FV].func)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform2fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*(void (*)(int,int,float *))funcs[GLUNIFORM2FV].func)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform1f
  (JNIEnv *e, jclass c, jint i1, jfloat f2)
{
  (*(void (*)(int, float))funcs[GLUNIFORM1F].func)(i1, f2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform4iv
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3)
{
  jint *i3ptr = (jint*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*(void (*)(int,int,jint *))funcs[GLUNIFORM4IV].func)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform3iv
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3)
{
  jint *i3ptr = (jint*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*(void (*)(int,int,jint *))funcs[GLUNIFORM3IV].func)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform2iv
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3)
{
  jint *i3ptr = (jint*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*(void (*)(int,int,jint *))funcs[GLUNIFORM2IV].func)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform1i
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*(void (*)(int, int))funcs[GLUNIFORM1I].func)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glVertexAttribPointer
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6)
{
  (*(void (*)(int, int, int, int, int, void*))funcs[GLVERTEXATTRIBPOINTER].func)(i1, i2, i3, i4, i5, (void*)(jlong)i6);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glViewport
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*(void (*)(int, int, int, int))funcs[GLVIEWPORT].func)(i1, i2, i3, i4);
}

static JNINativeMethod javaforce_gl_GL[] = {
  {"glInit", "()Z", (void *)&Java_javaforce_gl_GL_glInit},
  {"glActiveTexture", "(I)V", (void *)&Java_javaforce_gl_GL_glActiveTexture},
  {"glAttachShader", "(II)V", (void *)&Java_javaforce_gl_GL_glAttachShader},
  {"glBindBuffer", "(II)V", (void *)&Java_javaforce_gl_GL_glBindBuffer},
  {"glBindFramebuffer", "(II)V", (void *)&Java_javaforce_gl_GL_glBindFramebuffer},
  {"glBindRenderbuffer", "(II)V", (void *)&Java_javaforce_gl_GL_glBindRenderbuffer},
  {"glBindTexture", "(II)V", (void *)&Java_javaforce_gl_GL_glBindTexture},
  {"glBlendFunc", "(II)V", (void *)&Java_javaforce_gl_GL_glBlendFunc},
  {"glBufferData", "(II[FI)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3FI},
  {"glBufferData", "(II[SI)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3SI},
  {"glBufferData", "(II[II)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3II},
  {"glBufferData", "(II[BI)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3BI},
  {"glClear", "(I)V", (void *)&Java_javaforce_gl_GL_glClear},
  {"glClearColor", "(FFFF)V", (void *)&Java_javaforce_gl_GL_glClearColor},
  {"glColorMask", "(ZZZZ)V", (void *)&Java_javaforce_gl_GL_glColorMask},
  {"glCompileShader", "(I)V", (void *)&Java_javaforce_gl_GL_glCompileShader},
  {"glCreateProgram", "()I", (void *)&Java_javaforce_gl_GL_glCreateProgram},
  {"glCreateShader", "(I)I", (void *)&Java_javaforce_gl_GL_glCreateShader},
  {"glCullFace", "(I)V", (void *)&Java_javaforce_gl_GL_glCullFace},
  {"glDeleteBuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteBuffers},
  {"glDeleteFramebuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteFramebuffers},
  {"glDeleteRenderbuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteRenderbuffers},
  {"glDeleteTextures", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteTextures},
  {"glDrawElements", "(IIII)V", (void *)&Java_javaforce_gl_GL_glDrawElements},
  {"glDepthFunc", "(I)V", (void *)&Java_javaforce_gl_GL_glDepthFunc},
  {"glDisable", "(I)V", (void *)&Java_javaforce_gl_GL_glDisable},
  {"glDisableVertexAttribArray", "(I)V", (void *)&Java_javaforce_gl_GL_glDisableVertexAttribArray},
  {"glDepthMask", "(Z)V", (void *)&Java_javaforce_gl_GL_glDepthMask},
  {"glEnable", "(I)V", (void *)&Java_javaforce_gl_GL_glEnable},
  {"glEnableVertexAttribArray", "(I)V", (void *)&Java_javaforce_gl_GL_glEnableVertexAttribArray},
  {"glFlush", "()V", (void *)&Java_javaforce_gl_GL_glFlush},
  {"glFramebufferTexture2D", "(IIIII)V", (void *)&Java_javaforce_gl_GL_glFramebufferTexture2D},
  {"glFramebufferRenderbuffer", "(IIII)V", (void *)&Java_javaforce_gl_GL_glFramebufferRenderbuffer},
  {"glFrontFace", "(I)V", (void *)&Java_javaforce_gl_GL_glFrontFace},
  {"glGetAttribLocation", "(ILjava/lang/String;)I", (void *)&Java_javaforce_gl_GL_glGetAttribLocation},
  {"glGetError", "()I", (void *)&Java_javaforce_gl_GL_glGetError},
  {"glGetProgramInfoLog", "(I)Ljava/lang/String;", (void *)&Java_javaforce_gl_GL_glGetProgramInfoLog},
  {"glGetShaderInfoLog", "(I)Ljava/lang/String;", (void *)&Java_javaforce_gl_GL_glGetShaderInfoLog},
  {"glGetString", "(I)Ljava/lang/String;", (void *)&Java_javaforce_gl_GL_glGetString},
  {"glGetIntegerv", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGetIntegerv},
  {"glGenBuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenBuffers},
  {"glGenFramebuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenFramebuffers},
  {"glGenRenderbuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenRenderbuffers},
  {"glGenTextures", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenTextures},
  {"glGetUniformLocation", "(ILjava/lang/String;)I", (void *)&Java_javaforce_gl_GL_glGetUniformLocation},
  {"glLinkProgram", "(I)V", (void *)&Java_javaforce_gl_GL_glLinkProgram},
  {"glPixelStorei", "(II)V", (void *)&Java_javaforce_gl_GL_glPixelStorei},
  {"glReadPixels", "(IIIIII[I)V", (void *)&Java_javaforce_gl_GL_glReadPixels},
  {"glRenderbufferStorage", "(IIII)V", (void *)&Java_javaforce_gl_GL_glRenderbufferStorage},
  {"glShaderSource", "(II[Ljava/lang/String;[I)I", (void *)&Java_javaforce_gl_GL_glShaderSource},
  {"glStencilFunc", "(III)I", (void *)&Java_javaforce_gl_GL_glStencilFunc},
  {"glStencilMask", "(I)I", (void *)&Java_javaforce_gl_GL_glStencilMask},
  {"glStencilOp", "(III)I", (void *)&Java_javaforce_gl_GL_glStencilOp},
  {"glTexImage2D", "(IIIIIIII[I)V", (void *)&Java_javaforce_gl_GL_glTexImage2D},
  {"glTexSubImage2D", "(IIIIIIII[I)V", (void *)&Java_javaforce_gl_GL_glTexSubImage2D},
  {"glTexParameteri", "(III)V", (void *)&Java_javaforce_gl_GL_glTexParameteri},
  {"glUseProgram", "(I)V", (void *)&Java_javaforce_gl_GL_glUseProgram},
  {"glUniformMatrix4fv", "(III[F)V", (void *)&Java_javaforce_gl_GL_glUniformMatrix4fv},
  {"glUniform4fv", "(II[F)V", (void *)&Java_javaforce_gl_GL_glUniform4fv},
  {"glUniform3fv", "(II[F)V", (void *)&Java_javaforce_gl_GL_glUniform3fv},
  {"glUniform2fv", "(II[F)V", (void *)&Java_javaforce_gl_GL_glUniform2fv},
  {"glUniform1f", "(IF)V", (void *)&Java_javaforce_gl_GL_glUniform1f},
  {"glUniform4iv", "(II[I)V", (void *)&Java_javaforce_gl_GL_glUniform4iv},
  {"glUniform3iv", "(II[I)V", (void *)&Java_javaforce_gl_GL_glUniform3iv},
  {"glUniform2iv", "(II[I)V", (void *)&Java_javaforce_gl_GL_glUniform2iv},
  {"glUniform1i", "(II)V", (void *)&Java_javaforce_gl_GL_glUniform1i},
  {"glVertexAttribPointer", "(IIIIII)V", (void *)&Java_javaforce_gl_GL_glVertexAttribPointer},
  {"glViewport", "(IIII)V", (void *)&Java_javaforce_gl_GL_glViewport},
};

extern "C" void gl_register(JNIEnv *env);

void gl_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/gl/GL");
  registerNatives(env, cls, javaforce_gl_GL, sizeof(javaforce_gl_GL)/sizeof(JNINativeMethod));
}
