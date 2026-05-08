/**

FFM Array (JNI Array)

Returned from FFM functions that return arrays.

*/

//FFMArray

#define FFMArray(upper, lower) struct FFMArray##upper { lower* (*alloc)(int size); };

FFMArray(Byte,jbyte);
FFMArray(Short,jshort);
FFMArray(Int,jint);
FFMArray(Float,jfloat);

struct _FFMArrayString {
  void* (*alloc)(int size);
  void (*setString)(int idx, const char* str);
};

typedef _FFMArrayString* FFMArrayString;

//JNIArray (emulate FFMArray)

#define JNIArray(upper,lower,string) \
struct JNIArray##upper { \
  JNIEnv *e; \
  jobject array; \
  void* ptr; \
  jclass cls; \
  jmethodID mid_ctor; \
  jmethodID mid_getUpcall; \
  jmethodID mid_getArray; \
  FFMArray##upper toFFM() { \
    FFMArray##upper ffm; \
    ffm.alloc = (lower* (*)(int))ptr; \
    return ffm; \
  } \
  lower##Array getArray() { \
    return (lower##Array)e->CallObjectMethod(array, mid_getArray); \
  } \
  JNIArray##upper(JNIEnv *e) { \
    this->e = e; \
    cls = e->FindClass("javaforce/ffm/FFMArray"); \
    mid_getUpcall = e->GetMethodID(cls, "getUpcall", "(Ljava/lang/String;)J"); \
    mid_getArray = e->GetMethodID(cls, "getArray", "()Ljava/lang/Object;"); \
    mid_ctor = e->GetMethodID(cls, "<init>", "()V"); \
    array = e->NewObject(cls, mid_ctor); \
    ptr = (void*)e->CallLongMethod(array, mid_getUpcall, e->NewStringUTF(string)); \
  } \
};

JNIArray(Byte,jbyte,"Byte")
JNIArray(Short,jshort,"Short")
JNIArray(Int,jint,"Int")
JNIArray(Float,jfloat,"Float")

//special String instance

#define jstringArray jobjectArray

struct JNIArrayString {
  JNIEnv *e;
  jobject array;
  void* ptr;
  jclass cls;
  jmethodID mid_ctor;
  jmethodID mid_getUpcall;
  jmethodID mid_getArray;
  FFMArrayString toFFM() {
    FFMArrayString ffm = (FFMArrayString)ptr;
    return ffm;
  }
  jobjectArray getArray() {
    return (jstringArray)e->CallObjectMethod(array, mid_getArray);
  }
  JNIArrayString(JNIEnv *e) {
    this->e = e;
    cls = e->FindClass("javaforce/ffm/FFMArray");
    mid_getUpcall = e->GetMethodID(cls, "getUpcall", "(Ljava/lang/String;)J");
    mid_getArray = e->GetMethodID(cls, "getArray", "()Ljava/lang/Object;");
    mid_ctor = e->GetMethodID(cls, "<init>", "()V");
    array = e->NewObject(cls, mid_ctor);
    ptr = (void*)e->CallLongMethod(array, mid_getUpcall, e->NewStringUTF("String"));
  }
};
