//OpenGL functions

#include "register.h"

jboolean glPlatformInit();
jboolean glGetFunction(void **funcPtr, const char *name);  //platform impl

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL GLinit();

JNIEXPORT void (*_glActiveTexture)(int);
JNIEXPORT void (*_glAlphaFunc)(int,int);
JNIEXPORT void (*_glAttachShader)(int,int);
JNIEXPORT void (*_glBindBuffer)(int,int);
JNIEXPORT void (*_glBindFramebuffer)(int,int);
JNIEXPORT void (*_glBindRenderbuffer)(int,int);
JNIEXPORT void (*_glBindTexture)(int,int);
JNIEXPORT void (*_glBlendFunc)(int,int);
JNIEXPORT void (*_glBufferData)(int,void*,void*,int);
JNIEXPORT void (*_glClear)(int);
JNIEXPORT void (*_glClearColor)(float,float,float,float);
JNIEXPORT void (*_glClearStencil)(int);
JNIEXPORT void (*_glColorMask)(jboolean,jboolean,jboolean,jboolean);
JNIEXPORT void (*_glCompileShader)(int);
JNIEXPORT int (*_glCreateProgram)();
JNIEXPORT int (*_glCreateShader)(int);
JNIEXPORT void (*_glCullFace)(int);
JNIEXPORT void (*_glDeleteBuffers)(int,int *);
JNIEXPORT void (*_glDeleteFramebuffers)(int,int *);
JNIEXPORT void (*_glDeleteRenderbuffers)(int,int *);
JNIEXPORT void (*_glDeleteTextures)(int,int *);
JNIEXPORT void (*_glDrawElements)(int,int,int,void*);
JNIEXPORT void (*_glDepthFunc)(int);
JNIEXPORT void (*_glDisable)(int);
JNIEXPORT void (*_glDisableVertexAttribArray)(int);
JNIEXPORT void (*_glDepthMask)();
JNIEXPORT void (*_glEnable)();
JNIEXPORT void (*_glEnableVertexAttribArray)();
JNIEXPORT void (*_glFlush)();
JNIEXPORT void (*_glFramebufferTexture2D)(int,int,int,int,int);
JNIEXPORT void (*_glFramebufferRenderbuffer)(int,int,int,int);
JNIEXPORT void (*_glFrontFace)(int);
JNIEXPORT int (*_glGetAttribLocation)(int,const char *);
JNIEXPORT int (*_glGetError)();
JNIEXPORT const char* (*_glGetProgramInfoLog)(int,int,int*,char*);
JNIEXPORT const char* (*_glGetShaderInfoLog)(int,int,int*,char*);
JNIEXPORT const char* (*_glGetString)(int);
JNIEXPORT void (*_glGetIntegerv)(int,int *);
JNIEXPORT void (*_glGenBuffers)(int,int *);
JNIEXPORT void (*_glGenFramebuffers)(int,int *);
JNIEXPORT void (*_glGenRenderbuffers)(int,int *);
JNIEXPORT void (*_glGenTextures)(int,int *);
JNIEXPORT int (*_glGetUniformLocation)(int,const char *);
JNIEXPORT void (*_glLinkProgram)(int);
JNIEXPORT void (*_glPixelStorei)(int,int);
JNIEXPORT void (*_glReadPixels)(int,int,int,int,int,int,int *);
JNIEXPORT void (*_glRenderbufferStorage)(int,int,int,int);
JNIEXPORT int (*_glShaderSource)(int,int,const char**,int *);
JNIEXPORT int (*_glStencilFunc)(int,int,int);
JNIEXPORT int (*_glStencilMask)(int);
JNIEXPORT int (*_glStencilOp)(int,int,int);
JNIEXPORT void (*_glTexImage2D)(int,int,int,int,int,int,int,int,int *);
JNIEXPORT void (*_glTexSubImage2D)(int,int,int,int,int,int,int,int,int *);
JNIEXPORT void (*_glTexParameteri)(int,int,int);
JNIEXPORT void (*_glUseProgram)(int);
JNIEXPORT void (*_glUniformMatrix4fv)(int,int,int,float *);
JNIEXPORT void (*_glUniform4fv)(int,int,float *);
JNIEXPORT void (*_glUniform3fv)(int,int,float *);
JNIEXPORT void (*_glUniform2fv)(int,int,float *);
JNIEXPORT void (*_glUniform1f)(int, float);
JNIEXPORT void (*_glUniform4iv)(int,int,int *);
JNIEXPORT void (*_glUniform3iv)(int,int,int *);
JNIEXPORT void (*_glUniform2iv)(int,int,int *);
JNIEXPORT void (*_glUniform1i)(int,int);
JNIEXPORT void (*_glVertexAttribPointer)(int, int, int, int, int, void*);
JNIEXPORT void (*_glViewport)(int, int, int, int);

