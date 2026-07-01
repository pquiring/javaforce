extern "C" {
  JNI_GetCreatedJavaVMs_t get_JNI_GetCreatedJavaVMs();
}

bool debug_jfheap = false;

JNIEXPORT jlong JNICALL Java_javaforce_ffm_JFHeap_pin
  (JNIEnv *e, jclass c, jobject array)
{
  return (jlong)e->GetPrimitiveArrayCritical((jarray)array, NULL);
}

JNIEXPORT void JNICALL Java_javaforce_ffm_JFHeap_unpin
  (JNIEnv *e, jclass c, jobject array, jlong ptr, jboolean commit)
{
  e->ReleasePrimitiveArrayCritical((jarray)array, (void*)ptr, commit ? 0 : JNI_ABORT);
}

JNIEXPORT jlong JNICALL Java_javaforce_ffm_JFHeap_ref
  (JNIEnv *e, jclass c, jobject obj)
{
  if (obj == NULL) return 0;
  return (jlong)e->NewGlobalRef(obj);
}

JNIEXPORT void JNICALL Java_javaforce_ffm_JFHeap_unref
  (JNIEnv *e, jclass c, jlong ref)
{
  if (ref == 0) return;
  e->DeleteGlobalRef((jobject)ref);
}

static JNINativeMethod javaforce_ffm_JFHeap[] = {
  {"pin", "(Ljava/lang/Object;)J", (void *)&Java_javaforce_ffm_JFHeap_pin},
  {"unpin", "(Ljava/lang/Object;JZ)V", (void *)&Java_javaforce_ffm_JFHeap_unpin},
  {"ref", "(Ljava/lang/Object;)J", (void *)&Java_javaforce_ffm_JFHeap_ref},
  {"unref", "(J)V", (void *)&Java_javaforce_ffm_JFHeap_unref},
};

//JavaVM : per process
JavaVM* javavm;

//JNIEnv : per thread
JNIEnv* get_jnienv() {
  JNIEnv *jnienv;
  javavm->GetEnv((void**)&jnienv, JNI_VERSION_24);
  return jnienv;
}

//FFM : register natives for JFHeap
jboolean setup_JFHeap() {
  int cnt = 1;
  JNI_GetCreatedJavaVMs_t getvms = get_JNI_GetCreatedJavaVMs();
  if (debug_jfheap) {
    printf("JNI_GetCreatedJavaVMs=%p\n", getvms);
  }
  if (getvms == NULL) {
    printf("get_JNI_GetCreatedJavaVMs failed\n");
    return JNI_FALSE;
  }
  (*getvms)(&javavm, cnt, &cnt);
  if (javavm == NULL) {
    printf("JNI_GetCreatedJavaVMs failed\n");
    return JNI_FALSE;
  }
  if (debug_jfheap) {
    printf("JavaVM=%p\n", javavm);
  }
  JNIEnv* jnienv = get_jnienv();
  if (jnienv == NULL) {
    printf("JavaVM::GetEnv failed\n");
    return JNI_FALSE;
  }
  if (debug_jfheap) {
    printf("JNIEnv=%p\n", jnienv);
  }

  jclass classLoaderClass = jnienv->FindClass("java/lang/ClassLoader");
  if (classLoaderClass == NULL) {
    printf("ClassLoader not found\n");
    return JNI_FALSE;
  }
  if (debug_jfheap) {
    printf("ClassLoader.class=%p\n", classLoaderClass);
  }

/*
  jmethodID getClassLoaderMethod = jnienv->GetStaticMethodID(classLoaderClass, "getPlatformClassLoader", "()Ljava/lang/ClassLoader;");
  jobject platformClassLoader = jnienv->CallStaticObjectMethod(classLoaderClass, getClassLoaderMethod);
*/

  jclass threadClass = jnienv->FindClass("java/lang/Thread");
  jmethodID currentThreadMid = jnienv->GetStaticMethodID(threadClass, "currentThread", "()Ljava/lang/Thread;");
  jobject currentThread = jnienv->CallStaticObjectMethod(threadClass, currentThreadMid);

  jmethodID getCtxClassLoaderMid = jnienv->GetMethodID(threadClass, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
  jobject contextClassLoader = jnienv->CallObjectMethod(currentThread, getCtxClassLoaderMid);

  jmethodID findClassMid = jnienv->GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

  //define javaforce/jni/JFHeap class

  jclass cls = NULL;
  jstring classNameStr = NULL;

  //JNIEnv->FindClass("javaforce...");  //uses platform class loader, need to use Thread class loader

  classNameStr = jnienv->NewStringUTF("javaforce.ffm.JFHeap");
  cls = (jclass)jnienv->CallObjectMethod(contextClassLoader, findClassMid, classNameStr);

  registerNatives(jnienv, cls, javaforce_ffm_JFHeap, sizeof(javaforce_ffm_JFHeap)/sizeof(JNINativeMethod));

  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jboolean (*_setup_JFHeap)() = &setup_JFHeap;
}
