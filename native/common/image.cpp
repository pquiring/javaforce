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

#include "register.h"

jint* uiLoadPNG(jbyte* data, jint data_size, jint* size)
{
  int x, y, chs;
  jboolean isCopy;

  stbi_uc* px_ptr = stbi_load_from_memory((uint8*)data, data_size, &x, &y, &chs, 4);

  if (px_ptr == NULL) return NULL;

  size[0] = x;
  size[1] = y;

  jint* out = ffm->newIntArray(x*y);
  //convert RGBA to BGRA
  int xy = x * y;
  uint8* dst = (uint8*)out;
  uint8* src = px_ptr;
  int off = 0;
  for(int i=0;i<xy;i++) {
    dst[off+2] = src[off+0];
    dst[off+1] = src[off+1];
    dst[off+0] = src[off+2];
    dst[off+3] = src[off+3];
    off += 4;
  }

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

jbyte* uiSavePNG(jint* px, jint x, jint y)
{
  write_ctx ctx;
  int xy = x * y;

  ctx.buf = NULL;
  ctx.siz = 0;

  uint8* rgb_ptr = (uint8*)malloc(xy*4);
  //convert RGBA to BGRA
  uint8* dst = rgb_ptr;
  uint8* src = (uint8*)px;
  int off = 0;
  for(int i=0;i<xy;i++) {
    dst[off+2] = src[off+0];
    dst[off+1] = src[off+1];
    dst[off+0] = src[off+2];
    dst[off+3] = src[off+3];
    off += 4;
  }

  stbi_write_png_to_func(write_func, (void*)&ctx, x, y, 4, rgb_ptr, x*4);

  free(rgb_ptr);

  jbyte* out = ffm->newByteArray(ctx.siz);
  memcpy(out, ctx.buf, ctx.siz);

  free(ctx.buf);

  return out;
}

jint* uiLoadJPG(jbyte* data, jint data_size, jint* size)
{
  int x, y, chs;
  jboolean isCopy;

  stbi_uc* px_ptr = stbi_load_from_memory((uint8*)data, data_size, &x, &y, &chs, 4);

  if (px_ptr == NULL) return NULL;

  size[0] = x;
  size[1] = y;

  jint* out = ffm->newIntArray(x*y);
  memcpy(out, px_ptr, x*y*4);

  stbi_image_free(px_ptr);

  return out;
}

jbyte* uiSaveJPG(jint* px, jint x, jint y, jint quality)
{
  write_ctx ctx;
  jboolean isCopy;

  ctx.buf = NULL;
  ctx.siz = 0;

  stbi_write_jpg_to_func(write_func, (void*)&ctx, x, y, 4, px, quality);

  jbyte* out = ffm->newByteArray(ctx.siz);
  memcpy(out, ctx.buf, ctx.siz);

  free(ctx.buf);

  return out;
}

extern "C" {
  JNIEXPORT jint* (*_uiLoadPNG)(jbyte*,jint,jint*) = &uiLoadPNG;
  JNIEXPORT jbyte* (*_uiSavePNG)(jint*,jint,jint) = &uiSavePNG;
  JNIEXPORT jint* (*_uiLoadJPG)(jbyte*,jint length,jint*) = &uiLoadJPG;
  JNIEXPORT jbyte* (*_uiSaveJPG)(jint*,jint,jint,jint) = &uiSaveJPG;
}
