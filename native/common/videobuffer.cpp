JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height, jint mask)
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
  jint p1, p2;
  for(int i=0;i<size;i++) {
    p1 = *(pc1++);
    p2 = *(pc2++);
    p1 &= mask;
    p2 &= mask;
    if (p1 != p2) diff++;
  }

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->ReleasePrimitiveArrayCritical(img1, px1, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(img2, px2, JNI_ABORT);

  return changed;
}
