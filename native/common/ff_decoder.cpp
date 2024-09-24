//decoder codebase

//returns stream idx >= 0
static int decoder_open_codec_context(FFContext *ctx, AVFormatContext *fmt_ctx, int type)
{
  int ret;
  int stream_idx;
  AVStream *stream;
  AVCodec *codec;
  stream_idx = (*_av_find_best_stream)(ctx->fmt_ctx, type, -1, -1, NULL, 0);
  if (stream_idx >= 0) {
    stream = (AVStream*)ctx->fmt_ctx->streams[stream_idx];
    codec = (*_avcodec_find_decoder)(stream->codecpar->codec_id);
    if (codec == NULL) {
      return -1;
    }
    ctx->codec_ctx = (*_avcodec_alloc_context3)(codec);
    (*_avcodec_parameters_to_context)(ctx->codec_ctx, stream->codecpar);
//    ctx->codec_ctx->flags |= AV_CODEC_FLAG_LOW_DELAY;
    if ((ret = (*_avcodec_open2)(ctx->codec_ctx, codec, NULL)) < 0) {
      return ret;
    }
  }
  return stream_idx;
}


static jboolean decoder_open_codecs(FFContext *ctx, int new_width, int new_height, int new_chs, int new_freq) {
  AVCodecContext *codec_ctx;
  if ((ctx->video_stream_idx = decoder_open_codec_context(ctx, ctx->fmt_ctx, AVMEDIA_TYPE_VIDEO)) >= 0) {
    ctx->video_stream = (AVStream*)ctx->fmt_ctx->streams[ctx->video_stream_idx];
    ctx->video_codec_ctx = ctx->codec_ctx;
    if (new_width == -1) new_width = ctx->video_codec_ctx->width;
    if (new_height == -1) new_height = ctx->video_codec_ctx->height;
    if ((ctx->video_dst_bufsize = (*_av_image_alloc)(ctx->video_dst_data, ctx->video_dst_linesize
      , ctx->video_codec_ctx->width, ctx->video_codec_ctx->height
      , ctx->video_codec_ctx->pix_fmt, 1)) < 0)
    {
      return JNI_FALSE;
    }
    if ((ctx->rgb_video_dst_bufsize = (*_av_image_alloc)(ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize
      , new_width, new_height
      , AV_PIX_FMT_BGRA, 1)) < 0)
    {
      return JNI_FALSE;
    }
    ctx->jvideo_length = ctx->rgb_video_dst_bufsize/4;
    ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));
    //create video conversion context
    ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
      , new_width, new_height, AV_PIX_FMT_BGRA
      , SWS_BILINEAR, NULL, NULL, NULL);
  }

  if ((ctx->audio_stream_idx = decoder_open_codec_context(ctx, ctx->fmt_ctx, AVMEDIA_TYPE_AUDIO)) >= 0) {
    ctx->audio_stream = (AVStream*)ctx->fmt_ctx->streams[ctx->audio_stream_idx];
    ctx->audio_codec_ctx = ctx->codec_ctx;
    //create audio conversion context
    ctx->swr_ctx = (*_swr_alloc)();
    if (new_chs == -1) new_chs = ctx->audio_codec_ctx->ch_layout.nb_channels;
    AVChannelLayout new_layout;
    switch (new_chs) {
      case 1: (*_av_channel_layout_copy)(&new_layout, &channel_layout_1); ctx->dst_nb_channels = 1; break;
      case 2: (*_av_channel_layout_copy)(&new_layout, &channel_layout_2); ctx->dst_nb_channels = 2; break;
      case 4: (*_av_channel_layout_copy)(&new_layout, &channel_layout_4); ctx->dst_nb_channels = 4; break;
      default: return JNI_FALSE;
    }
    AVChannelLayout src_layout;
    (*_av_channel_layout_copy)(&src_layout, &ctx->audio_codec_ctx->ch_layout);
    ctx->dst_sample_fmt = AV_SAMPLE_FMT_S16;
    ctx->src_rate = ctx->audio_codec_ctx->sample_rate;
    if (new_freq == -1) new_freq = ctx->src_rate;
    (*_swr_alloc_set_opts2)(&ctx->swr_ctx,
      &new_layout, ctx->dst_sample_fmt, new_freq,
      &src_layout, ctx->audio_codec_ctx->sample_fmt, ctx->src_rate,
      0, NULL);

    int ret;
    ret = (*_swr_init)(ctx->swr_ctx);
    if (ret < 0) {
      printf("resample init failed:%d\n", ret);
    }
    ctx->dst_rate = new_freq;
  }

  if ((ctx->frame = (*_av_frame_alloc)()) == NULL) return JNI_FALSE;
  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  return JNI_TRUE;
}

/**
 * Starts demuxing/decoding a stream.
 * @param new_width - scale video to new width (-1 = use stream width)
 * @param new_height - scale video to new height (-1 = use stream height)
 * @param new_chs - # of channels to mix to (-1 = use stream channels)
 * @param new_freq - output sampling rate (-1 = use stream rate)
 * @param seekable - can you seek input? (true=file false=stream)
 * NOTE : Audio output is always 16bit
 */

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_start
  (JNIEnv *e, jobject c, jobject mio, jint new_width, jint new_height, jint new_chs, jint new_freq, jboolean seekable)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  int res;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->GetMediaIO();

  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 0, (void*)ctx, (void*)&read_packet, (void*)&write_packet, seekable ? (void*)&seek_packet : NULL);
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

  return decoder_open_codecs(ctx, new_width, new_height, new_chs, new_freq);
}

