//audio decoder codebase

jboolean audioDecoderStart_ctx(JNIEnv *e, jobject c, FFContext*ctx, jint codec_id, jint chs, jint freq)
{
  if (ctx == NULL) return JNI_FALSE;

  int ret;
  ctx->audio_codec = (*_avcodec_find_decoder)(codec_id);
  if (ctx->audio_codec == NULL) {
    printf("MediaAudioDecoder : codec == null\n");
    return JNI_FALSE;
  }
  ctx->audio_codec_ctx = (*_avcodec_alloc_context3)(ctx->audio_codec);

  //set default values
  ctx->audio_codec_ctx->codec_id = (AVCodecID)codec_id;

  ret = (*_avcodec_open2)(ctx->audio_codec_ctx, ctx->audio_codec, NULL);
  if (ret < 0) {
    printf("MediaAudioDecoder:avcodec_open2() failed : %d\n", ret);
    return JNI_FALSE;
  }

  ctx->frame = (*_av_frame_alloc)();
  if (ctx->frame == NULL) {
    printf("MediaAudioDecoder:av_frame_alloc() failed\n");
    return JNI_FALSE;
  }

  ctx->pkt = AVPacket_New();

  ctx->chs = chs;
  ctx->freq = freq;

  if (ctx->chs != -1) {
    switch (ctx->chs) {
      case 1: (*_av_channel_layout_copy)(&ctx->audio_codec_ctx->ch_layout, &channel_layout_1); ctx->dst_nb_channels = 1; break;
      case 2: (*_av_channel_layout_copy)(&ctx->audio_codec_ctx->ch_layout, &channel_layout_2); ctx->dst_nb_channels = 2; break;
      case 4: (*_av_channel_layout_copy)(&ctx->audio_codec_ctx->ch_layout, &channel_layout_4); ctx->dst_nb_channels = 4; break;
    }
  }
  if (ctx->freq != -1) {
    ctx->audio_codec_ctx->sample_rate = ctx->freq;
  }

  if (ctx->decode_buffer == NULL) {
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
    ctx->decode_buffer_size = 1024*1024;
  }

  return JNI_TRUE;
}

FFContext* audioDecoderStart(jint codec_id, jint chs, jint freq)
{
  FFContext *ctx = newFFContext(NULL,NULL);
  if (ctx == NULL) return 0;

  if (!audioDecoderStart_ctx(NULL, NULL, ctx, codec_id, chs, freq)) {
    freeFFContext(NULL, NULL, ctx);
    ctx = NULL;
  }

  return ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_audioDecoderStart
  (JNIEnv *e, jobject c, jint codec_id, jint chs, jint freq)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return 0;

  if (!audioDecoderStart_ctx(NULL, NULL, ctx, codec_id, chs, freq)) {
    freeFFContext(e, c, ctx);
    ctx = NULL;
  }

  return (jlong)ctx;
}

void audioDecoderStop_ctx(JNIEnv *e, jobject c, FFContext* ctx)
{
  if (ctx == NULL) return;

  if (ctx->frame != NULL) {
    (*_av_frame_free)((void**)&ctx->frame);
    ctx->frame = NULL;
  }
  if (ctx->audio_codec_ctx != NULL) {
    (*_avcodec_free_context)(&ctx->audio_codec_ctx);
    ctx->audio_codec_ctx = NULL;
  }
  if (ctx->swr_ctx != NULL) {
    (*_swr_free)(&ctx->swr_ctx);
    ctx->swr_ctx = NULL;
  }
  if (ctx->pkt != NULL) {
    (*_av_packet_free)(&ctx->pkt);
    ctx->pkt = NULL;
  }
  if (ctx->jaudio != NULL) {
    e->DeleteGlobalRef(ctx->jaudio);
    ctx->jaudio = NULL;
  }
  if (ctx->decode_buffer != NULL) {
    (*_av_free)(ctx->decode_buffer);
    ctx->decode_buffer = NULL;
    ctx->decode_buffer_size = 0;
  }
  freeFFContext(e,c,ctx);
}

void audioDecoderStop(FFContext* ctx)
{
  if (ctx == NULL) return;

  audioDecoderStop_ctx(NULL, NULL, ctx);
}

JNIEXPORT void JNICALL Java_javaforce_jni_MediaJNI_audioDecoderStop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return;

  audioDecoderStop_ctx(e, c, ctx);
}

