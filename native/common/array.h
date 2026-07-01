/**

FFM Array (JNI Array)

Returned from FFM functions that return arrays.

*/

//FFMArray

#define jstringArray jobjectArray

struct FFMArray {
  void* (*pin)();
  void (*unpin)();
  jbyte* (*newByteArray)(int size);
  jshort* (*newShortArray)(int size);
  jint* (*newIntArray)(int size);
  jlong* (*newLongArray)(int size);
  jfloat* (*newFloatArray)(int size);
  jdouble* (*newDoubleArray)(int size);
  jstringArray (*newStringArray)(int size);
  void (*setString)(int idx, const char* str);
  jobjectArray (*newString2Array)();
} *ffm;

void set_upcall_FFMArray(FFMArray *api) {
  ffm = api;
}

extern "C" {
  JNIEXPORT void (*_set_upcall_FFMArray)(FFMArray*) = &set_upcall_FFMArray;
}

//JNIArray (emulate FFMArray)

#define JNIArray(upper,lower,string) \
struct JNIArray##upper { \
  JNIEnv *e; \
  jclass cls; \
  jmethodID mid_ctor; \
  jmethodID mid_getArray; \
  lower##Array getArray() { \
    return (lower##Array)e->CallStaticObjectMethod(cls, mid_getArray); \
  } \
  JNIArray##upper(JNIEnv *e) { \
    this->e = e; \
    cls = e->FindClass("javaforce/ffm/FFM"); \
    mid_getArray = e->GetStaticMethodID(cls, "getArray", "()Ljava/lang/Object;"); \
  } \
};

JNIArray(Byte,jbyte,"Byte")
JNIArray(Short,jshort,"Short")
JNIArray(Int,jint,"Int")
JNIArray(Float,jfloat,"Float")
JNIArray(Double,jdouble,"Double")

//special String instance

#define jstringArray jobjectArray

struct JNIArrayString {
  JNIEnv *e;
  jobject array;
  void* ptr;
  jclass cls;
  jmethodID mid_getArray;
  jobjectArray getArray() {
    return (jstringArray)e->CallStaticObjectMethod(cls, mid_getArray);
  }
  JNIArrayString(JNIEnv *e) {
    this->e = e;
    cls = e->FindClass("javaforce/ffm/FFMArray");
    mid_getArray = e->GetStaticMethodID(cls, "getArray", "()Ljava/lang/Object;");
  }
};
