JFArray* uiLoadPNG(jbyte* data, jint data_size, jint* size)
{
  int x, y, chs;
  jboolean isCopy;

  stbi_uc* px_ptr = stbi_load_from_memory((uint8*)data, data_size, &x, &y, &chs, 4);

  if (px_ptr == NULL) return NULL;

  size[0] = x;
  size[1] = y;

  JFArray* out = JFArray::create(x*y, sizeof(int), ARRAY_TYPE_INT);
  //convert RGBA to BGRA
  int xy = x * y;
  uint8* dst = (uint8*)out->getBufferByte();
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

//HERE

JFArray* uiSavePNG(jint* px, jint x, jint y)
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

  JFArray* out = JFArray::create(ctx.siz, sizeof(jbyte), ARRAY_TYPE_BYTE);
  memcpy(out->getBufferByte(), ctx.buf, ctx.siz);

  free(ctx.buf);

  return out;
}

JFArray* uiLoadJPG(jbyte* data, jint data_size, jint* size)
{
  int x, y, chs;
  jboolean isCopy;

  stbi_uc* px_ptr = stbi_load_from_memory((uint8*)data, data_size, &x, &y, &chs, 4);

  if (px_ptr == NULL) return NULL;

  size[0] = x;
  size[1] = y;

  JFArray* out = JFArray::create(x*y, sizeof(int), ARRAY_TYPE_INT);
  memcpy(out->getBufferByte(), px_ptr, x*y*4);

  stbi_image_free(px_ptr);

  return out;
}

JFArray* uiSaveJPG(jint* px, jint x, jint y, jint quality)
{
  write_ctx ctx;
  jboolean isCopy;

  ctx.buf = NULL;
  ctx.siz = 0;

  stbi_write_jpg_to_func(write_func, (void*)&ctx, x, y, 4, px, quality);

  JFArray* out = JFArray::create(ctx.siz, sizeof(jbyte), ARRAY_TYPE_BYTE);
  memcpy(out->getBufferByte(), ctx.buf, ctx.siz);

  free(ctx.buf);

  return out;
}

extern "C" {
  JNIEXPORT JFArray* (*_uiLoadPNG)(jbyte*,jint,jint*) = &uiLoadPNG;
  JNIEXPORT JFArray* (*_uiSavePNG)(jint*,jint,jint) = &uiSavePNG;
  JNIEXPORT JFArray* (*_uiLoadJPG)(jbyte*,jint length,jint*) = &uiLoadJPG;
  JNIEXPORT JFArray* (*_uiSaveJPG)(jint*,jint,jint,jint) = &uiSaveJPG;
}
