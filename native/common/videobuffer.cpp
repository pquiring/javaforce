#include <emmintrin.h>

union MD128 {
  __m128i md;
  int i32[4];
};


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
  if (size & 0x3 != 0) {
    printf("Error: VideoBuffer.compareFrames() frame size must be / by 4\n");
    return 100.0f;
  }
  int size_4 = size >> 2;

  jint *pc1 = px1;
  jint *pc2 = px2;

  int diff = 0;
  MD128 p1, p2, add, mask;
  p1.md = _mm_setzero_si128();
  p2.md = _mm_setzero_si128();
  add.md = _mm_set_epi32(0x00080808,0x00080808,0x00080808,0x00080808);
  mask.md = _mm_set_epi32(0x00f0f0f0,0x00f0f0f0,0x00f0f0f0,0x00f0f0f0);
  for(int i=0;i<size_4;i++) {
    p1.i32[0] = *(pc1++);
    p1.i32[1] = *(pc1++);
    p1.i32[2] = *(pc1++);
    p1.i32[3] = *(pc1++);
    p1.md = _mm_adds_epu8(p1.md, add.md);
    p1.md = _mm_and_si128(p1.md, mask.md);
    p2.i32[0] = *(pc2++);
    p2.i32[1] = *(pc2++);
    p2.i32[2] = *(pc2++);
    p2.i32[3] = *(pc2++);
    p2.md = _mm_adds_epu8(p2.md, add.md);
    p2.md = _mm_and_si128(p2.md, mask.md);
    if ( p1.i32[0] != p2.i32[0] ) diff++;
    if ( p1.i32[1] != p2.i32[1] ) diff++;
    if ( p1.i32[2] != p2.i32[2] ) diff++;
    if ( p1.i32[3] != p2.i32[3] ) diff++;
  }

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->ReleasePrimitiveArrayCritical(img1, px1, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(img2, px2, JNI_ABORT);

  return changed;
}
