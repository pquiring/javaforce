//MediaInput

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaInput_nopenFile
  (JNIEnv *e, jclass c, jstring file, jstring format)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  int res;

  ctx->fmt_ctx = (*_avformat_alloc_context)();

  const char *cformat = e->GetStringUTFChars(format, NULL);
  if (cformat != NULL) {
    ctx->input_fmt = (*_av_find_input_format)(cformat);
    if (ctx->input_fmt == NULL) {
      printf("FFMPEG:av_find_input_format failed:%s\n", cformat);
      e->ReleaseStringUTFChars(format, cformat);
      return JNI_FALSE;
    }
  }
  e->ReleaseStringUTFChars(format, cformat);

  const char *cfile = e->GetStringUTFChars(file, NULL);
  if ((res = (*_avformat_open_input)((void**)&ctx->fmt_ctx, cfile, ctx->input_fmt, NULL)) != 0) {
    e->ReleaseStringUTFChars(file, cfile);
    printf("avformat_open_input() failed : %d\n", res);
    return JNI_FALSE;
  }
  e->ReleaseStringUTFChars(file, cfile);

  if ((res = (*_avformat_find_stream_info)(ctx->fmt_ctx, NULL)) < 0) {
    printf("avformat_find_stream_info() failed : %d\n", res);
    return JNI_FALSE;
  }

  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);

  decoder_alloc_frame(ctx);

  return (jlong)ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaInput_nopenIO
  (JNIEnv *e, jclass c, jobject mio)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  int res;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->GetMediaIO();

  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 0, (void*)ctx, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  if (ctx->io_ctx == NULL) return JNI_FALSE;
//  ctx->io_ctx->direct = 1;

  ctx->fmt_ctx = (*_avformat_alloc_context)();
  ctx->fmt_ctx->pb = ctx->io_ctx;

  if ((res = (*_avformat_open_input)((void**)&ctx->fmt_ctx, "stream", NULL, NULL)) != 0) {
    printf("avformat_open_input() failed : %d\n", res);
    return JNI_FALSE;
  }

  if ((res = (*_avformat_find_stream_info)(ctx->fmt_ctx, NULL)) < 0) {
    printf("avformat_find_stream_info() failed : %d\n", res);
    return JNI_FALSE;
  }

  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);

  decoder_alloc_frame(ctx);

  return (jlong)ctx;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaInput_nclose
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  if (ctx->io_ctx != NULL) {
    (*_avio_flush)(ctx->io_ctx);
    (*_av_free)(ctx->io_ctx->buffer);
    (*_av_free)(ctx->io_ctx);
    ctx->io_ctx = NULL;
  }
  if (ctx->fmt_ctx != NULL) {
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
  if (ctx->frame != NULL) {
    (*_av_frame_free)((void**)&ctx->frame);
    ctx->frame = NULL;
  }
  if (ctx->video_dst_data[0] != NULL) {
    (*_av_free)(ctx->video_dst_data[0]);
    ctx->video_dst_data[0] = NULL;
  }
  if (ctx->rgb_video_dst_data[0] != NULL) {
    (*_av_free)(ctx->rgb_video_dst_data[0]);
    ctx->rgb_video_dst_data[0] = NULL;
  }
  if (ctx->sws_ctx != NULL) {
    (*_sws_freeContext)(ctx->sws_ctx);
    ctx->sws_ctx = NULL;
  }
  if (ctx->swr_ctx != NULL) {
    (*_swr_free)(&ctx->swr_ctx);
    ctx->swr_ctx = NULL;
  }
  if (ctx->jaudio != NULL) {
    e->DeleteGlobalRef(ctx->jaudio);
    ctx->jaudio = NULL;
  }
  if (ctx->jvideo != NULL) {
    e->DeleteGlobalRef(ctx->jvideo);
    ctx->jvideo = NULL;
  }
  if (ctx->pkt != NULL) {
    (*_av_packet_free)(&ctx->pkt);
    ctx->pkt = NULL;
  }
  deleteFFContext(e,c,ctx);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaInput_nopenvideo
  (JNIEnv *e, jclass c, jlong ctxptr, jint new_width, jint new_height)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  decoder_open_video_codec(ctx, new_width, new_height);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaInput_nopenaudio
  (JNIEnv *e, jclass c, jlong ctxptr, jint new_chs, jint new_freq)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  decoder_open_audio_codec(ctx, new_chs, new_freq);

  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaInput_ngetDuration
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  if (ctx->fmt_ctx == NULL) return 0;
  if (ctx->fmt_ctx->duration << 1 == 0) return 0;  //0x8000000000000000
  return ctx->fmt_ctx->duration / AV_TIME_BASE;  //return in seconds
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_ngetVideoWidth
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_ngetVideoHeight
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_MediaInput_ngetVideoFrameRate
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx == NULL) return 0;
  AVRational value = ctx->video_stream->avg_frame_rate;
  float num = (float)value.num;
  float den = (float)value.den;
  if (den == 0) return 0;
  return num / den;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_ngetVideoKeyFrameInterval
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return -1;
  if (ctx->video_codec_ctx == NULL) return -1;
  return ctx->video_codec_ctx->gop_size;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_ngetAudioChannels
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  return ctx->dst_nb_channels;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_ngetAudioSampleRate
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_nread
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  if (ctx->pkt == NULL) {
    printf("MediaDecoder.read():pkt==NULL\n");
    return 0;
  }

  //read another frame (packet)
  if ((*_av_read_frame)(ctx->fmt_ctx, ctx->pkt) >= 0) {
    ctx->pkt_key_frame = ((ctx->pkt->flags & 0x0001) == 0x0001);
  } else {
    return 0;
  }

  return ctx->pkt->size;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaInput_ngetPacketKeyFrame
  (JNIEnv *e, jclass c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;
  return ctx->pkt_key_frame;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaInput_ngetPacketData
  (JNIEnv *e, jclass c, jlong ctxptr, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);

  if (length > ctx->pkt->size) return -1;

  e->SetByteArrayRegion(data, offset, length, (const jbyte*)ctx->pkt->data);

  return ctx->pkt->stream_index;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaInput_nseek
  (JNIEnv *e, jclass c, jlong ctxptr, jlong seconds)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;
  //AV_TIME_BASE is 1000000fps
  seconds *= AV_TIME_BASE;
  int ret = (*_av_seek_frame)(ctx->fmt_ctx, -1, seconds, 0);
  if (ret < 0) printf("av_seek_frame failed:%d\n", ret);
  return ret >= 0;
}
