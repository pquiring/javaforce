//audio decoder codebase

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaAudioDecoder_nstart
  (JNIEnv *e, jobject c, jint codec_id, jint chs, jint freq)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
//  printf("context=%p\n", ctx);
  ctx->audio_codec = (*_avcodec_find_decoder)(codec_id);
  if (ctx->audio_codec == NULL) {
    printf("MediaAudioDecoder : codec == null\n");
    return 0;
  }
  ctx->audio_codec_ctx = (*_avcodec_alloc_context3)(ctx->audio_codec);

  //set default values
  ctx->audio_codec_ctx->codec_id = (AVCodecID)codec_id;

  if (((*_avcodec_open2)(ctx->audio_codec_ctx, ctx->audio_codec, NULL)) < 0) {
    printf("avcodec_open2() failed\n");
    return 0;
  }

  if ((ctx->frame = (*_av_frame_alloc)()) == NULL) return JNI_FALSE;

  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  ctx->chs = chs;
  ctx->freq = freq;

  ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
  ctx->decode_buffer_size = 1024*1024;

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaAudioDecoder_nstop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
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
  deleteFFContext(e,c,ctx);
}

JNIEXPORT jshortArray JNICALL Java_javaforce_media_MediaAudioDecoder_ndecode
  (JNIEnv *e, jobject c, jlong ctxptr, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return NULL;
  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  //there should always be 64 bytes after the data to decode
  if (length + 64 > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + 64 > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }
  memcpy(ctx->decode_buffer, dataptr + offset, length);
  uint8_t *pad = dataptr + offset + length;
  for(int a=0;a<64;a++) {
    *(pad++) = 0;
  }

  ctx->pkt->size = length;
  ctx->pkt->data = ctx->decode_buffer;

  int ret = (*_avcodec_send_packet)(ctx->audio_codec_ctx, ctx->pkt);
  e->ReleasePrimitiveArrayCritical(data, (jbyte*)dataptr, JNI_ABORT);
  ctx->pkt->data = NULL;
  if (ret < 0) {
    printf("Error:avcodec_send_packet() == %d\n", ret);
    ctx->pkt->size = 0;
    return NULL;
  }

  while (1) {
    ret = (*_avcodec_receive_frame)(ctx->audio_codec_ctx, ctx->frame);
    if (ret < 0) break;

    //setup conversion once width/height are known
    if (ctx->jaudio == NULL) {
      if (ctx->audio_codec_ctx->ch_layout.nb_channels == 0 || ctx->audio_codec_ctx->sample_rate == 0) {
        printf("MediaAudioDecoder : chs/sample_rate not known yet\n");
        return NULL;
      }
      if (ctx->chs == -1) {
        ctx->chs = ctx->audio_codec_ctx->ch_layout.nb_channels;
      }
      if (ctx->freq == -1) {
        ctx->freq = ctx->audio_codec_ctx->sample_rate;
      }
      //create audio conversion context
      ctx->swr_ctx = (*_swr_alloc)();
      AVChannelLayout new_layout;
      switch (ctx->chs) {
        case 1: (*_av_channel_layout_copy)(&new_layout, &channel_layout_1); ctx->dst_nb_channels = 1; break;
        case 2: (*_av_channel_layout_copy)(&new_layout, &channel_layout_2); ctx->dst_nb_channels = 2; break;
        case 4: (*_av_channel_layout_copy)(&new_layout, &channel_layout_4); ctx->dst_nb_channels = 4; break;
        default: return JNI_FALSE;
      }
      AVChannelLayout src_layout;
      (*_av_channel_layout_copy)(&src_layout, &ctx->audio_codec_ctx->ch_layout);
      ctx->dst_sample_fmt = AV_SAMPLE_FMT_S16;
      ctx->src_rate = ctx->audio_codec_ctx->sample_rate;
      (*_swr_alloc_set_opts2)(&ctx->swr_ctx,
        &new_layout, ctx->dst_sample_fmt, ctx->freq,
        &src_layout, ctx->audio_codec_ctx->sample_fmt, ctx->src_rate,
        0, NULL);

      ret = (*_swr_init)(ctx->swr_ctx);
      if (ret < 0) {
        printf("resample init failed:%d\n", ret);
      }
      ctx->dst_rate = ctx->freq;
    }

    //convert to new format
    int dst_nb_samples;
    dst_nb_samples = (int)(*_av_rescale_rnd)((*_swr_get_delay)(ctx->swr_ctx, ctx->src_rate)
      + ctx->frame->nb_samples, ctx->dst_rate, ctx->src_rate, AV_ROUND_UP);
    if (((*_av_samples_alloc)(ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->dst_nb_channels
      , dst_nb_samples, ctx->dst_sample_fmt, 1)) < 0) return NULL_FRAME;
    int converted_nb_samples = 0;
    converted_nb_samples = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, dst_nb_samples
      , ctx->frame->extended_data, ctx->frame->nb_samples);
    if (converted_nb_samples < 0) {
      printf("FFMPEG:Resample failed!\n");
      return NULL_FRAME;
    }
    int count = converted_nb_samples * ctx->dst_nb_channels;
    if (ctx->jaudio == NULL || ctx->jaudio_length != count) {
      if (ctx->jaudio != NULL) {
        //free old audio array
        e->DeleteGlobalRef(ctx->jaudio);
      }
      ctx->jaudio_length = count;
      ctx->jaudio = (jshortArray)e->NewGlobalRef(e->NewShortArray(count));
    }
    e->SetShortArrayRegion(ctx->jaudio, 0, ctx->jaudio_length, (const jshort*)ctx->audio_dst_data[0]);
    //free audio_dst_data
    if (ctx->audio_dst_data[0] != NULL) {
      (*_av_free)(ctx->audio_dst_data[0]);
      ctx->audio_dst_data[0] = NULL;
    }
  }

  return ctx->jaudio;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaAudioDecoder_ngetChannels
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return 0;
  return ctx->dst_nb_channels;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaAudioDecoder_ngetSampleRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

