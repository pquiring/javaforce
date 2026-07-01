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
