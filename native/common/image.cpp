#define STB_IMAGE_IMPLEMENTATION
#define STBI_NO_BMP
#define STBI_NO_PSD
#define STBI_NO_TGA
#define STBI_NO_GIF
#define STBI_NO_GIF
#define STBI_NO_PIC
#define STBI_NO_PNM
#include "../stb/stb_image.h"

#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "../stb/stb_image_write.h"

JNIEXPORT jintArray JNICALL Java_javaforce_ui_Image_nloadPNG
  (JNIEnv *e, jobject o, jbyteArray data, jintArray size)
{
  int x, y, chs;
  jboolean isCopy;

  uint8 *data_ptr = (uint8*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  int data_len = e->GetArrayLength(data);

  stbi_uc* px_ptr = stbi_load_from_memory(data_ptr, data_len, &x, &y, &chs, 4);

  e->ReleasePrimitiveArrayCritical(data, data_ptr, JNI_ABORT);

  if (px_ptr == NULL) return NULL;

  int *size_ptr = (int*)e->GetPrimitiveArrayCritical(size, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  size_ptr[0] = x;
  size_ptr[1] = y;
  e->ReleasePrimitiveArrayCritical(size, size_ptr, JNI_COMMIT);

  jintArray out = e->NewIntArray(x*y);
  uint8 *out_ptr = (uint8*)e->GetPrimitiveArrayCritical(out, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  //convert RGBA to BGRA
  int xy = x * y;
  uint8* dst = out_ptr;
  uint8* src = px_ptr; 
  int off = 0;
  for(int i=0;i<xy;i++) {
    dst[off+2] = src[off+0];
    dst[off+1] = src[off+1];
    dst[off+0] = src[off+2];
    dst[off+3] = src[off+3];
    off += 4;
  }
  e->ReleasePrimitiveArrayCritical(out, out_ptr, JNI_COMMIT);

  stbi_image_free(px_ptr);

  return out;
}

struct write_ctx {
  uint8* buf;
  int siz;
};

static void write_func(void *context, void *data, int size) {
  write_ctx* ctx = (write_ctx*)context;

  if (ctx->buf == NULL) {
    ctx->buf = (uint8*)malloc(size);
    memcpy(ctx->buf, data, size);
    ctx->siz = size;
  } else {
    ctx->buf = (uint8*)realloc(ctx->buf, ctx->siz + size);
    memcpy(ctx->buf + ctx->siz, data, size);
    ctx->siz += size;
  }
}

JNIEXPORT jbyteArray JNICALL Java_javaforce_ui_Image_nsavePNG
  (JNIEnv *e, jobject o, jintArray px, jint x, jint y)
{
  write_ctx ctx;
  jboolean isCopy;

  ctx.buf = NULL;
  ctx.siz = 0;

  int px_len = e->GetArrayLength(px);
  if (px_len != x * y) return NULL;

  uint8 *px_ptr = (uint8*)e->GetPrimitiveArrayCritical(px, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  stbi_write_png_to_func(write_func, (void*)&ctx, x, y, 4, px_ptr, x*4);

  e->ReleasePrimitiveArrayCritical(px, px_ptr, JNI_ABORT);

  jbyteArray out = e->NewByteArray(ctx.siz);
  uint8 *out_ptr = (uint8*)e->GetPrimitiveArrayCritical(out, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  memcpy(out_ptr, ctx.buf, ctx.siz);
  e->ReleasePrimitiveArrayCritical(out, out_ptr, JNI_COMMIT);

  free(ctx.buf);

  return out;
}

JNIEXPORT jintArray JNICALL Java_javaforce_ui_Image_nloadJPG
  (JNIEnv *e, jobject o, jbyteArray data, jintArray size)
{
  int x, y, chs;
  jboolean isCopy;

  uint8 *data_ptr = (uint8*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  int data_len = e->GetArrayLength(data);

  stbi_uc* px_ptr = stbi_load_from_memory(data_ptr, data_len, &x, &y, &chs, 4);

  e->ReleasePrimitiveArrayCritical(data, data_ptr, JNI_ABORT);

  if (px_ptr == NULL) return NULL;

  int *size_ptr = (int*)e->GetPrimitiveArrayCritical(size, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  size_ptr[0] = x;
  size_ptr[1] = y;
  e->ReleasePrimitiveArrayCritical(size, size_ptr, JNI_COMMIT);

  jintArray out = e->NewIntArray(x*y);
  uint8 *out_ptr = (uint8*)e->GetPrimitiveArrayCritical(out, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  memcpy(out_ptr, px_ptr, x*y*4);
  e->ReleasePrimitiveArrayCritical(out, out_ptr, JNI_COMMIT);

  stbi_image_free(px_ptr);

  return out;
}

JNIEXPORT jbyteArray JNICALL Java_javaforce_ui_Image_nsaveJPG
  (JNIEnv *e, jobject o, jintArray px, jint x, jint y, jint quality)
{
  write_ctx ctx;
  jboolean isCopy;

  ctx.buf = NULL;
  ctx.siz = 0;

  int px_len = e->GetArrayLength(px);
  if (px_len != x * y) return NULL;

  uint8 *px_ptr = (uint8*)e->GetPrimitiveArrayCritical(px, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  stbi_write_jpg_to_func(write_func, (void*)&ctx, x, y, 4, px_ptr, quality);

  e->ReleasePrimitiveArrayCritical(px, px_ptr, JNI_ABORT);

  jbyteArray out = e->NewByteArray(ctx.siz);
  uint8 *out_ptr = (uint8*)e->GetPrimitiveArrayCritical(out, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
  memcpy(out_ptr, ctx.buf, ctx.siz);
  e->ReleasePrimitiveArrayCritical(out, out_ptr, JNI_COMMIT);

  free(ctx.buf);

  return out;
}
