//MediaOutput

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaOutput_ncreateFile
  (JNIEnv *e, jclass c, jstring file, jstring format)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  const char *cformat = e->GetStringUTFChars(format, NULL);
  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, cformat, NULL);
  e->ReleaseStringUTFChars(format, cformat);

  if (ctx->fmt_ctx == NULL) {
    deleteFFContext(e, c, ctx);
    return 0;
  }

  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;

  const char *cfile = e->GetStringUTFChars(file, NULL);
  (*_avio_open)(&ctx->io_ctx, cfile, AVIO_FLAG_READ_WRITE);
  e->ReleaseStringUTFChars(file, cfile);

  ctx->io_file = true;

  ctx->last_pts = -1;
  ctx->last_dts = -1;

  return (jlong)ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaOutput_ncreateIO
  (JNIEnv *e, jclass c, jobject mio, jstring format)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->cls_mio = e->GetObjectClass(ctx->mio);
  ctx->GetMediaIO();

  const char *cformat = e->GetStringUTFChars(format, NULL);
  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, cformat, NULL);
  e->ReleaseStringUTFChars(format, cformat);

  if (ctx->fmt_ctx == NULL) {
    deleteFFContext(e, c, ctx);
    return 0;
  }

  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;

  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 1, (void*)ctx, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  if (ctx->io_ctx == NULL) return JNI_FALSE;
  ctx->fmt_ctx->io_open = &io_open;
  ctx->fmt_ctx->io_close2 = &io_close2;
  ctx->fmt_ctx->opaque = ctx;

  ctx->last_pts = -1;
  ctx->last_dts = -1;

  return (jlong)ctx;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaOutput_naddVideoStream
  (JNIEnv *e, jclass c, jlong ctxptr, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  if (codec_id == -1) {
    codec_id = ctx->out_fmt->video_codec;
  }
  if (!encoder_add_stream(ctx, codec_id)) {
    printf("encoder_add_stream:video failed!\n");
    return -1;
  }

  if (!encoder_init_video(ctx)) {
    printf("encoder_init_video failed!\n");
    return -1;
  }

  return ctx->video_stream->index;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaOutput_naddAudioStream
  (JNIEnv *e, jclass c, jlong ctxptr, jint codec_id, jint bit_rate, jint chs, jint freq)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  if (codec_id == -1) {
    codec_id = ctx->out_fmt->audio_codec;
  }
  if (!encoder_add_stream(ctx, codec_id)) {
    printf("encoder_add_stream:audio failed!\n");
    return -1;
  }

  if (!encoder_init_audio(ctx)) {
    printf("encoder_init_audio failed!\n");
    return -1;
  }

  return ctx->audio_stream->index;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaOutput_nclose
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  encoder_stop(ctx);
  deleteFFContext(e,c,ctx);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaOutput_nwriteHeader
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  int ret = (*_avformat_write_header)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("avformat_write_header failed! %d\n", ret);
  }
  return ret >= 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaOutput_nwritePacket
  (JNIEnv *e, jclass c, jlong ctxptr, jint stream, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jbyte *cdata = (jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
  pkt->data = (uint8_t*)cdata;
  pkt->size = length;
  pkt->stream_index = stream;

  if (stream == 0) {
    ctx->last_pts++;
    ctx->last_dts++;
  }

  pkt->dts = ctx->last_dts;
  pkt->pts = ctx->last_pts;

  if (ctx->video_stream != NULL && ctx->video_stream->index == stream) {
    (*_av_packet_rescale_ts)(pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
  }
  else if (ctx->audio_stream != NULL && ctx->audio_stream->index == stream) {
    (*_av_packet_rescale_ts)(pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  }

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
  pkt->data = NULL;
  pkt->size = 0;
  (*_av_packet_free)(&pkt);

  e->ReleasePrimitiveArrayCritical(data, cdata, JNI_ABORT);

  return ret == 0;
}