JFArray* audioDecoderDecode_data(FFContext* ctx, jint length)
{
  ctx->pkt->data = ctx->decode_buffer;
  ctx->pkt->size = length;

  int ret = (*_avcodec_send_packet)(ctx->audio_codec_ctx, ctx->pkt);  //caller retains pkt ownership
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;
  if (ret < 0) {
    printf("MediaAudioDecoder:avcodec_send_packet() failed : %d\n", ret);
    return NULL;
  }

  ret = (*_avcodec_receive_frame)(ctx->audio_codec_ctx, ctx->frame);  //frame alloced by codec (existing one unref first)
  if (ret < 0) {
    printf("MediaAudioDecoder:avcodec_receive_frame() failed : %d\n", ret);
    return NULL;
  }

  //setup conversion once chs/freq are known
  if (ctx->swr_ctx == NULL) {
    if (ctx->audio_codec_ctx->ch_layout.nb_channels == 0 || ctx->audio_codec_ctx->sample_rate == 0) {
      printf("MediaAudioDecoder : chs/sample_rate not known yet\n");
      return NULL;
    }
    if (ctx->chs == -1) {
      ctx->chs = ctx->audio_codec_ctx->ch_layout.nb_channels;
      printf("MediaAudioDecoder : chs=%d\n", ctx->chs);
    }
    if (ctx->freq == -1) {
      ctx->freq = ctx->audio_codec_ctx->sample_rate;
      printf("MediaAudioDecoder : freq=%d\n", ctx->freq);
    }
    //create audio conversion context
    ctx->swr_ctx = (*_swr_alloc)();
    AVChannelLayout new_layout;
    switch (ctx->chs) {
      case 1: (*_av_channel_layout_copy)(&new_layout, &channel_layout_1); ctx->dst_nb_channels = 1; break;
      case 2: (*_av_channel_layout_copy)(&new_layout, &channel_layout_2); ctx->dst_nb_channels = 2; break;
      case 4: (*_av_channel_layout_copy)(&new_layout, &channel_layout_4); ctx->dst_nb_channels = 4; break;
      default: printf("MediaAudioDecoder:unknown channel layout:%d\n", ctx->chs); return NULL;
    }
    AVChannelLayout src_layout;
    (*_av_channel_layout_copy)(&src_layout, &ctx->audio_codec_ctx->ch_layout);
    ctx->dst_sample_fmt = AV_SAMPLE_FMT_S16;
    ctx->src_rate = ctx->audio_codec_ctx->sample_rate;
    if (ff_debug_log) printf("MediaAudioDecoder.swr_alloc_set_opts2:%p,%d,%d,%p,%d,%d\n",
      &new_layout, ctx->dst_sample_fmt, ctx->freq,
      &src_layout, ctx->audio_codec_ctx->sample_fmt, ctx->src_rate);
    (*_swr_alloc_set_opts2)(&ctx->swr_ctx,
      &new_layout, ctx->dst_sample_fmt, ctx->freq,
      &src_layout, ctx->audio_codec_ctx->sample_fmt, ctx->src_rate,
      0, NULL);

    ret = (*_swr_init)(ctx->swr_ctx);
    if (ret < 0) {
      printf("MediaAudioDecoder:resample init failed : %d\n", ret);
    }
    ctx->dst_rate = ctx->freq;
  }

  //convert to new format
  int dst_nb_samples;
  dst_nb_samples = (int)(*_av_rescale_rnd)((*_swr_get_delay)(ctx->swr_ctx, ctx->src_rate)
    + ctx->frame->nb_samples, ctx->dst_rate, ctx->src_rate, AV_ROUND_UP);
  if (ff_debug_log) printf("MediaAudioDecoder.av_samples_alloc:%d,%d,%d\n", ctx->dst_nb_channels, dst_nb_samples, ctx->dst_sample_fmt);
  ret = (*_av_samples_alloc)(ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->dst_nb_channels
    , dst_nb_samples, ctx->dst_sample_fmt, 1);
  if (ret < 0) {
    printf("MediaAudioDecoder:av_samples_alloc() failed : %d\n", ret);
    return NULL_FRAME;
  }
  int converted_nb_samples = 0;
  converted_nb_samples = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, dst_nb_samples
    , ctx->frame->extended_data, ctx->frame->nb_samples);
  if (converted_nb_samples < 0) {
    if (ctx->audio_dst_data[0] != NULL) {
      (*_av_free)(ctx->audio_dst_data[0]);
      ctx->audio_dst_data[0] = NULL;
    }
    printf("MediaAudioDecoder:swr_convert failed : %d\n", converted_nb_samples);
    return NULL;
  }
  int count = converted_nb_samples * ctx->dst_nb_channels;
  //copy to JFArray
  JFArray* array = JFArray::create(count, 2, ARRAY_TYPE_SHORT);
  memcpy(array->getBufferShort(), (const jshort*)ctx->audio_dst_data[0], count*2);
  //free audio_dst_data
  if (ctx->audio_dst_data[0] != NULL) {
    (*_av_free)(ctx->audio_dst_data[0]);
    ctx->audio_dst_data[0] = NULL;
  }

  return array;
}

