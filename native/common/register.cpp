const char* nclass;

jclass findClass(JNIEnv *env, const char *clsname) {
  nclass = clsname;
  jclass cls = env->FindClass(clsname);
  if (cls == NULL) {
    printf("Error:Class not found:%s\n", clsname);
    exit(1);
  }
  return cls;
}

void registerNatives(JNIEnv *env, jclass cls, JNINativeMethod *methods, jint count) {
  int res = env->RegisterNatives(cls, methods, count);
  if (res != 0) {
    printf("Registering natives for %s count %d error %d\n", nclass, count, res);
  }
}

extern "C" void ni_register(JNIEnv *env);
extern "C" void gl_register(JNIEnv *env);
extern "C" void glfw_register(JNIEnv *env);
extern "C" void font_register(JNIEnv *env);
extern "C" void image_register(JNIEnv *env);
extern "C" void camera_register(JNIEnv *env);
extern "C" void ffmpeg_register(JNIEnv *env);
extern "C" void videobuffer_register(JNIEnv *env);
extern "C" void pcap_register(JNIEnv *env);
extern "C" void cl_register(JNIEnv *env);
extern "C" void gpio_register(JNIEnv *env);
extern "C" void i2c_register(JNIEnv *env);

void registerCommonNatives(JNIEnv *env) {
  jclass cls;

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
