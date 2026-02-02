//MediaInput

jboolean inputOpenFile_ctx(FFContext *ctx, const char* file, const char* format) {
  if (ctx == NULL) return JNI_FALSE;

  ctx->fmt_ctx = (*_avformat_alloc_context)();

  ctx->input_fmt = (*_av_find_input_format)(format);
  if (ctx->input_fmt == NULL) {
    printf("MediaInput:av_find_input_format() failed : %s\n", format);
    return JNI_FALSE;
  }

  int ret;
  ret = (*_avformat_open_input)((void**)&ctx->fmt_ctx, file, ctx->input_fmt, NULL);
  ctx->input_fmt = NULL;  //do not free
  if (ret != 0) {
    printf("MediaInput:avformat_open_input() failed : %d\n", ret);
    return JNI_FALSE;
  }

  ret = (*_avformat_find_stream_info)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("MediaInput:avformat_find_stream_info() failed : %d\n", ret);
    return JNI_FALSE;
  }

  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);

  decoder_alloc_frame(ctx);
  decoder_alloc_packet(ctx);
  return JNI_TRUE;
}

FFContext* inputOpenFile(const char* file, const char* format)
{
  FFContext *ctx = newFFContext(NULL, NULL);
  if (ctx == NULL) return 0;

  if (!inputOpenFile_ctx(ctx, file, format)) {
    freeFFContext(NULL, NULL, ctx);
    ctx = NULL;
  }

  return ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_inputOpenFile
  (JNIEnv *e, jobject c, jstring file, jstring format)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return 0;

  const char *cfile = e->GetStringUTFChars(file, NULL);
  const char *cformat = e->GetStringUTFChars(format, NULL);
  if (!inputOpenFile_ctx(ctx, cfile, cformat)) {
    freeFFContext(e, c, ctx);
    ctx = NULL;
  }
  e->ReleaseStringUTFChars(file, cfile);
  e->ReleaseStringUTFChars(format, cformat);

  return (jlong)ctx;
}

jboolean inputOpenIO_ctx(FFContext *ctx)
{
  int ret;

  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 0, (void*)ctx, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  if (ctx->io_ctx == NULL) {
    printf("MediaInput:avio_alloc_context() failed\n");
    return JNI_FALSE;
  }
//  ctx->io_ctx->direct = 1;

  ctx->fmt_ctx = (*_avformat_alloc_context)();
  ctx->fmt_ctx->pb = ctx->io_ctx;

  ret = (*_avformat_open_input)((void**)&ctx->fmt_ctx, "stream", NULL, NULL);
  if (ret != 0) {
    printf("MediaInput:avformat_open_input() failed : %d\n", ret);
    return JNI_FALSE;
  }

  ret = (*_avformat_find_stream_info)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("MediaInput:avformat_find_stream_info() failed : %d\n", ret);
    return JNI_FALSE;
  }

  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);

  decoder_alloc_frame(ctx);
  decoder_alloc_packet(ctx);

  return JNI_TRUE;
}

FFContext* inputOpenIO(MediaIO* mio)
{
  FFContext *ctx = newFFContext(NULL,NULL);
  if (ctx == NULL) return JNI_FALSE;

  memcpy(&ctx->ffm_mio, mio, sizeof(MediaIO));

  if (!inputOpenIO_ctx(ctx)) {
    freeFFContext(NULL,NULL,ctx);
    ctx = NULL;
  }

  return ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_inputOpenIO
  (JNIEnv *e, jobject c, jobject mio)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->GetMediaIO();

  if (!inputOpenIO_ctx(ctx)) {
    freeFFContext(e,c,ctx);
    ctx = NULL;
  }

  return (jlong)ctx;
}

jboolean inputClose_ctx(JNIEnv *e, jobject c, FFContext *ctx)
{
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
  if (ctx->decode_buffer != NULL) {
    (*_av_free)(ctx->decode_buffer);
    ctx->decode_buffer = NULL;
    ctx->decode_buffer_size = 0;
  }
  freeFFContext(e,c,ctx);
  return JNI_TRUE;
}

jboolean inputClose(FFContext *ctx) {
  return inputClose_ctx(NULL, NULL, ctx);
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_inputClose
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e,c,ctxptr);
  if (ctx == NULL) return JNI_FALSE;
  return inputClose_ctx(e,c,ctx);
}

