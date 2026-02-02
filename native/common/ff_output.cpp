//MediaOutput

jboolean outputCreateFile_ctx(FFContext *ctx, const char* file, const char* format)
{
  if (ctx == NULL) return JNI_FALSE;

  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, format, NULL);

  if (ctx->fmt_ctx == NULL) {
    printf("MediaOutput.ncreateFile:format not found!");
    return JNI_FALSE;
  }

  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;

  (*_avio_open)(&ctx->io_ctx, file, AVIO_FLAG_READ_WRITE);

  ctx->fmt_ctx->pb = ctx->io_ctx;

  ctx->io_file = true;

  ctx->last_pts = -1;
  ctx->last_dts = -1;

  printf("MediaOutput.ncreateFile() : ctx=%p fmt_ctx=%p out_fmt=%p\n", ctx, ctx->fmt_ctx, ctx->out_fmt);

  ctx->pkt = AVPacket_New();

  return JNI_TRUE;
}

FFContext* outputCreateFile(const char* file, const char* format)
{
  FFContext *ctx = newFFContext(NULL, NULL);
  if (ctx == NULL) return 0;

  if (!outputCreateFile_ctx(ctx, file, format)) {
    freeFFContext(NULL, NULL, ctx);
    ctx = NULL;
  }

  return ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_outputCreateFile
  (JNIEnv *e, jobject c, jstring file, jstring format)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return 0;

  const char *cfile = e->GetStringUTFChars(file, NULL);
  const char *cformat = e->GetStringUTFChars(format, NULL);
  if (!outputCreateFile_ctx(ctx, cfile, cformat)) {
    freeFFContext(e, c, ctx);
    ctx = NULL;
  }
  e->ReleaseStringUTFChars(file, cfile);
  e->ReleaseStringUTFChars(format, cformat);

  return (jlong)ctx;
}

jboolean outputCreateIO_ctx(FFContext *ctx, const char* format)
{
  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, format, NULL);

  if (ctx->fmt_ctx == NULL) {
    return JNI_FALSE;
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
  printf("MediaOutput.ncreateIO() = %p\n", ctx);

  return JNI_TRUE;
}

FFContext* outputCreateIO(MediaIO* mio, const char* format)
{
  FFContext *ctx = newFFContext(NULL,NULL);
  if (ctx == NULL) return NULL;

  memcpy(&ctx->ffm_mio, mio, sizeof(MediaIO));

  if (!outputCreateIO_ctx(ctx, format)) {
    freeFFContext(NULL,NULL,ctx);
    ctx = NULL;
  }

  return ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_outputCreateIO
  (JNIEnv *e, jobject c, jobject mio, jstring format)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return 0;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->GetMediaIO();

  const char *cformat = e->GetStringUTFChars(format, NULL);

  if (!outputCreateIO_ctx(ctx, cformat)) {
    freeFFContext(e,c,ctx);
    ctx = NULL;
  }

  e->ReleaseStringUTFChars(format, cformat);

  return (jlong)ctx;
}

