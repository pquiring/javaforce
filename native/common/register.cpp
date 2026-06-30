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

JNIEXPORT jstring JNICALL Java_javaforce_jni_JFNative_getVersion
  (JNIEnv *e, jclass c)
{
  return e->NewStringUTF(JF_ABI_VERSION);
}

JNIEXPORT jobject JNICALL Java_javaforce_jni_JFNative_allocate
  (JNIEnv *e, jclass c, jint size)
{
  return e->NewDirectByteBuffer(malloc(size), size);
}

JNIEXPORT void JNICALL Java_javaforce_jni_JFNative_free
  (JNIEnv *e, jclass c, jobject bb)
{
  void* ptr = e->GetDirectBufferAddress(bb);
  free(ptr);
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

static JNINativeMethod javaforce_jni_JFNative[] = {
  {"getVersion", "()Ljava/lang/String;", (void *)&Java_javaforce_jni_JFNative_getVersion},
  {"allocate", "(I)Ljava/nio/ByteBuffer;", (void *)&Java_javaforce_jni_JFNative_allocate},
  {"free", "(Ljava/nio/ByteBuffer;)V", (void *)&Java_javaforce_jni_JFNative_free},
  {"getWindowHandle", "(Ljava/awt/Window;)J", (void *)&Java_javaforce_jni_JFNative_getWindowHandle},
};

void jfnative_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/JFNative");
  registerNatives(env, cls, javaforce_jni_JFNative, sizeof(javaforce_jni_JFNative)/sizeof(JNINativeMethod));
}

static JNINativeMethod javaforce_jni_JFHeap[] = {
  {"pin", "(Ljava/lang/Object;)J", (void *)&Java_javaforce_jni_JFHeap_pin},
  {"unpin", "(Ljava/lang/Object;JZ)V", (void *)&Java_javaforce_jni_JFHeap_unpin},
  {"ref", "(Ljava/lang/Object;)J", (void *)&Java_javaforce_jni_JFHeap_ref},
  {"unref", "(J)V", (void *)&Java_javaforce_jni_JFHeap_unref},
};

void jfheap_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/JFHeap");
  registerNatives(env, cls, javaforce_jni_JFHeap, sizeof(javaforce_jni_JFHeap)/sizeof(JNINativeMethod));
}

void registerCommonNatives(JNIEnv *env) {
  jfnative_register(env);

  jfheap_register(env);

  gl_register(env);

  glfw_register(env);

  font_register(env);

  image_register(env);

#ifndef __FreeBSD__
  camera_register(env);
#endif

  ffmpeg_register(env);

  videobuffer_register(env);

  pcap_register(env);

  cl_register(env);

#ifdef __RASPBERRY_PI__
  gpio_register(env);

  i2c_register(env);
#endif
}

#include "register-jfheap.cpp"
