/**

C Array

Returned from FFM functions that return arrays.

*/

#define ARRAY_TYPE_BYTE 1
#define ARRAY_TYPE_INT 2
#define ARRAY_TYPE_SHORT 3
#define ARRAY_TYPE_LONG 4
#define ARRAY_TYPE_STRING 100

//template<typename T, int N>
struct JFArray {
  int count;  //# of elements (N)
  short size;  //element size
  short type;  //ARRAY_TYPE_...
  union {
    //T elements[N];
    jint ints[0];
    jshort shorts[0];
    jbyte bytes[0];
    char* strs[0];
  };
  static JFArray* create(int element_count, short element_size, short element_type) {
    JFArray* arr = (JFArray*)malloc(8 + (element_count * element_size));
    arr->count = element_count;
    arr->size = element_size;
    arr->type = element_type;
    return arr;
  }

  //getters
  int getInt(int idx) {
    return ints[idx];
  }
  short getShort(int idx) {
    return shorts[idx];
  }
  char getByte(int idx) {
    return bytes[idx];
  }
  jbyte* getBufferByte() {
    return (jbyte*)&bytes;
  }
  jint* getBufferInt() {
    return (jint*)&bytes;
  }
  jshort* getBufferShort() {
    return (jshort*)&bytes;
  }

  //setters
  void setString(int idx, char* str) {
    strs[idx] = str;
  }
};

void jfArrayFree(JFArray* arr) {
  if (arr == NULL) {
    printf("Error:JFArrayFree:arr == NULL\n");
    return;
  }
  if (arr->type == ARRAY_TYPE_STRING) {
    //free all strings
    for(int i=0;i<arr->count;i++) {
      free(arr->strs[i]);
    }
  }
  free(arr);
}

extern "C" {
  JNIEXPORT void (*_jfArrayFree)(JFArray*) = &jfArrayFree;
}
