const char* nclass;

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
    printf("Registering natives for %s count %d error %d\n", nclass, count, res);
  }
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

static JNINativeMethod javaforce_jni_JFNative[] = {
  {"allocate", "(I)Ljava/nio/ByteBuffer;", (void *)&Java_javaforce_jni_JFNative_allocate},
  {"free", "(Ljava/nio/ByteBuffer;)V", (void *)&Java_javaforce_jni_JFNative_free},
};

void registerCommonNatives(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/JFNative");
  registerNatives(env, cls, javaforce_jni_JFNative, sizeof(javaforce_jni_JFNative)/sizeof(JNINativeMethod));

#ifndef __FreeBSD__
  ni_register(env);
#endif

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
