//MediaFormat native methods

jint getVideoStream(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->video_stream->index;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getVideoStream
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return getVideoStream(ctx);;
}

jint getAudioStream(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->audio_stream->index;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getAudioStream
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return getAudioStream(ctx);
}

jint getVideoCodecID(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->video_stream->codecpar->codec_id;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getVideoCodecID
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return getVideoCodecID(ctx);
}

jint getAudioCodecID(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->audio_stream->codecpar->codec_id;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getAudioCodecID
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return getAudioCodecID(ctx);
}

jint getVideoBitRate(FFContext *ctx)
{
  if (ctx == NULL) return 0;

  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->bit_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getVideoBitRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  return getVideoBitRate(ctx);
}

jint getAudioBitRate(FFContext *ctx)
{
  if (ctx == NULL) return 0;

  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->bit_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getAudioBitRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  return getAudioBitRate(ctx);
}
