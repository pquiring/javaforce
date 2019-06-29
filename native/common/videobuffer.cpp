JNIEXPORT jboolean JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height, jint threshold)
{
  if (img1 == NULL) return JNI_TRUE;
  if (img2 == NULL) return JNI_TRUE;
  jint *px1 = e->GetIntArrayElements(img1, NULL);
  jint *px2 = e->GetIntArrayElements(img2, NULL);

  int s1 = e->GetArrayLength(img1);
  int s2 = e->GetArrayLength(img2);

  int size = width * height;

  jint *pc1 = px1;
  jint *pc2 = px2;

  int off = 0;
  int diff = 0;
  for(int x=0;x<width;x++) {
    for(int y=0;y<height;y++) {
      jint p1 = *(pc1++);
      jint p2 = *(pc2++);
      p1 &= 0xf0f0f0;
      p2 &= 0xf0f0f0;
      if (p1 != p2) diff++;
    }
  }

  int changed = (diff * 100) / size;

  printf("changed=%d threshold=%d\n", changed, threshold);

  e->ReleaseIntArrayElements(img1, px1, 0);
  e->ReleaseIntArrayElements(img2, px2, 0);

  return changed > threshold;
}
