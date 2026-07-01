extern "C" {
  JNI_GetCreatedJavaVMs_t get_JNI_GetCreatedJavaVMs();
}

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
  if (getvms == NULL) {
    printf("get_JNI_GetCreatedJavaVMs failed\n");
    return JNI_FALSE;
  }
  (*getvms)(&javavm, cnt, &cnt);
  if (javavm == NULL) {
    printf("JNI_GetCreatedJavaVMs failed\n");
    return JNI_FALSE;
  }
  JNIEnv* jnienv = get_jnienv();
  if (jnienv == NULL) {
    printf("JavaVM::GetEnv failed\n");
    return JNI_FALSE;
  }

  jclass classLoaderClass = jnienv->FindClass("java/lang/ClassLoader");
  if (classLoaderClass == NULL) {
    printf("ClassLoader not found\n");
    return JNI_FALSE;
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

  classNameStr = jnienv->NewStringUTF("javaforce.jni.JFHeap");
  cls = (jclass)jnienv->CallObjectMethod(contextClassLoader, findClassMid, classNameStr);

  registerNatives(jnienv, cls, javaforce_ffm_JFHeap, sizeof(javaforce_ffm_JFHeap)/sizeof(JNINativeMethod));

  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jboolean (*_setup_JFHeap)() = &setup_JFHeap;
}
