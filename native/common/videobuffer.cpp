#ifdef _M_AMD64  //Microsoft VC++
  #ifndef __amd64__
    #define __amd64__
  #endif
#endif

#ifdef __amd64__
  #include "amd64.cpp"
#endif

#ifdef __aarch64__
  #include "arm64.cpp"
#endif

#include "register.h"

JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height)
{
  if (img1 == NULL) return 100.0f;
  if (img2 == NULL) return 100.0f;
  jboolean isCopy1, isCopy2;
  jint *px1 = (jint*)e->GetPrimitiveArrayCritical(img1, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jint *px2 = (jint*)e->GetPrimitiveArrayCritical(img2, &isCopy2);
  if (!shownCopyWarning && isCopy2 == JNI_TRUE) copyWarning();

  int s1 = e->GetArrayLength(img1);
  int s2 = e->GetArrayLength(img2);

  int size = width * height;
  if (s1 != size) {
    printf("Error: VideoBuffer.compareFrames() img1 is wrong size\n");
    return 100.0f;
  }
  if (s2 != size) {
    printf("Error: VideoBuffer.compareFrames() img2 is wrong size\n");
    return 100.0f;
  }
  if (size & 0x3 != 0) {
    printf("Error: VideoBuffer.compareFrames() frame size must be / by 4\n");
    return 100.0f;
  }
  int size_4 = size >> 2;

  jint *pc1 = px1;
  jint *pc2 = px2;

  int diff = simd_diff(pc1, pc2, size_4);

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->ReleasePrimitiveArrayCritical(img1, px1, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(img2, px2, JNI_ABORT);

  return changed;
}

static JNINativeMethod javaforce_media_VideoBuffer[] = {
  {"compareFrames", "([I[III)F", (void *)&Java_javaforce_media_VideoBuffer_compareFrames},
};

extern "C" void videobuffer_register(JNIEnv *env);

void videobuffer_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/media/VideoBuffer");
  registerNatives(env, cls, javaforce_media_VideoBuffer, sizeof(javaforce_media_VideoBuffer)/sizeof(JNINativeMethod));
}
