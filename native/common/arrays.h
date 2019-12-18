
#ifdef JFDK
  #define GET_BYTE_ARRAY GetPrimitiveArrayCritical
  #define GET_SHORT_ARRAY GetPrimitiveArrayCritical
  #define GET_INT_ARRAY GetPrimitiveArrayCritical
  #define GET_FLOAT_ARRAY GetPrimitiveArrayCritical

  #define RELEASE_BYTE_ARRAY ReleasePrimitiveArrayCritical
  #define RELEASE_SHORT_ARRAY ReleasePrimitiveArrayCritical
  #define RELEASE_INT_ARRAY ReleasePrimitiveArrayCritical
  #define RELEASE_FLOAT_ARRAY ReleasePrimitiveArrayCritical
#else
  #define GET_BYTE_ARRAY GetByteArrayElements
  #define GET_SHORT_ARRAY GetShortArrayElements
  #define GET_INT_ARRAY GetIntArrayElements
  #define GET_FLOAT_ARRAY GetFloatArrayElements

  #define RELEASE_BYTE_ARRAY ReleaseByteArrayElements
  #define RELEASE_SHORT_ARRAY ReleaseShortArrayElements
  #define RELEASE_INT_ARRAY ReleaseIntArrayElements
  #define RELEASE_FLOAT_ARRAY ReleaseFloatArrayElements
#endif