JFArray* audioDecoderDecode(FFContext* ctx, jbyte* data, jint offset, jint length)
{
  if (ctx == NULL) return NULL;

  //there should always be AV_INPUT_BUFFER_PADDING_SIZE bytes after the data to decode
  if (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }

  memcpy((jbyte*)ctx->decode_buffer, data+offset, length);

  uint8_t *pad = ctx->decode_buffer + offset + length;
  for(int a=0;a<AV_INPUT_BUFFER_PADDING_SIZE;a++) {
    *(pad++) = 0;
  }

  return audioDecoderDecode_data(ctx, length);
}

JNIEXPORT jshortArray JNICALL Java_javaforce_jni_MediaJNI_audioDecoderDecode
  (JNIEnv *e, jobject c, jlong ctxptr, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return NULL;

  //there should always be AV_INPUT_BUFFER_PADDING_SIZE bytes after the data to decode
  if (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }

  e->GetByteArrayRegion(data, offset, length, (jbyte*)ctx->decode_buffer);
  uint8_t *pad = ctx->decode_buffer + offset + length;
  for(int a=0;a<AV_INPUT_BUFFER_PADDING_SIZE;a++) {
    *(pad++) = 0;
  }

  JFArray* array = audioDecoderDecode_data(ctx, length);

  if (array == NULL) return NULL;

  int count = array->count;
  if (ctx->jaudio == NULL || ctx->jaudio_length != count) {
    if (ctx->jaudio != NULL) {
      //free old audio array
      e->DeleteGlobalRef(ctx->jaudio);
    }
    ctx->jaudio_length = count;
    ctx->jaudio = (jshortArray)e->NewGlobalRef(e->NewShortArray(count));
  }
  e->SetShortArrayRegion(ctx->jaudio, 0, ctx->jaudio_length, (const jshort*)ctx->audio_dst_data[0]);
  jfArrayFree(array);

  return ctx->jaudio;
}

jint audioDecoderGetChannels(FFContext* ctx)
{
  if (ctx == NULL) return 0;
  return ctx->dst_nb_channels;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_audioDecoderGetChannels
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  return audioDecoderGetChannels(ctx);
}

jint audioDecoderGetSampleRate(FFContext* ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_audioDecoderGetSampleRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  return audioDecoderGetSampleRate(ctx);
}

jboolean audioDecoderChange(FFContext* ctx, jint chs, jint freq)
{
  //TODO
  return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_audioDecoderChange
  (JNIEnv *e, jobject c, jlong ctxptr, jint chs, jint freq)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  return audioDecoderChange(ctx, chs, freq);
}

//video decoder codebase

jboolean videoDecoderStart_ctx(FFContext* ctx, jint codec_id, jint width, jint height)
{
  if (ctx == NULL) return JNI_FALSE;

  int ret;
  ctx->video_codec = (*_avcodec_find_decoder)(codec_id);
  if (ctx->video_codec == NULL) {
    printf("MediaVideoDecoder : codec == null\n");
    return JNI_FALSE;
  }
  ctx->video_codec_ctx = (*_avcodec_alloc_context3)(ctx->video_codec);

  //set default values
  ctx->video_codec_ctx->codec_id = (AVCodecID)codec_id;
  ctx->video_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;

  ret = (*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, NULL);
  if (ret < 0) {
    printf("MediaVideoDecoder:avcodec_open2() failed : %d\n", ret);
    return JNI_FALSE;
  }

  ctx->frame = (*_av_frame_alloc)();
  if (ctx->frame == NULL) {
    printf("MediaAudioDecoder:av_frame_alloc() failed\n");
    return JNI_FALSE;
  }

  ctx->pkt = AVPacket_New();

  ctx->width = width;
  ctx->height = height;

  if (ctx->decode_buffer == NULL) {
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
    ctx->decode_buffer_size = 1024*1024;
  }

  return JNI_TRUE;
}