//video decoder codebase

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaVideoDecoder_nstart
  (JNIEnv *e, jobject c, jint codec_id, jint width, jint height)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
//  printf("context=%p\n", ctx);
  ctx->video_codec = (*_avcodec_find_decoder)(codec_id);
  if (ctx->video_codec == NULL) {
    printf("MediaVideoDecoder : codec == null\n");
    return 0;
  }
  ctx->video_codec_ctx = (*_avcodec_alloc_context3)(ctx->video_codec);

  //set default values
  ctx->video_codec_ctx->codec_id = (AVCodecID)codec_id;
  ctx->video_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;

  if (((*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, NULL)) < 0) {
    printf("avcodec_open2() failed\n");
    return 0;
  }

  if ((ctx->frame = (*_av_frame_alloc)()) == NULL) return JNI_FALSE;

  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  ctx->width = width;
  ctx->height = height;

  ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
  ctx->decode_buffer_size = 1024*1024;

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaVideoDecoder_nstop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
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
    e->DeleteGlobalRef(ctx->jvideo);
    ctx->jvideo = NULL;
  }
  if (ctx->decode_buffer != NULL) {
    (*_av_free)(ctx->decode_buffer);
    ctx->decode_buffer = NULL;
    ctx->decode_buffer_size = 0;
  }
  deleteFFContext(e,c,ctx);
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_MediaVideoDecoder_ndecode
  (JNIEnv *e, jobject c, jlong ctxptr, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return NULL;
  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  //there should always be 64 bytes after the data to decode
  if (length + 64 > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + 64 > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }
  memcpy(ctx->decode_buffer, dataptr + offset, length);
  uint8_t *pad = dataptr + offset + length;
  for(int a=0;a<64;a++) {
    *(pad++) = 0;
  }

  ctx->pkt->size = length;
  ctx->pkt->data = ctx->decode_buffer;

  int ret = (*_avcodec_send_packet)(ctx->video_codec_ctx, ctx->pkt);
  e->ReleasePrimitiveArrayCritical(data, (jbyte*)dataptr, JNI_ABORT);
  ctx->pkt->data = NULL;
  if (ret < 0) {
    printf("Error:avcodec_send_packet() == %d\n", ret);
    ctx->pkt->size = 0;
    return NULL;
  }

  while (1) {
    ret = (*_avcodec_receive_frame)(ctx->video_codec_ctx, ctx->frame);
    if (ret < 0) break;

    //setup conversion once width/height are known
    if (ctx->jvideo == NULL) {
      if (ctx->video_codec_ctx->width == 0 || ctx->video_codec_ctx->height == 0) {
        printf("MediaVideoDecoder : width/height not known yet\n");
        return NULL;
      }
      if (ctx->width == -1 && ctx->height == -1) {
        ctx->width = ctx->video_codec_ctx->width;
        ctx->height = ctx->video_codec_ctx->height;
      }
      //create video conversion context
      ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
        , ctx->width, ctx->height, AV_PIX_FMT_BGRA
        , SWS_BILINEAR, NULL, NULL, NULL);

      int px_count = ctx->width * ctx->height;
      ctx->jvideo_length = px_count;
      ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));
    }

    jint *jvideo_ptr = (jint*)ctx->e->GetPrimitiveArrayCritical(ctx->jvideo, &isCopy);
    if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

    ctx->rgb_video_dst_data[0] = (uint8_t*)jvideo_ptr;
    ctx->rgb_video_dst_linesize[0] = ctx->width * 4;
    (*_sws_scale)(ctx->sws_ctx, ctx->frame->data, ctx->frame->linesize, 0, ctx->video_codec_ctx->height
      , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);

    ctx->e->ReleasePrimitiveArrayCritical(ctx->jvideo, jvideo_ptr, 0);
  }

  return ctx->jvideo;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaVideoDecoder_ngetWidth
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaVideoDecoder_ngetHeight
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_MediaVideoDecoder_ngetFrameRate
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = (FFContext*)ctxptr;
  if (ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx == NULL) return 0;
  if (ctx->video_codec_ctx->framerate.den == 0) return 0;
  return ctx->video_codec_ctx->framerate.num / ctx->video_codec_ctx->framerate.den;
}