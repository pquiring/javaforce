//MediaOutput

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaOutput_ncreateFile
  (JNIEnv *e, jclass c, jstring file, jstring format)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  const char *cformat = e->GetStringUTFChars(format, NULL);
  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, cformat, NULL);
  e->ReleaseStringUTFChars(format, cformat);

  if (ctx->fmt_ctx == NULL) {
    printf("MediaOutput.ncreateFile:format not found!");
    freeFFContext(e, c, ctx);
    return 0;
  }

  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;

  const char *cfile = e->GetStringUTFChars(file, NULL);
  (*_avio_open)(&ctx->io_ctx, cfile, AVIO_FLAG_READ_WRITE);
  e->ReleaseStringUTFChars(file, cfile);

  ctx->fmt_ctx->pb = ctx->io_ctx;

  ctx->io_file = true;

  ctx->last_pts = -1;
  ctx->last_dts = -1;

  printf("MediaOutput.ncreateFile() : ctx=%p fmt_ctx=%p out_fmt=%p\n", ctx, ctx->fmt_ctx, ctx->out_fmt);

  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  return (jlong)ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaOutput_ncreateIO
  (JNIEnv *e, jclass c, jobject mio, jstring format)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->cls_mio = e->GetObjectClass(ctx->mio);
  ctx->GetMediaIO();

  const char *cformat = e->GetStringUTFChars(format, NULL);
  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, cformat, NULL);
  e->ReleaseStringUTFChars(format, cformat);

  if (ctx->fmt_ctx == NULL) {
    freeFFContext(e, c, ctx);
    return 0;
  }

  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;

  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 1, (void*)ctx, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  if (ctx->io_ctx == NULL) {
    printf("MediaOutput:avio_alloc_context() failed\n");
    return JNI_FALSE;
  }

  ctx->fmt_ctx->pb = ctx->io_ctx;
  ctx->fmt_ctx->io_open = &io_open;
  ctx->fmt_ctx->io_close2 = &io_close2;
  ctx->fmt_ctx->opaque = ctx;

  ctx->last_pts = -1;
  ctx->last_dts = -1;

  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  printf("MediaOutput.ncreateIO() = %p\n", ctx);

  return (jlong)ctx;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaOutput_naddVideoStream
  (JNIEnv *e, jclass c, jlong ctxptr, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  if (codec_id <= 0) {
    codec_id = ctx->out_fmt->video_codec;
  }
  if (!encoder_add_stream(ctx, codec_id)) {
    printf("MediaOutput:encoder_add_stream(video) failed\n");
    return -1;
  }

  ctx->config_video_bit_rate = bit_rate;
  ctx->org_width = width;
  ctx->org_height = height;
  if (((width & 3) != 0) || ((height & 3) != 0)) {
    printf("Warning : Video resolution not / by 4 : Performance will be degraded!\n");
    ctx->scaleVideo = JNI_TRUE;
    //align up to / by 4 pixels
    width = (width + 3) & 0xfffffffc;
    height = (height + 3) & 0xfffffffc;
  } else {
    ctx->scaleVideo = JNI_FALSE;
  }
  ctx->width = width;
  ctx->height = height;
  ctx->fps = fps;
  ctx->config_gop_size = keyFrameInterval;

  if (!encoder_init_video(ctx)) {
    printf("MediaOutput:encoder_init_video() failed\n");
    return -1;
  }

  return ctx->video_stream->index;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaOutput_naddAudioStream
  (JNIEnv *e, jclass c, jlong ctxptr, jint codec_id, jint bit_rate, jint chs, jint freq)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  if (codec_id <= 0) {
    codec_id = ctx->out_fmt->audio_codec;
  }
  if (!encoder_add_stream(ctx, codec_id)) {
    printf("MediaOutput:encoder_add_stream(audio) failed\n");
    return -1;
  }

  ctx->config_audio_bit_rate = bit_rate;
  ctx->chs = chs;
  ctx->freq = freq;

  if (!encoder_init_audio(ctx)) {
    printf("MediaOutput:encoder_init_audio() failed\n");
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
  freeFFContext(e,c,ctx);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaOutput_nwriteHeader
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  printf("write header:fmt_ctx=%p\n", ctx->fmt_ctx);

  int ret = (*_avformat_write_header)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("MediaOutput:avformat_write_header failed : %d\n", ret);
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

  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = (uint8_t*)cdata;
  ctx->pkt->size = length;
  ctx->pkt->stream_index = stream;

  ctx->pkt->dts = ctx->last_dts;
  ctx->pkt->pts = ctx->last_pts;
  ctx->pkt->duration = ctx->last_duration;

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  e->ReleasePrimitiveArrayCritical(data, cdata, JNI_ABORT);

  return ret == 0;
}
