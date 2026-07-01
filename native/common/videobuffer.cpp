#ifdef _M_AMD64  //Microsoft VC++
  #ifndef __amd64__
    #define __amd64__
  #endif
#endif

#ifndef __amd64__
  #ifndef __aarch64__
    #define __amd64__
  #endif
#endif

#ifdef __amd64__
  #include "amd64.cpp"
#endif

#ifdef __aarch64__
  #include "arm64.cpp"
#endif

jfloat compareFrames(jint* img1, jint* img2, jint width, jint height)
{
  int size = width * height;
  if (size & 0x3 != 0) {
    printf("Error: VideoBuffer.compareFrames() frame size must be / by 4\n");
    return 100.0f;
  }
  int size_4 = size >> 2;

  jint *pc1 = img1;
  jint *pc2 = img2;

  int diff = simd_diff(pc1, pc2, size_4);

  float fdiff = diff;
  float fsize = size;
  float changed = (fdiff * 100.0f) / fsize;

  return changed;
}

extern "C" {
  //VideoBuffer
  JNIEXPORT float (*_compareFrames)(jint* frame1, jint* frame2, jint width, jint height) = &compareFrames;
}