FFContext* videoDecoderStart(jint codec_id, jint width, jint height)
{
  FFContext *ctx = newFFContext(NULL,NULL);
  if (ctx == NULL) return JNI_FALSE;

  if (!videoDecoderStart_ctx(ctx, codec_id, width, height)) {
    freeFFContext(NULL,NULL,ctx);
    ctx = NULL;
  }

  return ctx;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_MediaJNI_videoDecoderStart
  (JNIEnv *e, jobject c, jint codec_id, jint width, jint height)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  if (!videoDecoderStart_ctx(ctx, codec_id, width, height)) {
    freeFFContext(e,c,ctx);
    ctx = NULL;
  }

  return (jlong)ctx;
}

void videoDecoderStop_ctx(FFContext* ctx)
{
  if (ctx == NULL) return;
  if (ctx->frame != NULL) {
    (*_av_frame_free)((void**)&ctx->frame);
    ctx->frame = NULL;
  }
  if (ctx->video_codec_ctx != NULL) {
    (*_avcodec_free_context)(&ctx->video_codec_ctx);
    ctx->video_codec_ctx = NULL;
  }
  if (ctx->sws_ctx != NULL) {
    (*_sws_freeContext)(ctx->sws_ctx);
    ctx->sws_ctx = NULL;
  }
  if (ctx->pkt != NULL) {
    (*_av_packet_free)(&ctx->pkt);
    ctx->pkt = NULL;
  }
  if (ctx->jvideo != NULL) {
    ctx->e->DeleteGlobalRef(ctx->jvideo);
    ctx->jvideo = NULL;
  }
  if (ctx->video_dst_data[0] != NULL) {
    (*_av_free)(ctx->video_dst_data[0]);
    ctx->video_dst_data[0] = NULL;
  }
  if (ctx->rgb_video_dst_data[0] != NULL) {
    (*_av_free)(ctx->rgb_video_dst_data[0]);
    ctx->rgb_video_dst_data[0] = NULL;
  }
  if (ctx->decode_buffer != NULL) {
    (*_av_free)(ctx->decode_buffer);
    ctx->decode_buffer = NULL;
    ctx->decode_buffer_size = 0;
  }
}

void videoDecoderStop(FFContext* ctx)
{
  if (ctx == NULL) return;

  videoDecoderStop_ctx(ctx);

  freeFFContext(NULL,NULL,ctx);
}

JNIEXPORT void JNICALL Java_javaforce_jni_MediaJNI_videoDecoderStop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return;

  videoDecoderStop_ctx(ctx);

  freeFFContext(e,c,ctx);
}

JFArray* videoDecoderDecode_data(FFContext* ctx, jint length)
{
  if (ctx == NULL) return NULL;

  ctx->pkt->data = ctx->decode_buffer;
  ctx->pkt->size = length;

  int ret = (*_avcodec_send_packet)(ctx->video_codec_ctx, ctx->pkt);  //caller retains pkt ownership
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;
  if (ret < 0) {
    printf("MediaVideoDecoder:avcodec_send_packet() failed : %d\n", ret);
    return NULL;
  }

  ret = (*_avcodec_receive_frame)(ctx->video_codec_ctx, ctx->frame);  //frame alloced by codec (existing one unref first)
  if (ret < 0) {
    printf("MediaVideoDecoder:avcodec_receive_frame failed() : %d\n", ret);
    return NULL;
  }

  //setup conversion once width/height are known
  if (ctx->sws_ctx == NULL) {
    if (ctx->video_codec_ctx->width == 0 || ctx->video_codec_ctx->height == 0) {
      printf("MediaVideoDecoder : width/height not known yet\n");
      return NULL;
    }
    if (ctx->width == -1 && ctx->height == -1) {
      ctx->width = ctx->video_codec_ctx->width;
      ctx->height = ctx->video_codec_ctx->height;
    }
    //create video buffer
    ctx->rgb_video_dst_bufsize = (*_av_image_alloc)(ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize
      , ctx->width, ctx->height
      , AV_PIX_FMT_BGRA, 1);
    if (ctx->rgb_video_dst_bufsize < 0)
    {
      printf("MediaVideoDecoder:av_image_alloc() failed : %d\n", ctx->rgb_video_dst_bufsize);
      return JNI_FALSE;
    }
    //create video conversion context
    ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
      , ctx->width, ctx->height, AV_PIX_FMT_BGRA
      , SWS_BILINEAR, NULL, NULL, NULL);

  }
  (*_sws_scale)(ctx->sws_ctx, ctx->frame->data, ctx->frame->linesize, 0, ctx->video_codec_ctx->height
    , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);

  int px_count = ctx->width * ctx->height;
  JFArray* jfarray = JFArray::create(px_count, 4, ARRAY_TYPE_INT);
  memcpy(jfarray->getBufferInt(), ctx->rgb_video_dst_data[0], px_count * 4);

  return jfarray;
}

