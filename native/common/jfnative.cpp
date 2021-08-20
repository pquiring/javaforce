//javaforce.jni.JFNative native functions

JNIEXPORT void JNICALL Java_javaforce_jni_JFNative_test (JNIEnv *e, jclass c) {}

static bool g_isGraal = false;
static jclass PinnedObjectClass;
static jmethodID PinnedObject_create;
static jmethodID PinnedObject_addressOfArrayElement;

JNIEXPORT void JNICALL Java_javaforce_jni_JFNative_init (JNIEnv *e, jclass c) {
  g_isGraal = true;
  //org.graalvm.nativeimage.PinnedObject;
  PinnedObjectClass = e->FindClass("org/graalvm/nativeimage/PinnedObject");
  if (PinnedObjectClass == NULL) {
    g_isGraal = false;
    printf("Error:Unable to find org.graalvm.nativeimage.PinnedObject\n");
    return;
  }
		//static PinnedObject org.graalvm.nativeimage.PinnedObject.create(Object);
  PinnedObject_create = e->GetMethodID(PinnedObjectClass, "create" , "(Ljava/lang/Object;)Lorg/graalvm/nativeimage/PinnedObject;");
  printf("create=%p\n", PinnedObject_create);
		//<T extends org.graalvm.word.PointerBase> org.graalvm.nativeimage.PinnedObject.addressOfArrayElement(int index);
  PinnedObject_addressOfArrayElement = e->GetMethodID(PinnedObjectClass, "addressOfArrayElement", "(I)Lorg.graalvm.word.PointerBase;");
  printf("addressOfArrayElement=%p\n", PinnedObject_addressOfArrayElement);
}

//JNI Pointer methods

JNIEXPORT jlong JNICALL Java_javaforce_jni_JFNative_getPointer (JNIEnv *e, jclass c, jobject array) {
  jboolean isCopy;
  void *ptr = (void*)e->GetPrimitiveArrayCritical((jarray)array, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
//  printf("ptr=%p\n", ptr);
  return (jlong)ptr;  
}

JNIEXPORT void JNICALL Java_javaforce_jni_JFNative_freePointer (JNIEnv *e, jclass c, jobject array, jlong ptr) {
  e->ReleasePrimitiveArrayCritical((jarray)array, (void*)ptr, JNI_ABORT);
}

//Graal Pointer methods

JNIEXPORT jobject JNICALL Java_javaforce_jni_JFNative_createPinnedObject (JNIEnv *e, jclass c, jobject array) {
  return e->CallStaticObjectMethod(PinnedObjectClass, PinnedObject_create, array);
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_JFNative_getPinnedObjectPointer (JNIEnv *e, jclass c, jobject array) {
  return e->CallLongMethod(array, PinnedObject_addressOfArrayElement, 0);
}
