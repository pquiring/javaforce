#include <Foundation/Foundation.h>
#include <AppKit/AppKit.h>
#include <AVFoundation/AVFoundation.h>

#include <dlfcn.h>
#include <stdlib.h>

//#include <GL/gl.h>

#include <jni.h>

#include "javaforce_jni_JFNative.h"
#include "javaforce_jni_MacNative.h"
#include "javaforce_jni_GLJNI.h"
#include "javaforce_jni_CameraJNI.h"
#include "javaforce_jni_MediaJNI.h"
#include "javaforce_jni_UIJNI.h"
#include "javaforce_jni_CLJNI.h"

#include "../common/library.h"
#include "../common/
h"

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

//OpenGL

#include "../common/glfw.cpp"

#include "../common/gl.cpp"

void *gl;

jboolean glPlatformInit() {
  gl = dlopen("/System/Library/Frameworks/OpenGL.framework/Versions/A/Libraries/OpenGL.dylib", RTLD_LAZY | RTLD_GLOBAL);
  return gl != NULL;
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

#include "camera-jni.mm"

#include "../common/ffmpeg.cpp"

#include "../common/opencl-jni.cpp"
#include "../common/opencl-ffm.cpp"

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

static JNINativeMethod javaforce_media_Camera[] = {
  {"cameraInit", "()J", (void *)&Java_javaforce_jni_CameraJNI_cameraInit},
  {"cameraUninit", "(J)Z", (void *)&Java_javaforce_jni_CameraJNI_cameraUninit},
  {"cameraListDevices", "(J)[Ljava/lang/String;", (void *)&Java_javaforce_jni_CameraJNI_cameraListDevices},
  {"cameraListModes", "(JI)[Ljava/lang/String;", (void *)&Java_javaforce_jni_CameraJNI_cameraListModes},
  {"cameraStart", "(JIII)Z", (void *)&Java_javaforce_jni_CameraJNI_cameraStart},
  {"cameraStop", "(J)Z", (void *)&Java_javaforce_jni_CameraJNI_cameraStop},
  {"cameraGetFrame", "(J)[I", (void *)&Java_javaforce_jni_CameraJNI_cameraGetFrame},
  {"cameraGetWidth", "(J)I", (void *)&Java_javaforce_jni_CameraJNI_cameraGetWidth},
  {"cameraGetHeight", "(J)I", (void *)&Java_javaforce_jni_CameraJNI_cameraGetHeight},
};

extern "C" void camera_register(JNIEnv *env);

void camera_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/CameraJNI");
  registerNatives(env, cls, javaforce_media_Camera, sizeof(javaforce_media_Camera)/sizeof(JNINativeMethod));
}

#include "../common/register.cpp"
