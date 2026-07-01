#include "jf_version.h"

const char* nclass;

typedef int (JNICALL *JNI_GetCreatedJavaVMs_t)(JavaVM**, int, int*);

jclass findClass(JNIEnv *env, const char *clsname) {
  nclass = clsname;
  jclass cls = env->FindClass(clsname);
  if (cls == NULL) {
    printf("Warning:Class not found:%s (native methods will not be registered)\n", clsname);
  }
  return cls;
}

void registerNatives(JNIEnv *env, jclass cls, JNINativeMethod *methods, jint count) {
  if (cls == NULL) return;
  int res = env->RegisterNatives(cls, methods, count);
  if (res != 0) {
    printf("Registering natives for %s failed : count=%d error=%d\n", nclass, count, res);
  }
}

#include "jfheap.cpp"

const char* get_javaforce_version() {
  return JF_ABI_VERSION;
}

extern "C" {
  JNIEXPORT const char* (*_get_javaforce_version)() = &get_javaforce_version;
}
