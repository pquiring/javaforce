//MediaFormat native methods

JNIEXPORT jint JNICALL Java_javaforce_media_MediaFormat_ngetVideoStream
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return ctx->video_stream->index;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaFormat_ngetAudioStream
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return ctx->audio_stream->index;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaFormat_ngetVideoCodecID
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return ctx->video_stream->codecpar->codec_id;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaFormat_ngetAudioCodecID
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return ctx->audio_stream->codecpar->codec_id;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaFormat_ngetVideoBitRate
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->bit_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaFormat_ngetAudioBitRate
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->bit_rate;
}
