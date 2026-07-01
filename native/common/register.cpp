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

JNIEXPORT jlong JNICALL Java_javaforce_jni_JFHeap_pin
  (JNIEnv *e, jclass c, jobject array)
{
  return (jlong)e->GetPrimitiveArrayCritical((jarray)array, NULL);
}

JNIEXPORT void JNICALL Java_javaforce_jni_JFHeap_unpin
  (JNIEnv *e, jclass c, jobject array, jlong ptr, jboolean commit)
{
  e->ReleasePrimitiveArrayCritical((jarray)array, (void*)ptr, commit ? 0 : JNI_ABORT);
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_JFHeap_ref
  (JNIEnv *e, jclass c, jobject obj)
{
  return (jlong)e->NewGlobalRef(obj);
}

JNIEXPORT void JNICALL Java_javaforce_jni_JFHeap_unref
  (JNIEnv *e, jclass c, jlong ref)
{
  e->DeleteGlobalRef((jobject)ref);
}

static JNINativeMethod javaforce_ffm_JFHeap[] = {
  {"pin", "(Ljava/lang/Object;)J", (void *)&Java_javaforce_jni_JFHeap_pin},
  {"unpin", "(Ljava/lang/Object;JZ)V", (void *)&Java_javaforce_jni_JFHeap_unpin},
  {"ref", "(Ljava/lang/Object;)J", (void *)&Java_javaforce_jni_JFHeap_ref},
  {"unref", "(J)V", (void *)&Java_javaforce_jni_JFHeap_unref},
};

#include "register-jfheap.cpp"
