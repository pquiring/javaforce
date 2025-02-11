jclass findClass(JNIEnv *env, const char *clsname);
void registerNatives(JNIEnv *env, jclass cls, JNINativeMethod *methods, jint count);
void registerCommonNatives(JNIEnv *env);

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
