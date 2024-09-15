static JNINativeMethod javaforce_media_Camera[] = {
  {"cameraInit", "()Z", (void *)&Java_javaforce_media_Camera_cameraInit},
  {"cameraUninit", "()Z", (void *)&Java_javaforce_media_Camera_cameraUninit},
  {"cameraListDevices", "()[Ljava/lang/String;", (void *)&Java_javaforce_media_Camera_cameraListDevices},
  {"cameraListModes", "(I)[Ljava/lang/String;", (void *)&Java_javaforce_media_Camera_cameraListModes},
  {"cameraStart", "(III)Z", (void *)&Java_javaforce_media_Camera_cameraStart},
  {"cameraStop", "()Z", (void *)&Java_javaforce_media_Camera_cameraStop},
  {"cameraGetFrame", "()[I", (void *)&Java_javaforce_media_Camera_cameraGetFrame},
  {"cameraGetWidth", "()I", (void *)&Java_javaforce_media_Camera_cameraGetWidth},
  {"cameraGetHeight", "()I", (void *)&Java_javaforce_media_Camera_cameraGetHeight},
};

#ifdef __RASPBERRY_PI__
static JNINativeMethod javaforce_pi_GPIO[] = {
  {"ninit", "(I)Z", (void *)&Java_javaforce_pi_GPIO_ninit},
  {"configOutput", "(I)Z", (void *)&Java_javaforce_pi_GPIO_configOutput},
  {"configInput", "(I)Z", (void *)&Java_javaforce_pi_GPIO_configInput},
  {"write", "(IZ)Z", (void *)&Java_javaforce_pi_GPIO_write},
  {"read", "(I)Z", (void *)&Java_javaforce_pi_GPIO_read},
};

static JNINativeMethod javaforce_pi_I2C[] = {
  {"init", "()Z", (void *)&Java_javaforce_pi_I2C_init},
  {"setSlave", "(I)Z", (void *)&Java_javaforce_pi_I2C_setSlave},
  {"write", "([B)Z", (void *)&Java_javaforce_pi_I2C_write},
  {"read", "([B)I", (void *)&Java_javaforce_pi_I2C_read},
};
#endif

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
extern "C" void ffmpeg_register(JNIEnv *env);
extern "C" void videobuffer_register(JNIEnv *env);
extern "C" void pcap_register(JNIEnv *env);
extern "C" void cl_register(JNIEnv *env);

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
  cls = findClass(env, "javaforce/media/Camera");
  registerNatives(env, cls, javaforce_media_Camera, sizeof(javaforce_media_Camera)/sizeof(JNINativeMethod));
#endif

  ffmpeg_register(env);

  videobuffer_register(env);

  pcap_register(env);

  cl_register(env);

#ifdef __RASPBERRY_PI__
  cls = findClass(env, "javaforce/pi/GPIO");
  registerNatives(env, cls, javaforce_pi_GPIO, sizeof(javaforce_pi_GPIO)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/pi/I2C");
  registerNatives(env, cls, javaforce_pi_I2C, sizeof(javaforce_pi_I2C)/sizeof(JNINativeMethod));
#endif
}