jboolean inputOpenVideo(FFContext* ctx, jint new_width, jint new_height)
{
  if (ctx == NULL) return JNI_FALSE;

  decoder_open_video_codec(ctx, new_width, new_height);

  if (ctx->decode_buffer == NULL) {
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
    ctx->decode_buffer_size = 1024*1024;
  }

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_inputOpenVideo
  (JNIEnv *e, jobject c, jlong ctxptr, jint new_width, jint new_height)
{
  FFContext *ctx = castFFContext(e,c,ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return inputOpenVideo(ctx, new_width, new_height);
}

jboolean inputOpenAudio(FFContext* ctx, jint new_chs, jint new_freq)
{
  if (ctx == NULL) return JNI_FALSE;

  decoder_open_audio_codec(ctx, new_chs, new_freq);

  if (ctx->decode_buffer == NULL) {
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
    ctx->decode_buffer_size = 1024*1024;
  }

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_inputOpenAudio
  (JNIEnv *e, jobject c, jlong ctxptr, jint new_chs, jint new_freq)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return inputOpenAudio(ctx, new_chs, new_freq);
}

jlong getDuration(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->fmt_ctx == NULL) return 0;
  if (ctx->fmt_ctx->duration << 1 == 0) return 0;  //0x8000000000000000
  return ctx->fmt_ctx->duration / AV_TIME_BASE;  //return in seconds
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_getDuration
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  return getDuration(ctx);
}

jint getVideoWidth(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getVideoWidth
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx->video_codec_ctx == NULL) return 0;
  return getVideoWidth(ctx);
}

jint getVideoHeight(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getVideoHeight
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx->video_codec_ctx == NULL) return 0;
  return getVideoHeight(ctx);
}

jfloat getVideoFrameRate(FFContext *ctx) {
  if (ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx == NULL) return 0;
  AVRational value = ctx->video_stream->avg_frame_rate;
  float num = (float)value.num;
  float den = (float)value.den;
  if (den == 0) return 0;
  return num / den;
}

JNIEXPORT jfloat JNICALL Java_javaforce_jni_MediaJNI_getVideoFrameRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  return getVideoFrameRate(ctx);
}

jint getVideoKeyFrameInterval(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  if (ctx->video_codec_ctx == NULL) return -1;
  return ctx->video_codec_ctx->gop_size;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getVideoKeyFrameInterval
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  return getVideoKeyFrameInterval(ctx);
}

jint getAudioChannels(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  return ctx->dst_nb_channels;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getAudioChannels
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  return getAudioChannels(ctx);
}

jint getAudioSampleRate(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getAudioSampleRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  return getAudioSampleRate(ctx);
}

jint inputRead(FFContext* ctx)
{
  if (ctx == NULL) return 0;

  if (ctx->pkt == NULL) {
    printf("MediaInput.read():pkt==NULL\n");
    return 0;
  }

  //read "packet"
  if ((*_av_read_frame)(ctx->fmt_ctx, ctx->pkt) >= 0) {  //packet : caller retains ownership
    ctx->pkt_key_frame = ((ctx->pkt->flags & 0x0001) == 0x0001);
  } else {
    return 0;
  }

  return ctx->pkt->size;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_inputRead
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  return inputRead(ctx);
}

jboolean getPacketKeyFrame(FFContext* ctx)
{
  if (ctx == NULL) return JNI_FALSE;

  return ctx->pkt_key_frame;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_getPacketKeyFrame
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return getPacketKeyFrame(ctx);
}

jint getPacketData(FFContext* ctx, jbyte* data, jint offset, jint length)
{
  if (ctx == NULL) return -1;

  if (length > ctx->pkt->size) return -1;

  memcpy(data + offset, (const jbyte*)ctx->pkt->data, length);

  int stream_index = ctx->pkt->stream_index;

  (*_av_packet_unref)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  return stream_index;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_getPacketData
  (JNIEnv *e, jobject c, jlong ctxptr, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  e->SetByteArrayRegion(data, offset, length, (const jbyte*)ctx->pkt->data);

  int stream_index = ctx->pkt->stream_index;

  (*_av_packet_unref)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  return stream_index;
}

jboolean inputSeek(FFContext* ctx, jlong seconds)
{
  if (ctx == NULL) return JNI_FALSE;

  //AV_TIME_BASE is 1000000fps
  seconds *= AV_TIME_BASE;
  int ret = (*_av_seek_frame)(ctx->fmt_ctx, -1, seconds, 0);
  if (ret < 0) {
    printf("MediaInput:av_seek_frame() failed : %d\n", ret);
  }
  return ret >= 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_inputSeek
  (JNIEnv *e, jobject c, jlong ctxptr, jlong seconds)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return inputSeek(ctx, seconds);
}