jint addVideoStream(FFContext *ctx, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval)
{
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

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_addVideoStream
  (JNIEnv *e, jobject c, jlong ctxptr, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return addVideoStream(ctx, codec_id, bit_rate, width, height, fps, keyFrameInterval);
}

jint addAudioStream(FFContext* ctx, jint codec_id, jint bit_rate, jint chs, jint freq)
{
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

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_addAudioStream
  (JNIEnv *e, jobject c, jlong ctxptr, jint codec_id, jint bit_rate, jint chs, jint freq)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;

  return addAudioStream(ctx, codec_id, bit_rate, chs, freq);
}

jboolean outputClose_ctx(JNIEnv *e, jobject c, FFContext* ctx)
{
  if (ctx == NULL) return JNI_FALSE;

  //flush encoders
  if (ctx->audio_stream != NULL) {
    encoder_flush(ctx, ctx->audio_codec_ctx, ctx->audio_stream, JNI_TRUE);
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->video_stream != NULL) {
    encoder_flush(ctx, ctx->video_codec_ctx, ctx->video_stream, JNI_TRUE);
  }
  if (ff_debug_trace) printf("encoder_stop:%p\n", ctx->fmt_ctx);
  int ret = (*_av_write_trailer)(ctx->fmt_ctx);
  if (ret < 0) {
    printf("MediaEncoder:av_write_trailer() failed : %d\n", ret);
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->io_ctx != NULL) {
    if (ctx->io_file) {
      (*_avio_close)(ctx->io_ctx);
    } else {
      (*_avio_flush)(ctx->io_ctx);
      (*_av_free)(ctx->io_ctx->buffer);
      (*_av_free)(ctx->io_ctx);
    }
    ctx->io_ctx = NULL;
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->audio_frame != NULL) {
    (*_av_frame_free)((void**)&ctx->audio_frame);
    ctx->audio_frame = NULL;
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->video_frame != NULL) {
    (*_av_frame_free)((void**)&ctx->video_frame);
    ctx->video_frame = NULL;
  }
  if (ctx->fmt_ctx != NULL) {
    if (ctx->fmt_ctx->priv_data != NULL) {
      (*_av_free)(ctx->fmt_ctx->priv_data);
      ctx->fmt_ctx->priv_data = NULL;
    }
    (*_avformat_free_context)(ctx->fmt_ctx);
    ctx->fmt_ctx = NULL;
  }
  if (ctx->video_codec_ctx != NULL) {
    (*_avcodec_free_context)(&ctx->video_codec_ctx);
    ctx->video_codec_ctx = NULL;
  }
  if (ctx->audio_codec_ctx != NULL) {
    (*_avcodec_free_context)(&ctx->audio_codec_ctx);
    ctx->audio_codec_ctx = NULL;
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->src_pic != NULL) {
    (*_av_frame_free)((void**)&ctx->src_pic);
    ctx->src_pic = NULL;
  }
  if (ctx->sws_ctx != NULL) {
    (*_sws_freeContext)(ctx->sws_ctx);
    ctx->sws_ctx = NULL;
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->swr_ctx != NULL) {
    (*_swr_free)(&ctx->swr_ctx);
    ctx->swr_ctx = NULL;
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->audio_buffer != NULL) {
    (*_av_free)(ctx->audio_buffer);
    ctx->audio_buffer = NULL;
  }
  if (ctx->pkt != NULL) {
    ctx->pkt->data = NULL;
    ctx->pkt->size = 0;
    (*_av_packet_free)(&ctx->pkt);
    ctx->pkt = NULL;
  }
  freeFFContext(e,c,ctx);
  return JNI_TRUE;
}

jboolean outputClose(FFContext* ctx)
{
  return outputClose_ctx(NULL,NULL,ctx);
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_outputClose
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return outputClose_ctx(e,c,ctx);
}

jboolean writeHeader(FFContext* ctx)
{
  if (ctx == NULL) return JNI_FALSE;

  printf("write header:fmt_ctx=%p\n", ctx->fmt_ctx);

  int ret = (*_avformat_write_header)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("MediaOutput:avformat_write_header failed : %d\n", ret);
  }
  return ret >= 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_writeHeader
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return writeHeader(ctx);
}

jboolean writePacket(FFContext* ctx, jint stream, jbyte* data, jint offset, jint length, jboolean keyFrame)
{
  if (ctx == NULL) return JNI_FALSE;

  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = (uint8_t*)(data + offset);
  ctx->pkt->size = length;
  ctx->pkt->stream_index = stream;

  ctx->pkt->dts = ctx->last_dts;
  ctx->pkt->pts = ctx->last_pts;
  ctx->pkt->duration = ctx->last_duration;

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);  //packet : this function will take ownership ??? bug ???
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  return ret == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_writePacket
  (JNIEnv *e, jobject c, jlong ctxptr, jint stream, jbyteArray data, jint offset, jint length, jboolean keyFrame)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jbyte *cdata = (jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean result = writePacket(ctx, stream, cdata, offset, length, keyFrame);

  e->ReleasePrimitiveArrayCritical(data, cdata, JNI_ABORT);

  return result;
}
