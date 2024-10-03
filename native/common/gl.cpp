//OpenGL functions

#include "register.h"

jboolean glPlatformInit();
jboolean glGetFunction(void **funcPtr, const char *name);  //platform impl

void (*_glActiveTexture)(int);
void (*_glAttachShader)(int,int);
void (*_glBindBuffer)(int,int);
void (*_glBindFramebuffer)(int,int);
void (*_glBindRenderbuffer)(int,int);
void (*_glBindTexture)(int,int);
void (*_glBlendFunc)(int,int);
void (*_glBufferData)(int,void*,void *,int);
void (*_glClear)(int);
void (*_glClearColor)(float,float,float,float);
void (*_glColorMask)(jboolean,jboolean,jboolean,jboolean);
void (*_glCompileShader)(int);
int (*_glCreateProgram)();
int (*_glCreateShader)(int);
void (*_glCullFace)(int);
void (*_glDeleteBuffers)(int,int *);
void (*_glDeleteFramebuffers)(int,int *);
void (*_glDeleteRenderbuffers)(int,int *);
void (*_glDeleteTextures)(int,int *);
void (*_glDrawElements)(int,int,int,void*);
void (*_glDepthFunc)(int);
void (*_glDisable)(int);
void (*_glDisableVertexAttribArray)(int);
void (*_glDepthMask)();
void (*_glEnable)();
void (*_glEnableVertexAttribArray)();
void (*_glFlush)();
void (*_glFramebufferTexture2D)(int,int,int,int,int);
void (*_glFramebufferRenderbuffer)(int,int,int,int);
void (*_glFrontFace)(int);
int (*_glGetAttribLocation)(int,const char *);
int (*_glGetError)();
const char* (*_glGetProgramInfoLog)(int,int,int*,char*);
const char* (*_glGetShaderInfoLog)(int,int,int*,char*);
const char* (*_glGetString)(int);
void (*_glGetIntegerv)(int,int *);
void (*_glGenBuffers)(int,int *);
void (*_glGenFramebuffers)(int,int *);
void (*_glGenRenderbuffers)(int,int *);
void (*_glGenTextures)(int,int *);
int (*_glGetUniformLocation)(int,const char *);
void (*_glLinkProgram)(int);
void (*_glPixelStorei)(int,int);
void (*_glReadPixels)(int,int,int,int,int,int,int *);
void (*_glRenderbufferStorage)(int,int,int,int);
int (*_glShaderSource)(int,int,const char**,int *);
int (*_glStencilFunc)(int,int,int);
int (*_glStencilMask)(int);
int (*_glStencilOp)(int,int,int);
void (*_glTexImage2D)(int,int,int,int,int,int,int,int,int *);
void (*_glTexSubImage2D)(int,int,int,int,int,int,int,int,int *);
void (*_glTexParameteri)(int,int,int);
void (*_glUseProgram)(int);
void (*_glUniformMatrix4fv)(int,int,int,float *);
void (*_glUniform4fv)(int,int,float *);
void (*_glUniform3fv)(int,int,float *);
void (*_glUniform2fv)(int,int,float *);
void (*_glUniform1f)(int, float);
void (*_glUniform4iv)(int,int,int *);
void (*_glUniform3iv)(int,int,int *);
void (*_glUniform2iv)(int,int,int *);
void (*_glUniform1i)(int,int);
void (*_glVertexAttribPointer)(int, int, int, int, int, void*);
void (*_glViewport)(int, int, int, int);

