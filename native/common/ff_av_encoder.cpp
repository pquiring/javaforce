//audio encoder codebase

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaAudioEncoder_nstart
  (JNIEnv *e, jobject c, jint codec_id, jint bit_rate, jint chs, jint freq)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return 0;

  ctx->config_audio_bit_rate = bit_rate;
  ctx->chs = chs;
  ctx->freq = freq;

  int ret;
  ctx->audio_codec = (*_avcodec_find_encoder)(codec_id);
  if (ctx->audio_codec == NULL) {
    printf("MediaAudioEncoder : codec == null\n");
    return 0;
  }
  ctx->audio_codec_ctx = (*_avcodec_alloc_context3)(ctx->audio_codec);

  //set default values
  ctx->audio_codec_ctx->codec_id = (AVCodecID)codec_id;

  if (!encoder_init_audio(ctx)) {
    printf("MediaAudioEncoder.encoder_init_audio() failed\n");
    return 0;
  }

  //create audio packet
  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);

  ctx->encode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
  ctx->encode_buffer_size = 1024*1024;

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaAudioEncoder_nstop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return;
  if (ctx->audio_frame != NULL) {
    (*_av_frame_free)((void**)&ctx->audio_frame);
    ctx->audio_frame = NULL;
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
  if (ctx->encode_buffer != NULL) {
    (*_av_free)(ctx->encode_buffer);
    ctx->encode_buffer = NULL;
    ctx->encode_buffer_size = 0;
  }
  freeFFContext(e,c,ctx);
}

static jbyteArray av_encoder_addAudioFrame(FFContext *ctx, short *sams, int offset, int length)
{
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:%p,%p,%d,%d\n", ctx, sams, offset, length);
  int ret;
  int nb_samples = length / ctx->chs;
  int buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, AV_SAMPLE_FMT_S16, 0);
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:buffer_size:%d\n", buffer_size);
  void* samples_data = (*_av_mallocz)(buffer_size);
  //copy sams -> samples_data
  memcpy(samples_data, sams + offset, length * 2);

  if (ctx->swr_ctx != NULL) {
    //convert sample format (some codecs do not support S16)
    //sample rate is not changed
    if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:av_samples_alloc:%p,%p,%d,%d,%d,0\n", ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->chs, nb_samples, ctx->audio_codec_ctx->sample_fmt);
    ret = (*_av_samples_alloc)(ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->chs, nb_samples, ctx->audio_codec_ctx->sample_fmt, 0);
    if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:av_samples_alloc=%d\n", ret);
    if (ret < 0) {
      printf("MediaAudioEncoder:av_samples_alloc() failed : %d\n", ret);
      return NULL;
    }
    ctx->audio_src_data[0] = (uint8_t*)samples_data;
    if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:swr_convert:%p,%p,%d,%p,%d\n", ctx->swr_ctx, ctx->audio_dst_data, nb_samples, ctx->audio_src_data, nb_samples);
    ret = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, nb_samples, ctx->audio_src_data, nb_samples);
    if (ret < 0) {
      printf("MediaAudioEncoder:swr_convert() failed : %d\n", ret);
      return NULL;
    }
  } else {
    ctx->audio_dst_data[0] = (uint8_t*)samples_data;
  }

  (*_av_frame_make_writable)(ctx->audio_frame);  //ensure we can write to it now
  ctx->audio_frame->nb_samples = nb_samples;
  buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, ctx->audio_codec_ctx->sample_fmt, 0);
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:avcodec_fill_audio_frame\n");
  ret = (*_avcodec_fill_audio_frame)(ctx->audio_frame, ctx->chs, ctx->audio_codec_ctx->sample_fmt, ctx->audio_dst_data[0], buffer_size, 0);
  if (ret < 0) {
    printf("MediaAudioEncoder:avcodec_fill_audio_frame() failed : %d\n", ret);
    return NULL;
  }

  ctx->audio_frame->pts = ctx->audio_pts;  //(*_av_rescale_q)(ctx->audio_pts, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:send_frame:%p,%p\n", ctx->audio_codec_ctx, ctx->audio_frame);
  ret = (*_avcodec_send_frame)(ctx->audio_codec_ctx, ctx->audio_frame);
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:send_frame=%d\n", ret);
  if (ret < 0) {
    printf("MediaAudioEncoder:avcodec_send_frame() failed : %d:%s\n", ret, ctx->error_string(ret));
    return NULL;
  }

  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:init_packet:%p\n", ctx->pkt);
  if (ctx->pkt == NULL) {
    printf("MediaAudioEncode:ctx->pkt == null\n");
    return NULL;
  }
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  ret = (*_avcodec_receive_packet)(ctx->audio_codec_ctx, ctx->pkt);
  if (ret < 0) {
    printf("MediaAudioEncoder:avcodec_receive_packet() failed : %d\n", ret);
    return NULL;
  }
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudioFrame:receive_frame:%d\n", ret);

  //(*_av_packet_rescale_ts)(ctx->pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  ctx->last_dts = ctx->pkt->dts;
  ctx->last_pts = ctx->pkt->pts;
  ctx->last_duration = ctx->pkt->duration;
  if (ctx->swr_ctx != NULL) {
    //free audio_dst_data (only the first pointer : regardless if format was plannar : it's alloced as one large block)
    if (ctx->audio_dst_data[0] != NULL) {
      (*_av_free)(ctx->audio_dst_data[0]);
      ctx->audio_dst_data[0] = NULL;
    }
  }
  ctx->audio_pts += nb_samples;

  jbyteArray array = ctx->e->NewByteArray(ctx->pkt->size);

  ctx->e->SetByteArrayRegion(array, 0, ctx->pkt->size, (jbyte*)ctx->pkt->data);

  (*_av_free)(samples_data);

  return array;
}

