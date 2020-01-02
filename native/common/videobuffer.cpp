#include <emmintrin.h>

JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height)
{
  if (img1 == NULL) return 100.0f;
  if (img2 == NULL) return 100.0f;
  jboolean isCopy1, isCopy2;
  jint *px1 = (jint*)e->GetPrimitiveArrayCritical(img1, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jint *px2 = (jint*)e->GetPrimitiveArrayCritical(img2, &isCopy2);
  if (!shownCopyWarning && isCopy2 == JNI_TRUE) copyWarning();

  int s1 = e->GetArrayLength(img1);
  int s2 = e->GetArrayLength(img2);

  int size = width * height;
  if (s1 != size) {
    printf("Error: VideoBuffer.compareFrames() img1 is wrong size\n");
    return 100.0f;
  }
  if (s2 != size) {
    printf("Error: VideoBuffer.compareFrames() img2 is wrong size\n");
    return 100.0f;
  }

  jint *pc1 = px1;
  jint *pc2 = px2;

  int diff = 0;
  __m128i p1, p2;
  p1.m128i_i64[0] = 0;
  p1.m128i_i64[1] = 0;
  p2.m128i_i64[0] = 0;
  p2.m128i_i64[1] = 0;
  __m128i add;
  add.m128i_i64[0] = 0;
  add.m128i_i64[1] = 0;
  add.m128i_i32[0] = 0x00080808;
  for(int i=0;i<size;i++) {
    p1.m128i_i32[0] = *(pc1++);
    p1 = _mm_adds_epu8(p1, add);
    p2.m128i_i32[0] = *(pc2++);
    p2 = _mm_adds_epu8(p2, add);
    if ((p1.m128i_i32[0] & 0x00f0f0f0) != (p2.m128i_i32[0] & 0x00f0f0f0)) diff++;
  }

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->ReleasePrimitiveArrayCritical(img1, px1, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(img2, px2, JNI_ABORT);

  return changed;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames16
  (JNIEnv *e, jclass c, jshortArray img1, jshortArray img2, jint width, jint height)
{
  if (img1 == NULL) return 100.0f;
  if (img2 == NULL) return 100.0f;
  jboolean isCopy1, isCopy2;
  jshort *px1 = (jshort*)e->GetPrimitiveArrayCritical(img1, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jshort *px2 = (jshort*)e->GetPrimitiveArrayCritical(img2, &isCopy2);
  if (!shownCopyWarning && isCopy2 == JNI_TRUE) copyWarning();

  int s1 = e->GetArrayLength(img1);
  int s2 = e->GetArrayLength(img2);

  int size = width * height;
  if (s1 != size) {
    printf("Error: VideoBuffer.compareFrames() img1 is wrong size\n");
    return 100.0f;
  }
  if (s2 != size) {
    printf("Error: VideoBuffer.compareFrames() img2 is wrong size\n");
    return 100.0f;
  }

  jshort *pc1 = px1;
  jshort *pc2 = px2;

  short diff = 0;
  __m128i p1, p2;
  p1.m128i_i64[0] = 0;
  p1.m128i_i64[1] = 0;
  p2.m128i_i64[0] = 0;
  p2.m128i_i64[1] = 0;
  __m128i add;
  add.m128i_i64[0] = 0;
  add.m128i_i64[1] = 0;
  add.m128i_i16[0] = 0b10000100001;
  for(int i=0;i<size;i++) {
    p1.m128i_i16[0] = *(pc1++);
    p1 = _mm_adds_epu8(p1, add);
    p2.m128i_i16[0] = *(pc2++);
    p2 = _mm_adds_epu8(p2, add);
    if ((p1.m128i_i16[0] & 0b111001110011100) != (p2.m128i_i16[0] & 0b111001110011100)) diff++;
  }

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->ReleasePrimitiveArrayCritical(img1, px1, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(img2, px2, JNI_ABORT);

  return changed;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_VideoBuffer_convertImage16
  (JNIEnv *e, jclass c, jshortArray sa, jintArray ia, jint width, jint height)
{
  if (sa == NULL) return JNI_FALSE;
  if (ia == NULL) return JNI_FALSE;
  jboolean isCopy1, isCopy2;
  jshort *px1 = (jshort*)e->GetPrimitiveArrayCritical(sa, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jint *px2 = (jint*)e->GetPrimitiveArrayCritical(ia, &isCopy2);
  if (!shownCopyWarning && isCopy2 == JNI_TRUE) copyWarning();

  int s1 = e->GetArrayLength(sa);
  int s2 = e->GetArrayLength(ia);

  int size = width * height;
  if (s1 != size) {
    printf("Error: VideoBuffer.convertImage15() img1 is wrong size\n");
    return JNI_FALSE;
  }
  if (s2 != size) {
    printf("Error: VideoBuffer.convertImage15() img2 is wrong size\n");
    return JNI_FALSE;
  }

  jshort *pc1 = px1;
  jint *pc2 = px2;

  unsigned int p15;
  unsigned int p24;
  for(int i=0;i<size;i++) {
    p15 = *(pc1++);
    p24 = 0xff000000;  //opaque
    p24 += (p15 & 0x7c00) << 9;
    p24 += (p15 & 0x3e0) << 6;
    p24 += (p15 & 0x1f) << 3;
    *(pc2++) = p24;
  }

  e->ReleasePrimitiveArrayCritical(sa, px1, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(ia, px2, JNI_COMMIT);

  return JNI_TRUE;
}
