jclass findClass(JNIEnv *env, const char *clsname);
void registerNatives(JNIEnv *env, jclass cls, JNINativeMethod *methods, jint count);
