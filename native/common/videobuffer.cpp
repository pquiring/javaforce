JNIEXPORT jfloat JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height, jint mask)
{
  if (img1 == NULL) return 100.0f;
  if (img2 == NULL) return 100.0f;
  jint *px1 = e->GetIntArrayElements(img1, NULL);
  jint *px2 = e->GetIntArrayElements(img2, NULL);

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
  for(int x=0;x<width;x++) {
    for(int y=0;y<height;y++) {
      jint p1 = *(pc1++);
      jint p2 = *(pc2++);
      p1 &= mask;
      p2 &= mask;
      if (p1 != p2) diff++;
    }
  }

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  e->ReleaseIntArrayElements(img1, px1, 0);
  e->ReleaseIntArrayElements(img2, px2, 0);

  return changed;
}