JNIEXPORT jboolean JNICALL Java_javaforce_gl_GL_init
  (JNIEnv *e, jclass c)
{
  if (!glPlatformInit()) return JNI_FALSE;
  glGetFunction((void**)&_glActiveTexture,"glActiveTexture");
  glGetFunction((void**)&_glAttachShader,"glAttachShader");
  glGetFunction((void**)&_glBindBuffer,"glBindBuffer");
  glGetFunction((void**)&_glBindFramebuffer,"glBindFramebuffer");
  glGetFunction((void**)&_glBindRenderbuffer,"glBindRenderbuffer");
  glGetFunction((void**)&_glBindTexture,"glBindTexture");
  glGetFunction((void**)&_glBlendFunc,"glBlendFunc");
  glGetFunction((void**)&_glBufferData,"glBufferData");
  glGetFunction((void**)&_glClear,"glClear");
  glGetFunction((void**)&_glClearColor,"glClearColor");
  glGetFunction((void**)&_glColorMask,"glColorMask");
  glGetFunction((void**)&_glCompileShader,"glCompileShader");
  glGetFunction((void**)&_glCreateProgram,"glCreateProgram");
  glGetFunction((void**)&_glCreateShader,"glCreateShader");
  glGetFunction((void**)&_glCullFace,"glCullFace");
  glGetFunction((void**)&_glDeleteBuffers,"glDeleteBuffers");
  glGetFunction((void**)&_glDeleteFramebuffers,"glDeleteFramebuffers");
  glGetFunction((void**)&_glDeleteRenderbuffers,"glDeleteRenderbuffers");
  glGetFunction((void**)&_glDeleteTextures,"glDeleteTextures");
  glGetFunction((void**)&_glDrawElements,"glDrawElements");
  glGetFunction((void**)&_glDepthFunc,"glDepthFunc");
  glGetFunction((void**)&_glDisable,"glDisable");
  glGetFunction((void**)&_glDisableVertexAttribArray,"glDisableVertexAttribArray");
  glGetFunction((void**)&_glDepthMask,"glDepthMask");
  glGetFunction((void**)&_glEnable,"glEnable");
  glGetFunction((void**)&_glEnableVertexAttribArray,"glEnableVertexAttribArray");
  glGetFunction((void**)&_glFlush,"glFlush");
  glGetFunction((void**)&_glFramebufferTexture2D,"glFramebufferTexture2D");
  glGetFunction((void**)&_glFramebufferRenderbuffer,"glFramebufferRenderbuffer");
  glGetFunction((void**)&_glFrontFace,"glFrontFace");
  glGetFunction((void**)&_glGetAttribLocation,"glGetAttribLocation");
  glGetFunction((void**)&_glGetError,"glGetError");
  glGetFunction((void**)&_glGetProgramInfoLog,"glGetProgramInfoLog");
  glGetFunction((void**)&_glGetShaderInfoLog,"glGetShaderInfoLog");
  glGetFunction((void**)&_glGetString,"glGetString");
  glGetFunction((void**)&_glGetIntegerv,"glGetIntegerv");
  glGetFunction((void**)&_glGenBuffers,"glGenBuffers");
  glGetFunction((void**)&_glGenFramebuffers,"glGenFramebuffers");
  glGetFunction((void**)&_glGenRenderbuffers,"glGenRenderbuffers");
  glGetFunction((void**)&_glGenTextures,"glGenTextures");
  glGetFunction((void**)&_glGetUniformLocation,"glGetUniformLocation");
  glGetFunction((void**)&_glLinkProgram,"glLinkProgram");
  glGetFunction((void**)&_glPixelStorei,"glPixelStorei");
  glGetFunction((void**)&_glReadPixels,"glReadPixels");
  glGetFunction((void**)&_glRenderbufferStorage,"glRenderbufferStorage");
  glGetFunction((void**)&_glShaderSource,"glShaderSource");
  glGetFunction((void**)&_glStencilFunc,"glStencilFunc");
  glGetFunction((void**)&_glStencilMask,"glStencilMask");
  glGetFunction((void**)&_glStencilOp,"glStencilOp");
  glGetFunction((void**)&_glTexImage2D,"glTexImage2D");
  glGetFunction((void**)&_glTexSubImage2D,"glTexSubImage2D");
  glGetFunction((void**)&_glTexParameteri,"glTexParameteri");
  glGetFunction((void**)&_glUseProgram,"glUseProgram");
  glGetFunction((void**)&_glUniformMatrix4fv,"glUniformMatrix4fv");
  glGetFunction((void**)&_glUniform4fv,"glUniform4fv");
  glGetFunction((void**)&_glUniform3fv,"glUniform3fv");
  glGetFunction((void**)&_glUniform2fv,"glUniform2fv");
  glGetFunction((void**)&_glUniform1f,"glUniform1f");
  glGetFunction((void**)&_glUniform4iv,"glUniform4iv");
  glGetFunction((void**)&_glUniform3iv,"glUniform3iv");
  glGetFunction((void**)&_glUniform2iv,"glUniform2iv");
  glGetFunction((void**)&_glUniform1i,"glUniform1i");
  glGetFunction((void**)&_glVertexAttribPointer,"glVertexAttribPointer");
  glGetFunction((void**)&_glViewport,"glViewport");
  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glActiveTexture
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glActiveTexture)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glAttachShader
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glAttachShader)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindBuffer
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glBindBuffer)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindFramebuffer
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glBindFramebuffer)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindRenderbuffer
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glBindRenderbuffer)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBindTexture
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glBindTexture)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBlendFunc
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glBlendFunc)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3FI
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3, jint i4)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)f3ptr, i4);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3SI
  (JNIEnv *e, jclass c, jint i1, jint i2, jshortArray s3, jint i4)
{
  jshort *s3ptr = (jshort*)e->GetPrimitiveArrayCritical(s3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)s3ptr, i4);
  e->ReleasePrimitiveArrayCritical(s3, s3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3II
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3, jint i4)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)i3ptr, i4);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glBufferData__II_3BI
  (JNIEnv *e, jclass c, jint i1, jint i2, jbyteArray b3, jint i4)
{
  jbyte *b3ptr = (jbyte*)e->GetPrimitiveArrayCritical(b3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)b3ptr, i4);
  e->ReleasePrimitiveArrayCritical(b3, b3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glClear
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glClear)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glClearColor
  (JNIEnv *e, jclass c, jfloat f1, jfloat f2, jfloat f3, jfloat f4)
{
  (*_glClearColor)(f1,f2,f3,f4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glColorMask
  (JNIEnv *e, jclass c, jboolean b1, jboolean b2, jboolean b3, jboolean b4)
{
  (*_glColorMask)(b1,b2,b3,b4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glCompileShader
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glCompileShader)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glCreateProgram
  (JNIEnv *e, jclass c)
{
  return (*_glCreateProgram)();
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glCreateShader
  (JNIEnv *e, jclass c, jint i1)
{
  return (*_glCreateShader)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glCullFace
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glCullFace)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteBuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteBuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteFramebuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteFramebuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteRenderbuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteRenderbuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDeleteTextures
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteTextures)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDrawElements
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glDrawElements)(i1,i2,i3,(void*)(jlong)i4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDepthFunc
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glDepthFunc)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDisable
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glDisable)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDisableVertexAttribArray
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glDisableVertexAttribArray)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDepthMask
  (JNIEnv *, jclass, jboolean b1)
{
  (*(void (*)(jboolean))_glDepthMask)(b1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glEnable
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))_glEnable)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glEnableVertexAttribArray
  (JNIEnv *e, jclass c, jint i1)
{
  (*(void (*)(int))_glEnableVertexAttribArray)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFlush
  (JNIEnv *e, jclass c)
{
  (*_glFlush)();
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFramebufferTexture2D
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5)
{
  (*_glFramebufferTexture2D)(i1,i2,i3,i4,i5);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFramebufferRenderbuffer
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glFramebufferRenderbuffer)(i1,i2,i3,i4);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glFrontFace
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glFrontFace)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glGetAttribLocation
  (JNIEnv *e, jclass c, jint i1, jstring s2)
{
  const char *s2ptr = e->GetStringUTFChars(s2,NULL);
  jint ret = (*_glGetAttribLocation)(i1, s2ptr);
  e->ReleaseStringUTFChars(s2, s2ptr);
  return ret;
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glGetError
  (JNIEnv *e , jclass c)
{
  return (*_glGetError)();
}

JNIEXPORT jstring JNICALL Java_javaforce_gl_GL_glGetProgramInfoLog
  (JNIEnv *e, jclass c, jint i1)
{
  char * log = (char*)malloc(1024);
  (*_glGetProgramInfoLog)(i1, 1024, NULL, log);
  jstring str = e->NewStringUTF(log);
  free(log);
  return str;
}

JNIEXPORT jstring JNICALL Java_javaforce_gl_GL_glGetShaderInfoLog
  (JNIEnv *e, jclass c, jint i1)
{
  char * log = (char*)malloc(1024);
  (*_glGetShaderInfoLog)(i1, 1024, NULL, log);
  jstring str = e->NewStringUTF(log);
  free(log);
  return str;
}

JNIEXPORT jstring JNICALL Java_javaforce_gl_GL_glGetString
  (JNIEnv *e, jclass c, jint i1)
{
  const char * cstr = (*_glGetString)(i1);
  jstring str = e->NewStringUTF(cstr);
  return str;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGetIntegerv
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGetIntegerv)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenBuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenBuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenFramebuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenFramebuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenRenderbuffers
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenRenderbuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glGenTextures
  (JNIEnv *e, jclass c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenTextures)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glGetUniformLocation
  (JNIEnv *e, jclass c, jint i1, jstring s2)
{
  const char *s2ptr = e->GetStringUTFChars(s2,NULL);
  jint ret = (*_glGetUniformLocation)(i1, s2ptr);
  e->ReleaseStringUTFChars(s2, s2ptr);
  return ret;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glLinkProgram
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glLinkProgram)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glPixelStorei
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glPixelStorei)(i1,i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glReadPixels
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jintArray i7)
{
  int *i7ptr = (int*)e->GetPrimitiveArrayCritical(i7,NULL);
  (*_glReadPixels)(i1, i2, i3, i4, i5, i6, i7ptr);
  e->ReleasePrimitiveArrayCritical(i7, i7ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glRenderbufferStorage
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glRenderbufferStorage)(i1,i2,i3,i4);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glShaderSource
  (JNIEnv *e, jclass c, jint i1, jint i2, jobjectArray s3, jintArray i4)
{
  int *i4ptr = NULL;
  if (i4 != NULL) i4ptr = (int*)e->GetPrimitiveArrayCritical(i4,NULL);
  int s3size = e->GetArrayLength(s3);
  const char **s3ptr = (const char **)malloc(s3size * sizeof(void*));
  for(int a=0;a<s3size;a++) {
    jobject s3e = e->GetObjectArrayElement(s3, a);
    s3ptr[a] = e->GetStringUTFChars((jstring)s3e, NULL);
  }
  jint ret = (*_glShaderSource)(i1, i2, s3ptr, i4ptr);
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
  return (*_glStencilFunc)(i1,i2,i3);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glStencilMask
  (JNIEnv *e, jclass c, jint i1)
{
  return (*_glStencilMask)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_gl_GL_glStencilOp
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3)
{
  return (*_glStencilOp)(i1,i2,i3);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glTexImage2D
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jint i7, jint i8, jintArray i9)
{
  int *i9ptr = (int*)e->GetPrimitiveArrayCritical(i9,NULL);
  (*_glTexImage2D)(i1, i2, i3, i4, i5, i6, i7, i8, i9ptr);
  e->ReleasePrimitiveArrayCritical(i9, i9ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glTexSubImage2D
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jint i7, jint i8, jintArray i9)
{
  int *i9ptr = (int*)e->GetPrimitiveArrayCritical(i9,NULL);
  (*_glTexSubImage2D)(i1, i2, i3, i4, i5, i6, i7, i8, i9ptr);
  e->ReleasePrimitiveArrayCritical(i9, i9ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glTexParameteri
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3)
{
  (*_glTexParameteri)(i1,i2,i3);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUseProgram
  (JNIEnv *e, jclass c, jint i1)
{
  (*_glUseProgram)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniformMatrix4fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jfloatArray f4)
{
  float *f4ptr = (jfloat*)e->GetPrimitiveArrayCritical(f4,NULL);
  (*_glUniformMatrix4fv)(i1, i2, i3, f4ptr);
  e->ReleasePrimitiveArrayCritical(f4, f4ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform4fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glUniform4fv)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform3fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glUniform3fv)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform2fv
  (JNIEnv *e, jclass c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glUniform2fv)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform1f
  (JNIEnv *e, jclass c, jint i1, jfloat f2)
{
  (*_glUniform1f)(i1, f2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform4iv
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glUniform4iv)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform3iv
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glUniform3iv)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform2iv
  (JNIEnv *e, jclass c, jint i1, jint i2, jintArray i3)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glUniform2iv)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glUniform1i
  (JNIEnv *e, jclass c, jint i1, jint i2)
{
  (*_glUniform1i)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glVertexAttribPointer
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6)
{
  (*_glVertexAttribPointer)(i1, i2, i3, i4, i5, (void*)(jlong)i6);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glViewport
  (JNIEnv *e, jclass c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glViewport)(i1, i2, i3, i4);
}

static JNINativeMethod javaforce_gl_GL[] = {
  {"init", "()Z", (void *)&Java_javaforce_gl_GL_init},
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
