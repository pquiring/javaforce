JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height, jint mask)
{
  if (img1 == NULL) return 100.0f;
  if (img2 == NULL) return 100.0f;
  jboolean isCopy1, isCopy2;
  jint *px1 = (jint*)e->GET_INT_ARRAY(img1, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jint *px2 = (jint*)e->GET_INT_ARRAY(img2, &isCopy2);
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

  e->RELEASE_INT_ARRAY(img1, px1, JNI_ABORT);
  e->RELEASE_INT_ARRAY(img2, px2, JNI_ABORT);

  return changed;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames16
  (JNIEnv *e, jclass c, jshortArray img1, jshortArray img2, jint width, jint height)
{
  if (img1 == NULL) return 100.0f;
  if (img2 == NULL) return 100.0f;
  jboolean isCopy1, isCopy2;
  jshort *px1 = (jshort*)e->GET_SHORT_ARRAY(img1, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jshort *px2 = (jshort*)e->GET_SHORT_ARRAY(img2, &isCopy2);
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
  jshort p1, p2;
  for(int i=0;i<size;i++) {
    p1 = *(pc1++);
    p2 = *(pc2++);
    if (p1 != p2) diff++;
  }

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->RELEASE_SHORT_ARRAY(img1, px1, JNI_ABORT);
  e->RELEASE_SHORT_ARRAY(img2, px2, JNI_ABORT);

  return changed;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_VideoBuffer_convertImage15
  (JNIEnv *e, jclass c, jshortArray sa, jintArray ia, jint width, jint height)
{
  if (sa == NULL) return JNI_FALSE;
  if (ia == NULL) return JNI_FALSE;
  jboolean isCopy1, isCopy2;
  jshort *px1 = (jshort*)e->GET_SHORT_ARRAY(sa, &isCopy1);
  if (!shownCopyWarning && isCopy1 == JNI_TRUE) copyWarning();
  jint *px2 = (jint*)e->GET_INT_ARRAY(ia, &isCopy2);
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

  e->RELEASE_SHORT_ARRAY(sa, px1, JNI_ABORT);
  e->RELEASE_INT_ARRAY(ia, px2, JNI_COMMIT);

  return JNI_TRUE;
}
