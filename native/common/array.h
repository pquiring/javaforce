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