/**
 * Alternative start that works with files.
 *
 * Example: start("/dev/video0", "v4l2", ...);
 *
 * NOTE:format may be NULL
 */

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_startFile
  (JNIEnv *e, jobject c, jstring file, jstring format, jint new_width, jint new_height, jint new_chs, jint new_freq)
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

  return decoder_open_codecs(ctx, new_width, new_height, new_chs, new_freq);
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaDecoder_stop
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return;
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
}

/** Reads next frame in stream and returns what type it was : AUDIO_FRAME, VIDEO_FRAME, NULL_FRAME or END_FRAME */
JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_read
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->pkt == NULL) {
    printf("MediaDecoder.read():pkt==NULL\n");
    return END_FRAME;
  }
  //read another frame
  if ((*_av_read_frame)(ctx->fmt_ctx, ctx->pkt) >= 0) {
    ctx->pkt_key_frame = ((ctx->pkt->flags & 0x0001) == 0x0001);
  } else {
    return END_FRAME;
  }

  //try to decode another frame
  if (ctx->pkt->stream_index == ctx->video_stream_idx) {
    //extract a video frame
    int ret = (*_avcodec_send_packet)(ctx->video_codec_ctx, ctx->pkt);
    if (ret < 0) {
      printf("Error:%d\n", ret);
      return NULL_FRAME;
    }
    _av_free_packet(ctx->pkt);
    while (1) {
      ret = (*_avcodec_receive_frame)(ctx->video_codec_ctx, ctx->frame);
      if (ret < 0) break;
      (*_av_image_copy)(ctx->video_dst_data, ctx->video_dst_linesize
        , ctx->frame->data, ctx->frame->linesize
        , ctx->video_codec_ctx->pix_fmt, ctx->video_codec_ctx->width, ctx->video_codec_ctx->height);
      //convert image to RGBA format
      (*_sws_scale)(ctx->sws_ctx, ctx->video_dst_data, ctx->video_dst_linesize, 0, ctx->video_codec_ctx->height
        , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);
    }
    return VIDEO_FRAME;
  }

  if (ctx->pkt->stream_index == ctx->audio_stream_idx) {
    //extract an audio frame
    int ret = (*_avcodec_send_packet)(ctx->audio_codec_ctx, ctx->pkt);
    if (ret < 0) {
      printf("Error:%d\n", ret);
      return NULL_FRAME;
    }
    _av_free_packet(ctx->pkt);
    while (1) {
      ret = (*_avcodec_receive_frame)(ctx->audio_codec_ctx, ctx->frame);
      if (ret < 0) break;
  //    int unpadded_linesize = frame.nb_samples * avutil.av_get_bytes_per_sample(audio_codec_ctx.sample_fmt);
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
    return AUDIO_FRAME;
  }

  //discard unknown packet
  _av_free_packet(ctx->pkt);

  return NULL_FRAME;
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_MediaDecoder_getVideo
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return NULL;
  if (ctx->jvideo == NULL) return NULL;
  e->SetIntArrayRegion(ctx->jvideo, 0, ctx->jvideo_length, (const jint*)ctx->rgb_video_dst_data[0]);
  return ctx->jvideo;
}

JNIEXPORT jshortArray JNICALL Java_javaforce_media_MediaDecoder_getAudio
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return NULL;
  return ctx->jaudio;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getWidth
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getHeight
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_MediaDecoder_getFrameRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx == NULL) return 0;
  AVRational value = ctx->video_stream->avg_frame_rate;
  float num = (float)value.num;
  float den = (float)value.den;
  if (den == 0) return 0;
  return num / den;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaDecoder_getDuration
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->fmt_ctx == NULL) return 0;
  if (ctx->fmt_ctx->duration << 1 == 0) return 0;  //0x8000000000000000
  return ctx->fmt_ctx->duration / AV_TIME_BASE;  //return in seconds
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getSampleRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getChannels
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  return ctx->dst_nb_channels;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getBitsPerSample
  (JNIEnv *e, jobject c)
{
  return 16;  //output is always converted to 16bits/sample (signed)
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_seek
  (JNIEnv *e, jobject c, jlong seconds)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  //AV_TIME_BASE is 1000000fps
  seconds *= AV_TIME_BASE;
/*      int ret = avformat.avformat_seek_file(fmt_ctx, -1
    , seconds - AV_TIME_BASE_PARTIAL, seconds, seconds + AV_TIME_BASE_PARTIAL, 0);*/
  int ret = (*_av_seek_frame)(ctx->fmt_ctx, -1, seconds, 0);
  if (ret < 0) printf("av_seek_frame failed:%d\n", ret);
  return ret >= 0;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getVideoBitRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->bit_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getAudioBitRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->bit_rate;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_isKeyFrame
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  return ctx->pkt_key_frame;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_resize
  (JNIEnv *e, jobject c, jint new_width, jint new_height)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  if (ctx->video_stream == NULL) return JNI_FALSE;  //no video

  if (ctx->rgb_video_dst_data[0] != NULL) {
    (*_av_free)(ctx->rgb_video_dst_data[0]);
    ctx->rgb_video_dst_data[0] = NULL;
  }

  if ((ctx->rgb_video_dst_bufsize = (*_av_image_alloc)(ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize
    , new_width, new_height
    , AV_PIX_FMT_BGRA, 1)) < 0) return JNI_FALSE;

  if (ctx->jvideo != NULL) {
    e->DeleteGlobalRef(ctx->jvideo);
  }
  ctx->jvideo_length = ctx->rgb_video_dst_bufsize/4;
  ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));

  if (ctx->sws_ctx != NULL) {
    (*_sws_freeContext)(ctx->sws_ctx);
    ctx->sws_ctx = NULL;
  }

  ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
    , new_width, new_height, AV_PIX_FMT_BGRA
    , SWS_BILINEAR, NULL, NULL, NULL);

  return JNI_TRUE;
}