static jbyteArray av_encoder_addAudio(FFContext *ctx, short *sams, int offset, int length) {
  if (ff_debug_log) printf("MediaAudioEncoder.av_encoder_addAudio:%p,%d,%d\n", sams, offset, length);
  int frame_size = length;
  if (!ctx->audio_frame_size_variable) {
    frame_size = ctx->audio_frame_size;
    if (ctx->audio_buffer_size > 0) {
      //fill audio_buffer with input samples
      int size = ctx->audio_frame_size - ctx->audio_buffer_size;
      if (size > length) size = length;
      memcpy(ctx->audio_buffer + ctx->audio_buffer_size, sams + offset, size * 2);
      ctx->audio_buffer_size += size;
      if (ctx->audio_buffer_size < ctx->audio_frame_size) return NULL;  //frame still not full
      av_encoder_addAudioFrame(ctx, ctx->audio_buffer, 0, ctx->audio_buffer_size);
      ctx->audio_buffer_size = 0;
      offset += size;
      length -= size;
    }
  }

  while (length > 0) {
    int size = length;
    if (size > frame_size) size = frame_size;
    if (size < frame_size && !ctx->audio_frame_size_variable) {
      //partial frame : copy the rest to temp storage for next call
      memcpy(ctx->audio_buffer, sams + offset, size * 2);
      ctx->audio_buffer_size = size;
      return NULL;
    }
    jbyteArray array = av_encoder_addAudioFrame(ctx, sams, offset, size);
    return array;
  }

  return NULL;
}

