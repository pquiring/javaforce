#include <Foundation/Foundation.h>
#include <AppKit/AppKit.h>
#include <AVFoundation/AVFoundation.h>

#include <dlfcn.h>
#include <stdlib.h>

#include <GL/gl.h>

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "../common/string.h"
#include "../common/array.h"
#include "../common/library.h"

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

//OpenGL

#include "../common/ui.cpp"

#include "../common/gl.cpp"

void *gl;

JNIEXPORT jboolean JNICALL GLinit(const char* libgl_so)
{
  gl = dlopen("/System/Library/Frameworks/OpenGL.framework/Versions/A/Libraries/OpenGL.dylib", RTLD_LAZY | RTLD_GLOBAL);
  GL_get_functions();
  return TRUE;
}

jboolean glGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)dlsym(gl, name);
  if (func != NULL) {
    *funcPtr = func;
    return JNI_TRUE;
  } else {
    printf("OpenGL:Error:Can not find function:%s\n", name);
    return JNI_FALSE;
  }
}

#include "camera.mm"

#include "../common/ffmpeg.cpp"

#include "../common/opencl.cpp"

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

#include "../common/register.cpp"

JNI_GetCreatedJavaVMs_t get_JNI_GetCreatedJavaVMs() {
  void* lib = dlopen("libjvm.so", RTLD_NOW | RTLD_GLOBAL);
  if (lib == NULL) {
    printf("dlopen('libjvm.so') failed\n");
    return NULL;
  }
  return (JNI_GetCreatedJavaVMs_t)dlsym(lib, "JNI_GetCreatedJavaVMs");
}
