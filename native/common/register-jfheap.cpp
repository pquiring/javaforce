extern "C" {
  JNI_GetCreatedJavaVMs_t get_JNI_GetCreatedJavaVMs();
}

//FFM : register natives for JFHeap
jboolean setup_JFHeap(jbyte* jfheap_class, jint jfheap_size) {
  JavaVM* jvm;
  int cnt = 1;
  JNI_GetCreatedJavaVMs_t getvms = get_JNI_GetCreatedJavaVMs();
  if (getvms == NULL) {
    printf("get_JNI_GetCreatedJavaVMs failed\n");
    return JNI_FALSE;
  }
  (*getvms)(&jvm, cnt, &cnt);
  if (jvm == NULL) {
    printf("JNI_GetCreatedJavaVMs failed\n");
    return JNI_FALSE;
  }
  JNIEnv *e;
  jvm->GetEnv((void**)&e, JNI_VERSION_24);
  if (e == NULL) {
    printf("JavaVM::GetEnv failed\n");
    return JNI_FALSE;
  }

  jclass classLoaderClass = e->FindClass("java/lang/ClassLoader");
  if (classLoaderClass == NULL) {
    printf("ClassLoader not found\n");
    return JNI_FALSE;
  }

  jmethodID getClassLoaderMethod = e->GetStaticMethodID(classLoaderClass, "getPlatformClassLoader", "()Ljava/lang/ClassLoader;");

  jobject platformClassLoader = e->CallStaticObjectMethod(classLoaderClass, getClassLoaderMethod);

  jclass threadClass = e->FindClass("java/lang/Thread");
  jmethodID currentThreadMid = e->GetStaticMethodID(threadClass, "currentThread", "()Ljava/lang/Thread;");
  jobject currentThread = e->CallStaticObjectMethod(threadClass, currentThreadMid);

  jmethodID getCtxClassLoaderMid = e->GetMethodID(threadClass, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
  jobject contextClassLoader = e->CallObjectMethod(currentThread, getCtxClassLoaderMid);

  jmethodID findClassMid = e->GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

  //define javaforce/jni/JFHeap class

  jclass cls = NULL;

  //e->FindClass("javaforce/jni/JFHeap");  //uses native class loader, need to use Thread class loader

  jstring classNameStr = e->NewStringUTF("javaforce.jni.JFHeap");
  cls = (jclass)e->CallObjectMethod(contextClassLoader, findClassMid, classNameStr);

  if (cls == NULL) {
    e->ExceptionClear();
    cls = e->DefineClass("javaforce/jni/JFHeap", contextClassLoader, jfheap_class, jfheap_size);
  }

  //jfheap_register(e);
  registerNatives(e, cls, javaforce_jni_JFHeap, sizeof(javaforce_jni_JFHeap)/sizeof(JNINativeMethod));

  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jboolean (*_setup_JFHeap)(jbyte*,jint) = &setup_JFHeap;
}