JFArray* videoDecoderDecode(FFContext* ctx, jbyte* data, jint offset, jint length)
{
  if (ctx == NULL) return NULL;

  //there should always be AV_INPUT_BUFFER_PADDING_SIZE bytes after the data to decode
  if (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }
  memcpy((jbyte*)ctx->decode_buffer, data + offset, length);
  uint8_t *pad = ctx->decode_buffer + offset + length;
  for(int a=0;a<AV_INPUT_BUFFER_PADDING_SIZE;a++) {
    *(pad++) = 0;
  }

  return videoDecoderDecode_data(ctx, length);
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_MediaJNI_videoDecoderDecode
  (JNIEnv *e, jobject c, jlong ctxptr, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return NULL;

  //there should always be AV_INPUT_BUFFER_PADDING_SIZE bytes after the data to decode
  if (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + AV_INPUT_BUFFER_PADDING_SIZE > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }
  e->GetByteArrayRegion(data, offset, length, (jbyte*)ctx->decode_buffer);
  uint8_t *pad = ctx->decode_buffer + offset + length;
  for(int a=0;a<AV_INPUT_BUFFER_PADDING_SIZE;a++) {
    *(pad++) = 0;
  }

  JFArray* jfarray = videoDecoderDecode_data(ctx, length);

  if (jfarray == NULL) return NULL;

  if (ctx->jvideo == NULL) {
    int px_count = ctx->width * ctx->height;
    ctx->jvideo_length = px_count;
    ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));
  }

  e->SetIntArrayRegion(ctx->jvideo, 0, ctx->jvideo_length, (const jint*)jfarray->getBufferInt());
  jfArrayFree(jfarray);

  return ctx->jvideo;
}

jint videoDecoderGetWidth(FFContext* ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_videoDecoderGetWidth
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  return videoDecoderGetWidth(ctx);
}

jint videoDecoderGetHeight(FFContext* ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_MediaJNI_videoDecoderGetHeight
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;
  return videoDecoderGetHeight(ctx);
}

jfloat videoDecoderGetFrameRate(FFContext* ctx)
{
  if (ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx->framerate.den == 0) return 0.0f;
  return ctx->video_codec_ctx->framerate.num / ctx->video_codec_ctx->framerate.den;
}

JNIEXPORT jfloat JNICALL Java_javaforce_jni_MediaJNI_videoDecoderGetFrameRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0.0f;

  return videoDecoderGetFrameRate(ctx);
}

jboolean videoDecoderChange(FFContext* ctx, jint new_width, jint new_height)
{
  if (ctx == NULL) return JNI_FALSE;
  if (ctx->video_stream == NULL) return JNI_FALSE;  //no video

  if (ctx->rgb_video_dst_data[0] != NULL) {
    (*_av_free)(ctx->rgb_video_dst_data[0]);
    ctx->rgb_video_dst_data[0] = NULL;
  }

  ctx->rgb_video_dst_bufsize = (*_av_image_alloc)(ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize
    , new_width, new_height, AV_PIX_FMT_BGRA, 1);
  if (ctx->rgb_video_dst_bufsize < 0) {
    printf("MediaDecoder:av_image_alloc() failed : %d\n", ctx->rgb_video_dst_bufsize);
    return JNI_FALSE;
  }

  if (ctx->sws_ctx != NULL) {
    (*_sws_freeContext)(ctx->sws_ctx);
    ctx->sws_ctx = NULL;
  }

  ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
    , new_width, new_height, AV_PIX_FMT_BGRA
    , SWS_BILINEAR, NULL, NULL, NULL);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MediaJNI_videoDecoderChange
  (JNIEnv *e, jobject c, jlong ctxptr, jint new_width, jint new_height)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return JNI_FALSE;

  if (!videoDecoderChange(ctx, new_width, new_height)) {
    return JNI_FALSE;
  }

  if (ctx->jvideo != NULL) {
    e->DeleteGlobalRef(ctx->jvideo);
  }
  ctx->jvideo_length = ctx->rgb_video_dst_bufsize/4;
  ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));

  return JNI_TRUE;
}
