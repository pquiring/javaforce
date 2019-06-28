JNIEXPORT jboolean JNICALL Java_javaforce_media_VideoBuffer_compareFrames
  (JNIEnv *e, jclass c, jintArray img1, jintArray img2, jint width, jint height, jint threshold)
{
  jint *px1 = e->GetIntArrayElements(img1, NULL);
  jint *px2 = e->GetIntArrayElements(img2, NULL);

  jint *pc1 = px1;
  jint *pc2 = px2;

  int off = 0;
  int diff = 0;
  for(int x=0;x<width;x++) {
    for(int y=0;y<width;y++) {
      jint p1 = *(pc1++);
      jint p2 = *(pc2++);
      p1 &= 0xf0f0f0;
      p2 &= 0xf0f0f0;
      if (p1 != p2) diff++;
    }
  }

  int size = width * height;
  int changed = (diff * 100) / size;

  e->ReleaseIntArrayElements(img1, px1, 0);
  e->ReleaseIntArrayElements(img2, px2, 0);

  return changed > threshold;
}