JNIEXPORT jbyteArray JNICALL Java_javaforce_media_MediaAudioEncoder_nencode
  (JNIEnv *e, jobject c, jlong ctxptr, jshortArray sams, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return NULL;

  if (ctx->audio_codec_ctx == NULL) return NULL;

  jboolean isCopy;
  jshort* sams_ptr = (jshort*)e->GetPrimitiveArrayCritical(sams, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jbyteArray ba = av_encoder_addAudio(ctx, sams_ptr, offset, length);

  e->ReleasePrimitiveArrayCritical(sams, sams_ptr, JNI_ABORT);

  return ba;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaAudioEncoder_ngetAudioFramesize
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return 0;

  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->frame_size;
}

//video encoder codebase

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaVideoEncoder_nstart
  (JNIEnv *e, jobject c, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval)
{
  FFContext *ctx = newFFContext(e,c);
  if (ctx == NULL) return 0;
  int ret;
//  printf("context=%p\n", ctx);
  ctx->video_codec = (*_avcodec_find_encoder)(codec_id);
  if (ctx->video_codec == NULL) {
    printf("MediaVideoEncoder : codec == null\n");
    return 0;
  }
  ctx->video_codec_ctx = (*_avcodec_alloc_context3)(ctx->video_codec);

  //set default values
  ctx->video_codec_ctx->codec_id = (AVCodecID)codec_id;
  ctx->video_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;

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

  ret = (*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, NULL);
  if (ret < 0) {
    printf("MediaVideoEncoder:avcodec_open2() failed : %d\n", ret);
    return 0;
  }

  if ((ctx->video_frame = (*_av_frame_alloc)()) == NULL) return 0;

  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  ctx->width = width;
  ctx->height = height;

  ctx->encode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
  ctx->encode_buffer_size = 1024*1024;

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaVideoEncoder_nstop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return;
  if (ctx->video_frame != NULL) {
    (*_av_frame_free)((void**)&ctx->video_frame);
    ctx->video_frame = NULL;
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
  if (ctx->encode_buffer != NULL) {
    (*_av_free)(ctx->encode_buffer);
    ctx->encode_buffer = NULL;
    ctx->encode_buffer_size = 0;
  }
  freeFFContext(e,c,ctx);
}

static jbyteArray av_encoder_addVideo(FFContext *ctx, int *px)
{
  int length = ctx->org_width * ctx->org_height * 4;
  (*_av_frame_make_writable)(ctx->video_frame);  //ensure we can write to it now
  if (ctx->scaleVideo) {
    //copy px -> ctx->src_pic->data[0];
    memcpy(ctx->src_pic->data[0], px, length);
    //convert src_pic -> video_frame
    (*_sws_scale)(ctx->sws_ctx, ctx->src_pic->data, ctx->src_pic->linesize, 0, ctx->org_height
      , ctx->video_frame->data, ctx->video_frame->linesize);
  } else {
    //copy px -> ctx->video_frame->data[0];
    memcpy(ctx->video_frame->data[0], px, length);
  }

  ctx->video_frame->pts = ctx->video_pts;  //(*_av_rescale_q)(ctx->video_pts, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
  int ret = (*_avcodec_send_frame)(ctx->video_codec_ctx, ctx->video_frame);
  if (ret < 0) {
    printf("MediaVideoEncoder:avcodec_send_frame() failed : %d\n", ret);
    return NULL;
  }

  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  ret = (*_avcodec_receive_packet)(ctx->video_codec_ctx, ctx->pkt);
  if (ret < 0) {
    printf("MediaVideoEncoder:avcodec_receive_packet() failed : %d\n", ret);
    return NULL;
  }

  //(*_av_packet_rescale_ts)(ctx->pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
//    printf("packet:%lld/%lld/%lld\n", pkt->dts, pkt->pts, pkt->duration);
  ctx->last_dts = ctx->pkt->dts;
  ctx->last_pts = ctx->pkt->pts;
  ctx->last_duration = ctx->pkt->duration;
//    log_packet("video", ctx->fmt_ctx, pkt);
  ctx->video_pts++;

  jbyteArray array = ctx->e->NewByteArray(ctx->pkt->size);

  ctx->e->SetByteArrayRegion(array, 0, ctx->pkt->size, (jbyte*)ctx->pkt->data);

  return array;
}

JNIEXPORT jbyteArray JNICALL Java_javaforce_media_MediaVideoEncoder_nencode
  (JNIEnv *e, jobject c, jlong ctxptr, jintArray px, jint offset, jint length)
{
  FFContext *ctx = castFFContext(e, c, ctxptr);
  if (ctx == NULL) return NULL;

  if (ctx->video_codec_ctx == NULL) return NULL;

  jboolean isCopy;
  jint *px_ptr = (jint*)e->GetPrimitiveArrayCritical(px, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jbyteArray ba = av_encoder_addVideo(ctx, (int*)px_ptr);

  e->ReleasePrimitiveArrayCritical(px, px_ptr, JNI_ABORT);

  return ba;
}