#ifdef __cplusplus
}
#endif

JNIEXPORT jboolean JNICALL GLinit()
{
  if (!glPlatformInit()) return JNI_FALSE;
  glGetFunction((void**)&_glActiveTexture,"glActiveTexture");
  glGetFunction((void**)&_glAlphaFunc,"glAlphaFunc");
  glGetFunction((void**)&_glAttachShader,"glAttachShader");
  glGetFunction((void**)&_glBindBuffer,"glBindBuffer");
  glGetFunction((void**)&_glBindFramebuffer,"glBindFramebuffer");
  glGetFunction((void**)&_glBindRenderbuffer,"glBindRenderbuffer");
  glGetFunction((void**)&_glBindTexture,"glBindTexture");
  glGetFunction((void**)&_glBlendFunc,"glBlendFunc");
  glGetFunction((void**)&_glBufferData,"glBufferData");
  glGetFunction((void**)&_glClear,"glClear");
  glGetFunction((void**)&_glClearColor,"glClearColor");
  glGetFunction((void**)&_glClearStencil,"glClearStencil");
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_GLJNI_init
  (JNIEnv *e, jobject c)
{
  return GLinit();
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glActiveTexture
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glActiveTexture)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glAlphaFunc
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glAlphaFunc)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glAttachShader
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glAttachShader)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBindBuffer
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glBindBuffer)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBindFramebuffer
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glBindFramebuffer)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBindRenderbuffer
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glBindRenderbuffer)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBindTexture
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glBindTexture)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBlendFunc
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glBlendFunc)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBufferData__II_3FI
  (JNIEnv *e, jobject c, jint i1, jint i2, jfloatArray f3, jint i4)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)f3ptr, i4);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBufferData__II_3SI
  (JNIEnv *e, jobject c, jint i1, jint i2, jshortArray s3, jint i4)
{
  jshort *s3ptr = (jshort*)e->GetPrimitiveArrayCritical(s3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)s3ptr, i4);
  e->ReleasePrimitiveArrayCritical(s3, s3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBufferData__II_3II
  (JNIEnv *e, jobject c, jint i1, jint i2, jintArray i3, jint i4)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)i3ptr, i4);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glBufferData__II_3BI
  (JNIEnv *e, jobject c, jint i1, jint i2, jbyteArray b3, jint i4)
{
  jbyte *b3ptr = (jbyte*)e->GetPrimitiveArrayCritical(b3,NULL);
  (*_glBufferData)(i1, (void*)(jlong)i2, (void*)b3ptr, i4);
  e->ReleasePrimitiveArrayCritical(b3, b3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glClear
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glClear)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glClearColor
  (JNIEnv *e, jobject c, jfloat f1, jfloat f2, jfloat f3, jfloat f4)
{
  (*_glClearColor)(f1,f2,f3,f4);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glClearStencil
  (JNIEnv *e, jobject c, jint s1)
{
  (*_glClearStencil)(s1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glColorMask
  (JNIEnv *e, jobject c, jboolean b1, jboolean b2, jboolean b3, jboolean b4)
{
  (*_glColorMask)(b1,b2,b3,b4);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glCompileShader
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glCompileShader)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glCreateProgram
  (JNIEnv *e, jobject c)
{
  return (*_glCreateProgram)();
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glCreateShader
  (JNIEnv *e, jobject c, jint i1)
{
  return (*_glCreateShader)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glCullFace
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glCullFace)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDeleteBuffers
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteBuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDeleteFramebuffers
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteFramebuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDeleteRenderbuffers
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteRenderbuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDeleteTextures
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glDeleteTextures)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDrawElements
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glDrawElements)(i1,i2,i3,(void*)(jlong)i4);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDepthFunc
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glDepthFunc)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDisable
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glDisable)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDisableVertexAttribArray
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glDisableVertexAttribArray)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glDepthMask
  (JNIEnv *, jobject c, jboolean b1)
{
  (*(void (*)(jboolean))_glDepthMask)(b1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glEnable
  (JNIEnv *e, jobject c, jint i1)
{
  (*(void (*)(int))_glEnable)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glEnableVertexAttribArray
  (JNIEnv *e, jobject c, jint i1)
{
  (*(void (*)(int))_glEnableVertexAttribArray)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glFlush
  (JNIEnv *e, jobject c)
{
  (*_glFlush)();
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glFramebufferTexture2D
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4, jint i5)
{
  (*_glFramebufferTexture2D)(i1,i2,i3,i4,i5);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glFramebufferRenderbuffer
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glFramebufferRenderbuffer)(i1,i2,i3,i4);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glFrontFace
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glFrontFace)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glGetAttribLocation
  (JNIEnv *e, jobject c, jint i1, jstring s2)
{
  const char *s2ptr = e->GetStringUTFChars(s2,NULL);
  jint ret = (*_glGetAttribLocation)(i1, s2ptr);
  e->ReleaseStringUTFChars(s2, s2ptr);
  return ret;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glGetError
  (JNIEnv *e , jobject c)
{
  return (*_glGetError)();
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_GLJNI_glGetProgramInfoLog
  (JNIEnv *e, jobject c, jint i1)
{
  char * log = (char*)malloc(1024);
  (*_glGetProgramInfoLog)(i1, 1024, NULL, log);
  jstring str = e->NewStringUTF(log);
  free(log);
  return str;
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_GLJNI_glGetShaderInfoLog
  (JNIEnv *e, jobject c, jint i1)
{
  char * log = (char*)malloc(1024);
  (*_glGetShaderInfoLog)(i1, 1024, NULL, log);
  jstring str = e->NewStringUTF(log);
  free(log);
  return str;
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_GLJNI_glGetString
  (JNIEnv *e, jobject c, jint i1)
{
  const char * cstr = (*_glGetString)(i1);
  jstring str = e->NewStringUTF(cstr);
  return str;
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glGetIntegerv
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGetIntegerv)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glGenBuffers
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenBuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glGenFramebuffers
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenFramebuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glGenRenderbuffers
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenRenderbuffers)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glGenTextures
  (JNIEnv *e, jobject c, jint i1, jintArray i2)
{
  int *i2ptr = (int*)e->GetPrimitiveArrayCritical(i2,NULL);
  (*_glGenTextures)(i1, i2ptr);
  e->ReleasePrimitiveArrayCritical(i2, i2ptr, 0);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glGetUniformLocation
  (JNIEnv *e, jobject c, jint i1, jstring s2)
{
  const char *s2ptr = e->GetStringUTFChars(s2,NULL);
  jint ret = (*_glGetUniformLocation)(i1, s2ptr);
  e->ReleaseStringUTFChars(s2, s2ptr);
  return ret;
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glLinkProgram
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glLinkProgram)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glPixelStorei
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glPixelStorei)(i1,i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glReadPixels
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jintArray i7)
{
  int *i7ptr = (int*)e->GetPrimitiveArrayCritical(i7,NULL);
  (*_glReadPixels)(i1, i2, i3, i4, i5, i6, i7ptr);
  e->ReleasePrimitiveArrayCritical(i7, i7ptr, 0);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glRenderbufferStorage
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glRenderbufferStorage)(i1,i2,i3,i4);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glShaderSource
  (JNIEnv *e, jobject c, jint i1, jint i2, jobjectArray s3, jintArray i4)
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

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glStencilFunc
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3)
{
  return (*_glStencilFunc)(i1,i2,i3);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glStencilMask
  (JNIEnv *e, jobject c, jint i1)
{
  return (*_glStencilMask)(i1);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_GLJNI_glStencilOp
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3)
{
  return (*_glStencilOp)(i1,i2,i3);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glTexImage2D
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jint i7, jint i8, jintArray i9)
{
  int *i9ptr = (int*)e->GetPrimitiveArrayCritical(i9,NULL);
  (*_glTexImage2D)(i1, i2, i3, i4, i5, i6, i7, i8, i9ptr);
  e->ReleasePrimitiveArrayCritical(i9, i9ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glTexSubImage2D
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6, jint i7, jint i8, jintArray i9)
{
  int *i9ptr = (int*)e->GetPrimitiveArrayCritical(i9,NULL);
  (*_glTexSubImage2D)(i1, i2, i3, i4, i5, i6, i7, i8, i9ptr);
  e->ReleasePrimitiveArrayCritical(i9, i9ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glTexParameteri
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3)
{
  (*_glTexParameteri)(i1,i2,i3);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUseProgram
  (JNIEnv *e, jobject c, jint i1)
{
  (*_glUseProgram)(i1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniformMatrix4fv
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jfloatArray f4)
{
  float *f4ptr = (jfloat*)e->GetPrimitiveArrayCritical(f4,NULL);
  (*_glUniformMatrix4fv)(i1, i2, i3, f4ptr);
  e->ReleasePrimitiveArrayCritical(f4, f4ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform4fv
  (JNIEnv *e, jobject c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glUniform4fv)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform3fv
  (JNIEnv *e, jobject c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glUniform3fv)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform2fv
  (JNIEnv *e, jobject c, jint i1, jint i2, jfloatArray f3)
{
  float *f3ptr = (jfloat*)e->GetPrimitiveArrayCritical(f3,NULL);
  (*_glUniform2fv)(i1, i2, f3ptr);
  e->ReleasePrimitiveArrayCritical(f3, f3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform1f
  (JNIEnv *e, jobject c, jint i1, jfloat f2)
{
  (*_glUniform1f)(i1, f2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform4iv
  (JNIEnv *e, jobject c, jint i1, jint i2, jintArray i3)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glUniform4iv)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform3iv
  (JNIEnv *e, jobject c, jint i1, jint i2, jintArray i3)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glUniform3iv)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform2iv
  (JNIEnv *e, jobject c, jint i1, jint i2, jintArray i3)
{
  int *i3ptr = (int*)e->GetPrimitiveArrayCritical(i3,NULL);
  (*_glUniform2iv)(i1, i2, i3ptr);
  e->ReleasePrimitiveArrayCritical(i3, i3ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glUniform1i
  (JNIEnv *e, jobject c, jint i1, jint i2)
{
  (*_glUniform1i)(i1, i2);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glVertexAttribPointer
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4, jint i5, jint i6)
{
  (*_glVertexAttribPointer)(i1, i2, i3, i4, i5, (void*)(jlong)i6);
}

JNIEXPORT void JNICALL Java_javaforce_jni_GLJNI_glViewport
  (JNIEnv *e, jobject c, jint i1, jint i2, jint i3, jint i4)
{
  (*_glViewport)(i1, i2, i3, i4);
}

static JNINativeMethod javaforce_jni_GLJNI[] = {
  {"init", "()Z", (void *)&Java_javaforce_jni_GLJNI_init},
  {"glActiveTexture", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glActiveTexture},
  {"glAlphaFunc", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glAlphaFunc},
  {"glAttachShader", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glAttachShader},
  {"glBindBuffer", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glBindBuffer},
  {"glBindFramebuffer", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glBindFramebuffer},
  {"glBindRenderbuffer", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glBindRenderbuffer},
  {"glBindTexture", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glBindTexture},
  {"glBlendFunc", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glBlendFunc},
  {"glBufferData", "(II[FI)V", (void *)&Java_javaforce_jni_GLJNI_glBufferData__II_3FI},
  {"glBufferData", "(II[SI)V", (void *)&Java_javaforce_jni_GLJNI_glBufferData__II_3SI},
  {"glBufferData", "(II[II)V", (void *)&Java_javaforce_jni_GLJNI_glBufferData__II_3II},
  {"glBufferData", "(II[BI)V", (void *)&Java_javaforce_jni_GLJNI_glBufferData__II_3BI},
  {"glClear", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glClear},
  {"glClearColor", "(FFFF)V", (void *)&Java_javaforce_jni_GLJNI_glClearColor},
  {"glColorMask", "(ZZZZ)V", (void *)&Java_javaforce_jni_GLJNI_glColorMask},
  {"glCompileShader", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glCompileShader},
  {"glCreateProgram", "()I", (void *)&Java_javaforce_jni_GLJNI_glCreateProgram},
  {"glCreateShader", "(I)I", (void *)&Java_javaforce_jni_GLJNI_glCreateShader},
  {"glCullFace", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glCullFace},
  {"glDeleteBuffers", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glDeleteBuffers},
  {"glDeleteFramebuffers", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glDeleteFramebuffers},
  {"glDeleteRenderbuffers", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glDeleteRenderbuffers},
  {"glDeleteTextures", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glDeleteTextures},
  {"glDrawElements", "(IIII)V", (void *)&Java_javaforce_jni_GLJNI_glDrawElements},
  {"glDepthFunc", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glDepthFunc},
  {"glDisable", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glDisable},
  {"glDisableVertexAttribArray", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glDisableVertexAttribArray},
  {"glDepthMask", "(Z)V", (void *)&Java_javaforce_jni_GLJNI_glDepthMask},
  {"glEnable", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glEnable},
  {"glEnableVertexAttribArray", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glEnableVertexAttribArray},
  {"glFlush", "()V", (void *)&Java_javaforce_jni_GLJNI_glFlush},
  {"glFramebufferTexture2D", "(IIIII)V", (void *)&Java_javaforce_jni_GLJNI_glFramebufferTexture2D},
  {"glFramebufferRenderbuffer", "(IIII)V", (void *)&Java_javaforce_jni_GLJNI_glFramebufferRenderbuffer},
  {"glFrontFace", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glFrontFace},
  {"glGetAttribLocation", "(ILjava/lang/String;)I", (void *)&Java_javaforce_jni_GLJNI_glGetAttribLocation},
  {"glGetError", "()I", (void *)&Java_javaforce_jni_GLJNI_glGetError},
  {"glGetProgramInfoLog", "(I)Ljava/lang/String;", (void *)&Java_javaforce_jni_GLJNI_glGetProgramInfoLog},
  {"glGetShaderInfoLog", "(I)Ljava/lang/String;", (void *)&Java_javaforce_jni_GLJNI_glGetShaderInfoLog},
  {"glGetString", "(I)Ljava/lang/String;", (void *)&Java_javaforce_jni_GLJNI_glGetString},
  {"glGetIntegerv", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glGetIntegerv},
  {"glGenBuffers", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glGenBuffers},
  {"glGenFramebuffers", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glGenFramebuffers},
  {"glGenRenderbuffers", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glGenRenderbuffers},
  {"glGenTextures", "(I[I)V", (void *)&Java_javaforce_jni_GLJNI_glGenTextures},
  {"glGetUniformLocation", "(ILjava/lang/String;)I", (void *)&Java_javaforce_jni_GLJNI_glGetUniformLocation},
  {"glLinkProgram", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glLinkProgram},
  {"glPixelStorei", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glPixelStorei},
  {"glReadPixels", "(IIIIII[I)V", (void *)&Java_javaforce_jni_GLJNI_glReadPixels},
  {"glRenderbufferStorage", "(IIII)V", (void *)&Java_javaforce_jni_GLJNI_glRenderbufferStorage},
  {"glShaderSource", "(II[Ljava/lang/String;[I)I", (void *)&Java_javaforce_jni_GLJNI_glShaderSource},
  {"glStencilFunc", "(III)I", (void *)&Java_javaforce_jni_GLJNI_glStencilFunc},
  {"glStencilMask", "(I)I", (void *)&Java_javaforce_jni_GLJNI_glStencilMask},
  {"glStencilOp", "(III)I", (void *)&Java_javaforce_jni_GLJNI_glStencilOp},
  {"glTexImage2D", "(IIIIIIII[I)V", (void *)&Java_javaforce_jni_GLJNI_glTexImage2D},
  {"glTexSubImage2D", "(IIIIIIII[I)V", (void *)&Java_javaforce_jni_GLJNI_glTexSubImage2D},
  {"glTexParameteri", "(III)V", (void *)&Java_javaforce_jni_GLJNI_glTexParameteri},
  {"glUseProgram", "(I)V", (void *)&Java_javaforce_jni_GLJNI_glUseProgram},
  {"glUniformMatrix4fv", "(III[F)V", (void *)&Java_javaforce_jni_GLJNI_glUniformMatrix4fv},
  {"glUniform4fv", "(II[F)V", (void *)&Java_javaforce_jni_GLJNI_glUniform4fv},
  {"glUniform3fv", "(II[F)V", (void *)&Java_javaforce_jni_GLJNI_glUniform3fv},
  {"glUniform2fv", "(II[F)V", (void *)&Java_javaforce_jni_GLJNI_glUniform2fv},
  {"glUniform1f", "(IF)V", (void *)&Java_javaforce_jni_GLJNI_glUniform1f},
  {"glUniform4iv", "(II[I)V", (void *)&Java_javaforce_jni_GLJNI_glUniform4iv},
  {"glUniform3iv", "(II[I)V", (void *)&Java_javaforce_jni_GLJNI_glUniform3iv},
  {"glUniform2iv", "(II[I)V", (void *)&Java_javaforce_jni_GLJNI_glUniform2iv},
  {"glUniform1i", "(II)V", (void *)&Java_javaforce_jni_GLJNI_glUniform1i},
  {"glVertexAttribPointer", "(IIIIII)V", (void *)&Java_javaforce_jni_GLJNI_glVertexAttribPointer},
  {"glViewport", "(IIII)V", (void *)&Java_javaforce_jni_GLJNI_glViewport},
};

extern "C" void gl_register(JNIEnv *env);

void gl_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/GLJNI");
  registerNatives(env, cls, javaforce_jni_GLJNI, sizeof(javaforce_jni_GLJNI)/sizeof(JNINativeMethod));
}
