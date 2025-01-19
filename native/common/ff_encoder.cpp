//encoder codebase

static jboolean encoder_add_stream(FFContext *ctx, int codec_id) {
  printf("encoder_add_stream\n");
  AVCodecContext *codec_ctx;
  AVStream *stream;
  AVCodec *codec;

  if (!(*_avformat_query_codec)(ctx->out_fmt, codec_id, FF_COMPLIANCE_NORMAL)) {
    printf("MediaEncoder:output format does not support codec %d\n", codec_id);
    return JNI_FALSE;
  }

  codec = (*_avcodec_find_encoder)(codec_id);
  if (codec == NULL) {
    printf("MediaEncoder:avcodec_find_encoder() failed\n");
    return JNI_FALSE;
  }
  stream = (*_avformat_new_stream)(ctx->fmt_ctx, codec);
  if (stream == NULL) {
    printf("MediaEncoder:avformat_new_stream() failed\n");
    return JNI_FALSE;
  }
  stream->id = ctx->fmt_ctx->nb_streams-1;
  codec_ctx = (*_avcodec_alloc_context3)(codec);

  switch (codec->type) {
    case AVMEDIA_TYPE_AUDIO:
      ctx->audio_stream = stream;
      ctx->audio_codec = codec;
      ctx->audio_codec_ctx = codec_ctx;
      break;
    case AVMEDIA_TYPE_VIDEO:
      ctx->video_stream = stream;
      ctx->video_codec = codec;
      ctx->video_codec_ctx = codec_ctx;
      break;
    case AVMEDIA_TYPE_UNKNOWN:
    case AVMEDIA_TYPE_DATA:
    case AVMEDIA_TYPE_SUBTITLE:
    case AVMEDIA_TYPE_ATTACHMENT:
    case AVMEDIA_TYPE_NB:
    default:
      printf("MediaEncoder:unsupported stream type:%d\n", codec->type);
      break;
  }

  if (((ctx->out_fmt->flags & AVFMT_GLOBALHEADER) != 0) && (!ctx->is_dash)) {
    codec_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
  }

  return JNI_TRUE;
}

static int get_size_alignment(int width, int height) {
  if (((width & 3) != 0) || ((height & 3) != 0)) {
    return 8;  //must be byte size alignment (performance degraded)
  }
  return 32;  //int alignment (faster)
}

static jboolean encoder_init_video(FFContext *ctx) {
  printf("encoder_init_video:codec_ctx=%p:codec=%p:stream=%p\n", ctx->video_codec_ctx, ctx->video_codec, ctx->video_stream);

  ctx->video_codec_ctx->bit_rate = ctx->config_video_bit_rate;
  ctx->video_codec_ctx->width = ctx->width;
  ctx->video_codec_ctx->height = ctx->height;
  if (ctx->config_fps_1000_1001) {
    ctx->video_codec_ctx->time_base.num = 1000;
    ctx->video_codec_ctx->time_base.den = ctx->fps * 1001;
    ctx->video_stream->time_base.num = 1000;
    ctx->video_stream->time_base.den = ctx->fps * 1001;
    ctx->video_codec_ctx->framerate.num = ctx->fps * 1001;
    ctx->video_codec_ctx->framerate.den = 1000;
  } else {
    ctx->video_codec_ctx->time_base.num = 1;
    ctx->video_codec_ctx->time_base.den = ctx->fps;
    ctx->video_stream->time_base.num = 1;
    ctx->video_stream->time_base.den = ctx->fps * 1024;
    ctx->video_codec_ctx->framerate.num = ctx->fps;
    ctx->video_codec_ctx->framerate.den = 1;
  }
  ctx->video_codec_ctx->gop_size = ctx->config_gop_size;
//  ctx->video_codec_ctx->keyint_min = ctx->config_gop_size;
  ctx->video_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
//  ctx->video_codec_ctx->max_b_frames = 12;

  if (ff_debug_trace) printf("encoder_init_video\n");
  //set video codec options
  switch (ctx->video_codec_ctx->codec_id) {
    case AV_CODEC_ID_MPEG4: {
      printf("codec=MPEG4\n");
      break;
    }
    case AV_CODEC_ID_H264: {
      printf("codec=H264\n");
      //see https://trac.ffmpeg.org/wiki/Encode/H.264
      switch (ctx->config_profileLevel) {
        case 1: (*_av_opt_set)(ctx->video_codec_ctx->priv_data, "profile", "baseline", 0); break;
        case 2: (*_av_opt_set)(ctx->video_codec_ctx->priv_data, "profile", "main", 0); break;
        case 3: (*_av_opt_set)(ctx->video_codec_ctx->priv_data, "profile", "high", 0); break;
      }
//      (*_av_opt_set)(ctx->video_codec_ctx->priv_data, "preset", "fast", 0);  //TODO
      break;
    }
    case AV_CODEC_ID_H265: {
      printf("codec=H265\n");
      break;
    };
    case AV_CODEC_ID_VP9: {
      printf("codec=VP9\n");
      //see https://trac.ffmpeg.org/wiki/Encode/VP9
      (*_av_opt_set)(ctx->video_codec_ctx->priv_data, "preset", "veryfast", 0);
      (*_av_opt_set)(ctx->video_codec_ctx->priv_data, "deadline", "realtime", 0);
      (*_av_opt_set_int)(ctx->video_codec_ctx->priv_data, "cpu-used", 8, 0);
//      (*_av_opt_set_int)(ctx->video_codec_ctx->priv_data, "tile-columns", 4, 0);
//      (*_av_opt_set_int)(ctx->video_codec_ctx->priv_data, "tile-rows", 4, 0);
      (*_av_opt_set_int)(ctx->video_codec_ctx->priv_data, "crf", 10, 0);
//      (*_av_opt_set_int)(ctx->video_codec_ctx->priv_data, "threads", 4, 0);
      break;
    }
    default: {
      printf("Unknown video codec:0x%x\n", ctx->video_codec_ctx->codec_id);
      break;
    }
  }
  ctx->video_codec_ctx->qmin = 2;
  ctx->video_codec_ctx->qmax = 40;
//  ctx->video_codec_ctx->delay = 1;
  if (ctx->config_compressionLevel != -1) {
    ctx->video_codec_ctx->compression_level = ctx->config_compressionLevel;
  }
//  ctx->video_codec_ctx->flags |= AV_CODEC_FLAG_LOW_DELAY;

  if (ff_debug_trace) printf("encoder_init_video:open:%p:%p\n", ctx->video_codec_ctx, ctx->video_codec);
  //open video codec
  int ret = (*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, NULL);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_open2() failed : %d\n", ret);
    return JNI_FALSE;
  }

  if (ff_debug_trace) printf("encoder_init_video:params:%p:%p\n", ctx->video_stream->codecpar, ctx->video_codec_ctx);
  //copy params (after codec is opened)
  ret = (*_avcodec_parameters_from_context)(ctx->video_stream->codecpar, ctx->video_codec_ctx);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_parameters_from_context() failed : %d\n", ret);
    return JNI_FALSE;
  }

  if (ff_debug_trace) printf("encoder_init_video\n");
  if (ctx->video_codec_ctx->pix_fmt != AV_PIX_FMT_BGRA) {
    ctx->scaleVideo = JNI_TRUE;
  }
  if (ctx->scaleVideo) {
    ctx->src_pic = (*_av_frame_alloc)();
    ctx->src_pic->width = ctx->org_width;
    ctx->src_pic->height = ctx->org_height;
    ctx->src_pic->format = AV_PIX_FMT_BGRA;
    if (ff_debug_trace) printf("encoder_init_video\n");
    ret = (*_av_frame_get_buffer)(ctx->src_pic, get_size_alignment(ctx->org_width, ctx->org_height));
    if (ret < 0) {
      printf("MediaEncoder:av_frame_get_buffer() failed : %d\n", ret);
      return JNI_FALSE;
    }
    if (ff_debug_trace) printf("encoder_init_video\n");
    ctx->sws_ctx = (*_sws_getContext)(ctx->org_width, ctx->org_height, AV_PIX_FMT_BGRA
      , ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt, SWS_BICUBIC, NULL, NULL, NULL);
  }
  if (ff_debug_trace) printf("encoder_init_video\n");
  //get caps
  ctx->video_delay = (ctx->video_codec->capabilities & AV_CODEC_CAP_DELAY) != 0;
  ctx->video_frame = (*_av_frame_alloc)();
  if (ctx->video_frame == NULL) {
    printf("MediaEncoder:av_frame_alloc() failed\n");
    return JNI_FALSE;
  }
  ctx->video_frame->width = ctx->width;
  ctx->video_frame->height = ctx->height;
  ctx->video_frame->format = ctx->video_codec_ctx->pix_fmt;
  if (ff_debug_trace) printf("encoder_init_video\n");
  ret = (*_av_frame_get_buffer)(ctx->video_frame, get_size_alignment(ctx->width, ctx->height));
  if (ret < 0) {
    printf("MediaEncoder:av_frame_get_buffer() failed : %d\n", ret);
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

static jboolean encoder_init_audio(FFContext *ctx) {
  printf("encoder_init_audio:codec_ctx=%p:codec=%p:stream=%p\n", ctx->audio_codec_ctx, ctx->audio_codec, ctx->audio_stream);

  //NOTE : ffmpeg/7.1 has deprecated AVCodec.sample_fmts : must use avcodec_get_supported_config() instead which was only added in 7.1
  if (ctx->audio_codec->sample_fmts != NULL) {
    bool have_fmt = false;
    for(int idx=0;;idx++) {
      AVSampleFormat fmt = ctx->audio_codec->sample_fmts[idx];
      if (fmt == -1) {
        if (!have_fmt) {
          ctx->audio_codec_ctx->sample_fmt = ctx->audio_codec->sample_fmts[0];
        }
        break;
      }
      printf("audio:available sample format:%d\n", fmt);
      if (fmt == AV_SAMPLE_FMT_S16) {
        //preferred format
        have_fmt = true;
        ctx->audio_codec_ctx->sample_fmt = fmt;
        break;
      }
      if (fmt == AV_SAMPLE_FMT_S16P && !have_fmt) {
        //second preferred format
        have_fmt = true;
        ctx->audio_codec_ctx->sample_fmt = fmt;
        break;
      }
    };
  } else {
    ctx->audio_codec_ctx->sample_fmt = AV_SAMPLE_FMT_S16;
  }

  ctx->audio_codec_ctx->bit_rate = ctx->config_audio_bit_rate;
  ctx->audio_codec_ctx->sample_rate = ctx->freq;
  switch (ctx->chs) {
    case 1: (*_av_channel_layout_copy)(&ctx->audio_codec_ctx->ch_layout, &channel_layout_1); ctx->dst_nb_channels = 1; break;
    case 2: (*_av_channel_layout_copy)(&ctx->audio_codec_ctx->ch_layout, &channel_layout_2); ctx->dst_nb_channels = 2; break;
    case 4: (*_av_channel_layout_copy)(&ctx->audio_codec_ctx->ch_layout, &channel_layout_4); ctx->dst_nb_channels = 4; break;
  }
  ctx->audio_codec_ctx->time_base.num = 1;
  ctx->audio_codec_ctx->time_base.den = ctx->freq;
  if (ctx->audio_stream != NULL) {
    ctx->audio_stream->time_base.num = 1;
    ctx->audio_stream->time_base.den = ctx->freq;
  }
  if (ctx->audio_codec_ctx->frame_size == 0) {
    ctx->audio_codec_ctx->frame_size = 160;
  }

  //set audio codec options
  switch (ctx->audio_codec_ctx->codec_id) {
    case AV_CODEC_ID_MP3: {
      break;
    }
    case AV_CODEC_ID_AAC: {
      ctx->audio_codec_ctx->frame_size = 1024;  //default frame size per channel
      break;
    }
    case AV_CODEC_ID_OPUS: {
      if (ctx->freq != 48000) {
        //opus only supports 48k
        ctx->audio_codec_ctx->sample_rate = 48000;
        ctx->audio_codec_ctx->time_base.num = 1;
        ctx->audio_codec_ctx->time_base.den = 48000;
        if (ctx->audio_stream != NULL) {
          ctx->audio_stream->time_base.num = 1;
          ctx->audio_stream->time_base.den = 48000;
        }
      }
      break;
    }
    default: {
      printf("Unknown audio codec:0x%x\n", ctx->audio_codec_ctx->codec_id);
      break;
    }
  }

  //open audio codec
  if (ff_debug_log) printf("avcodec_open2\n");
  int ret = (*_avcodec_open2)(ctx->audio_codec_ctx, ctx->audio_codec, NULL);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_open2() failed : %d\n", ret);
    return JNI_FALSE;
  }

  //copy params (after codec is opened)
  if (ctx->audio_stream != NULL) {
    ret = (*_avcodec_parameters_from_context)(ctx->audio_stream->codecpar, ctx->audio_codec_ctx);
    if (ret < 0) {
      printf("MediaEncoder:avcodec_parameters_from_context() failed : %d\n", ret);
      return JNI_FALSE;
    }
  }

  //create audio frame
  ctx->audio_frame = (*_av_frame_alloc)();
  if (ctx->audio_frame == NULL) {
    printf("MediaEncoder:av_frame_alloc() failed\n");
    return JNI_FALSE;
  }
  ctx->audio_frame->format = ctx->audio_codec_ctx->sample_fmt;
  ctx->audio_frame->sample_rate = ctx->freq;
  (*_av_channel_layout_copy)(&ctx->audio_frame->ch_layout, &ctx->audio_codec_ctx->ch_layout);
  printf("encoder_init_audio:frame_size=%d chs=%d\n", ctx->audio_codec_ctx->frame_size, ctx->chs);
  ctx->audio_frame_size = ctx->audio_codec_ctx->frame_size * ctx->chs;  //max samples that encoder will accept
  ctx->audio_frame_size_variable = (ctx->audio_codec->capabilities & AV_CODEC_CAP_VARIABLE_FRAME_SIZE) != 0;
  ctx->audio_frame->nb_samples = ctx->audio_codec_ctx->frame_size;
  ret = (*_av_frame_get_buffer)(ctx->audio_frame, 0);
  if (ret < 0) {
    printf("MediaEncoder:av_frame_get_buffer() failed : %d\n", ret);
    return JNI_FALSE;
  }
  if (!ctx->audio_frame_size_variable) {
    ctx->audio_buffer = (short*)(*_av_malloc)(ctx->audio_frame_size * 2);
    ctx->audio_buffer_size = 0;
  }
  if (ctx->audio_codec_ctx->sample_fmt == AV_SAMPLE_FMT_S16 && ctx->audio_codec_ctx->sample_rate == ctx->freq) {
    return JNI_TRUE;
  }

  //create audio conversion context
  ctx->swr_ctx = (*_swr_alloc)();
  (*_swr_alloc_set_opts2)(&ctx->swr_ctx,
    &ctx->audio_codec_ctx->ch_layout, ctx->audio_codec_ctx->sample_fmt, ctx->audio_codec_ctx->sample_rate,  //output
    &ctx->audio_codec_ctx->ch_layout, AV_SAMPLE_FMT_S16, ctx->freq,  //input
    0, NULL);

  ret = (*_swr_init)(ctx->swr_ctx);
  if (ret < 0) {
    printf("MediaEncoder:resample init failed : %d\n", ret);
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

static int read_null(FFContext *ctx, void*buf, int size) {
  return 0;
}

static int write_null(FFContext *ctx, void*buf, int size) {
  return size;
}

static jlong seek_null(FFContext *ctx, jlong offset, int how) {
  return 0;
}


static jboolean ff_startsWith(const char*str, const char*with) {
  int len = strlen(with);
  return strncmp(str, with, len) == 0;
}

static int io_open(AVFormatContext *fmt_ctx, AVIOContext **pb, const char *url, int flags, AVDictionary **options) {
  FFContext *ctx = (FFContext*)fmt_ctx->opaque;
  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  AVIOContext *io_ctx;
  if ((ff_startsWith(url, "init-")) || (ff_startsWith(url, "chunk-"))) {
    io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 1, (void*)ctx, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  } else {
    //usually .tmp junk data
    io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 1, (void*)ctx, (void*)&read_null, (void*)&write_null, (void*)&seek_null);
  }
  *pb = io_ctx;
  printf("ffmpeg:io_open:ctx=%p:pb=%p:url=%s\n", fmt_ctx, *pb, url);
  return 0;
}

static int io_close2(AVFormatContext *fmt_ctx, AVIOContext *pb) {
  FFContext *ctx = (FFContext*)fmt_ctx->opaque;

  printf("ffmpeg:io_close2:ctx=%p:pb=%p\n", fmt_ctx, pb);

  (*_avio_flush)(pb);
  (*_av_free)(pb->buffer);
  (*_av_free)(pb);

  return 0;
}

static jboolean single_file = JNI_FALSE;  //not working

static jboolean encoder_start(FFContext *ctx, const char *format, jint video_codec, jint audio_codec, const char *file, void*read, void*write, void*seek) {
  jboolean doVideo = video_codec != 0;
  jboolean doAudio = audio_codec != 0;

  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, format, NULL);
  if (ctx->fmt_ctx == NULL) {
    printf("MediaEncoder:Unable to find format : %s\n", format);
    return JNI_FALSE;
  }

  printf("encoder_start:fmt_ctx=%p:out_fmt=%p\n", ctx->fmt_ctx, ctx->fmt_ctx->oformat);

  if (strcmp(format, "dash") == 0) {
    ctx->is_dash = 1;
  }
  else if (strcmp(format, "mp4") == 0) {
    ctx->is_mp4 = 1;
  }
  if (ff_debug_trace) printf("encoder_start\n");

  if (ctx->is_dash) {
    if (single_file) {
      (*_av_opt_set_int)(ctx->fmt_ctx->priv_data, "single_file", 1, 0);
      (*_av_opt_set)(ctx->fmt_ctx->priv_data, "single_file_name", "dash.mp4", 0);
    }
    (*_av_opt_set_int)(ctx->fmt_ctx->priv_data, "streaming", 1, 0);
    (*_av_opt_set)(ctx->fmt_ctx->priv_data, "dash_segment_type", "mp4", 0);
//    (*_av_opt_set_int)(ctx->fmt_ctx->priv_data, "ldash", 1, 0);  //enable low latency dash
  } else {
    if (file == NULL) {
      void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
      ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 1, (void*)ctx, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
      if (ctx->io_ctx == NULL) {
        printf("MediaEncoder:avio_alloc_context() failed\n");
        return JNI_FALSE;
      }
    //  ctx->io_ctx->direct = 1;
      ctx->fmt_ctx->io_open = &io_open;
      ctx->fmt_ctx->io_close2 = &io_close2;
      ctx->fmt_ctx->opaque = ctx;
    } else {
      (*_avio_open)(&ctx->io_ctx, file, AVIO_FLAG_READ_WRITE);
      ctx->io_file = true;
    }
    printf("io_ctx=%p\n", ctx->io_ctx);
    ctx->fmt_ctx->pb = ctx->io_ctx;
  }

  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;

  if (ff_debug_trace) printf("encoder_start\n");
  if ((ctx->out_fmt->video_codec != AV_CODEC_ID_NONE) && doVideo) {
    if (video_codec == -1) {
      video_codec = ctx->out_fmt->video_codec;
    }
    if (!encoder_add_stream(ctx, video_codec)) {
      printf("MediaEncoder:encoder_add_stream(video) failed\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if ((ctx->out_fmt->audio_codec != AV_CODEC_ID_NONE) && doAudio) {
    if (audio_codec == -1) {
      audio_codec = ctx->out_fmt->audio_codec;
    }
    if (!encoder_add_stream(ctx, audio_codec)) {
      printf("MediaEncoder:encoder_add_stream(audio) failed\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if (ctx->video_stream != NULL) {
    if (!encoder_init_video(ctx)) {
      printf("MediaEncoder:encoder_init_video() failed\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if (ctx->audio_stream != NULL) {
    if (!encoder_init_audio(ctx)) {
      printf("MediaEncoder:encoder_init_audio() failed\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");

  if (ctx->is_dash) {
    (*_av_dict_set)(&ctx->fmt_ctx->metadata, "movflags", "+dash+delay_moov+skip_sidx+skip_trailer", AV_DICT_APPEND);
  }

  if (ff_debug_time_base) {
    printf("before:num/den=%d/%d\n", ctx->video_stream->time_base.num, ctx->video_stream->time_base.den);
    printf("    cc:num/den=%d/%d\n", ctx->video_codec_ctx->time_base.num, ctx->video_codec_ctx->time_base.den);
    printf("    fr:num/den=%d/%d\n", ctx->video_codec_ctx->framerate.num, ctx->video_codec_ctx->framerate.den);
  }
  int ret = (*_avformat_write_header)(ctx->fmt_ctx, NULL);
  if (ff_debug_time_base) {
    printf(" after:num/den=%d/%d\n", ctx->video_stream->time_base.num, ctx->video_stream->time_base.den);
    printf("    cc:num/den=%d/%d\n", ctx->video_codec_ctx->time_base.num, ctx->video_codec_ctx->time_base.den);
    printf("    fr:num/den=%d/%d\n", ctx->video_codec_ctx->framerate.num, ctx->video_codec_ctx->framerate.den);
  }
  if (ret < 0) {
    printf("MediaEncoder:avformat_write_header() failed : %d\n", ret);
  }

  if (ff_debug_trace) printf("encoder_start\n");
  if (ctx->audio_frame != NULL) {
    ctx->audio_pts = 0;
  }
  if (ctx->video_frame != NULL) {
    ctx->video_pts = 0;
  }
  if (ff_debug_trace) printf("encoder_start\n");
  (*_av_dump_format)(ctx->fmt_ctx, 0, "dump.avi", 1);
  if (ff_debug_trace) printf("encoder_start\n");
  if (doAudio) {
    printf("audio:codec->time_base=%d/%d stream->time_base=%d/%d\n", ctx->audio_codec_ctx->time_base.num, ctx->audio_codec_ctx->time_base.den, ctx->audio_stream->time_base.num, ctx->audio_stream->time_base.den);
    printf("audio:bitrate=%d samplerate=%d channels=%d\n", ctx->config_audio_bit_rate, ctx->freq, ctx->chs);
    printf("audio:framesize=%d (variable:%s)\n", ctx->audio_frame_size, ctx->audio_frame_size_variable ? "true" : "false");
    if (ctx->audio_codec_ctx->sample_fmt != AV_SAMPLE_FMT_S16) {
      printf("audio:conversion=%d to %d\n", AV_SAMPLE_FMT_S16, ctx->audio_codec_ctx->sample_fmt);
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if (doVideo) {
    printf("video:codec->time_base=%d/%d stream->time_base=%d/%d\n", ctx->video_codec_ctx->time_base.num, ctx->video_codec_ctx->time_base.den, ctx->video_stream->time_base.num, ctx->video_stream->time_base.den);
    printf("video:bitrate=%d fps=%d\n", ctx->config_video_bit_rate, ctx->fps);
    if (ctx->video_codec_ctx->framerate.den > 0) {
      printf("video:framerate=%d\n", ctx->video_codec_ctx->framerate.num / ctx->video_codec_ctx->framerate.den);
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_nstart
  (JNIEnv *e, jobject c, jobject mio, jint width, jint height, jint fps, jint chs, jint freq, jstring format, jint video_codec, jint audio_codec)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  jboolean doVideo = video_codec != 0;
  jboolean doAudio = audio_codec != 0;

  if (doVideo && (width <= 0 || height <= 0)) {
    printf("MediaEncoder:no audio or video\n");
    return JNI_FALSE;
  }
  if (doAudio && (chs <= 0 || freq <= 0)) {
    printf("MediaEncoder:audio with invalid chs/freq\n");
    return JNI_FALSE;
  }
  if (fps <= 0) fps = 24;  //must be valid, even for audio only

  ctx->mio = e->NewGlobalRef(mio);
  ctx->cls_mio = e->GetObjectClass(ctx->mio);
  ctx->GetMediaIO();

  jclass cls_encoder = e->FindClass("javaforce/media/MediaEncoder");
  jfieldID fid_fps_1000_1001 = e->GetFieldID(cls_encoder, "fps_1000_1001", "Z");
  jfieldID fid_framesPerKeyFrame = e->GetFieldID(cls_encoder, "framesPerKeyFrame", "I");
  jfieldID fid_videoBitRate = e->GetFieldID(cls_encoder, "videoBitRate", "I");
  jfieldID fid_audioBitRate = e->GetFieldID(cls_encoder, "audioBitRate", "I");
  jfieldID fid_compressionLevel = e->GetFieldID(cls_encoder, "compressionLevel", "I");
  jfieldID fid_profileLevel = e->GetFieldID(cls_encoder, "profileLevel", "I");

  ctx->config_fps_1000_1001 = e->GetBooleanField(c, fid_fps_1000_1001);
  ctx->config_gop_size = e->GetIntField(c, fid_framesPerKeyFrame);
  ctx->config_video_bit_rate = e->GetIntField(c, fid_videoBitRate);
  ctx->config_audio_bit_rate = e->GetIntField(c, fid_audioBitRate);
  ctx->config_compressionLevel = e->GetIntField(c, fid_compressionLevel);
  ctx->config_profileLevel = e->GetIntField(c, fid_profileLevel);

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
  ctx->chs = chs;
  ctx->freq = freq;

  ctx->pkt = AVPacket_New();

  const char *cformat = e->GetStringUTFChars(format, NULL);
  jboolean ret = encoder_start(ctx, cformat, video_codec, audio_codec, NULL, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  e->ReleaseStringUTFChars(format, cformat);

  return ret;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_nstartFile
  (JNIEnv *e, jobject c, jstring file, jint width, jint height, jint fps, jint chs, jint freq, jstring format, jint video_codec, jint audio_codec)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  jboolean doVideo = video_codec != 0;
  jboolean doAudio = audio_codec != 0;

  if (doVideo && (width <= 0 || height <= 0)) {
    printf("MediaEncoder:no audio or video\n");
    return JNI_FALSE;
  }
  if (doAudio && (chs <= 0 || freq <= 0)) {
    printf("MediaEncoder:audio with invalid chs/freq\n");
    return JNI_FALSE;
  }
  if (fps <= 0) fps = 24;  //must be valid, even for audio only

  jclass cls_encoder = e->FindClass("javaforce/media/MediaEncoder");
  jfieldID fid_fps_1000_1001 = e->GetFieldID(cls_encoder, "fps_1000_1001", "Z");
  jfieldID fid_framesPerKeyFrame = e->GetFieldID(cls_encoder, "framesPerKeyFrame", "I");
  jfieldID fid_videoBitRate = e->GetFieldID(cls_encoder, "videoBitRate", "I");
  jfieldID fid_audioBitRate = e->GetFieldID(cls_encoder, "audioBitRate", "I");
  jfieldID fid_compressionLevel = e->GetFieldID(cls_encoder, "compressionLevel", "I");
  jfieldID fid_profileLevel = e->GetFieldID(cls_encoder, "profileLevel", "I");

  ctx->config_fps_1000_1001 = e->GetBooleanField(c, fid_fps_1000_1001);
  ctx->config_gop_size = e->GetIntField(c, fid_framesPerKeyFrame);
  ctx->config_video_bit_rate = e->GetIntField(c, fid_videoBitRate);
  ctx->config_audio_bit_rate = e->GetIntField(c, fid_audioBitRate);
  ctx->config_compressionLevel = e->GetIntField(c, fid_compressionLevel);
  ctx->config_profileLevel = e->GetIntField(c, fid_profileLevel);

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
  ctx->chs = chs;
  ctx->freq = freq;

  ctx->pkt = AVPacket_New();

  const char *cformat = e->GetStringUTFChars(format, NULL);
  const char *cfile = e->GetStringUTFChars(file, NULL);

  jboolean ret = encoder_start(ctx, cformat, video_codec, audio_codec, cfile, NULL, NULL, NULL);

  e->ReleaseStringUTFChars(file, cfile);
  e->ReleaseStringUTFChars(format, cformat);

  return ret;
}

static jboolean encoder_addAudioFrame(FFContext *ctx, short *sams, int offset, int length)
{
  int nb_samples = length / ctx->chs;
  int buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, AV_SAMPLE_FMT_S16, 0);
  void* samples_data = (*_av_mallocz)(buffer_size);
  //copy sams -> samples_data
  memcpy(samples_data, sams + offset, length * 2);
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  int ret;

  if (ctx->swr_ctx != NULL) {
    //convert sample format (some codecs do not support S16)
    //sample rate is not changed
    ret = (*_av_samples_alloc)(ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->chs
      , nb_samples, ctx->audio_codec_ctx->sample_fmt, 0);
    if (ret < 0)
    {
      printf("MediaEncoder:av_samples_alloc() failed : %d\n", ret);
      return JNI_FALSE;
    }
    ctx->audio_src_data[0] = (uint8_t*)samples_data;
    int ret;
    ret = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, nb_samples
      , ctx->audio_src_data, nb_samples);
  } else {
    ctx->audio_dst_data[0] = (uint8_t*)samples_data;
  }
  (*_av_frame_make_writable)(ctx->audio_frame);  //ensure we can write to it now
  ctx->audio_frame->nb_samples = nb_samples;
  buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, ctx->audio_codec_ctx->sample_fmt, 0);
  ret = (*_avcodec_fill_audio_frame)(ctx->audio_frame, ctx->chs, ctx->audio_codec_ctx->sample_fmt, ctx->audio_dst_data[0], buffer_size, 0);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_fill_audio_frame() failed : %d\n", ret);
    return JNI_FALSE;
  }
  ctx->audio_frame->pts = ctx->audio_pts;  //(*_av_rescale_q)(ctx->audio_pts, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  ret = (*_avcodec_send_frame)(ctx->audio_codec_ctx, ctx->audio_frame);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_send_frame() failed : %d\n", ret);
    return JNI_FALSE;
  }
  while (1) {
    ret = (*_avcodec_receive_packet)(ctx->audio_codec_ctx, ctx->pkt);
    if (ret < 0) break;
    ctx->pkt->stream_index = ctx->audio_stream->index;
    (*_av_packet_rescale_ts)(ctx->pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
    ctx->last_dts = ctx->pkt->dts;
    ctx->last_pts = ctx->pkt->pts;
//    log_packet("audio", ctx->fmt_ctx, ctx->pkt);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);
    if (ret < 0) {
      printf("MediaEncoder:av_interleaved_write_frame() failed : %d\n", ret);
    }
  }
  (*_av_free)(samples_data);
  if (ctx->swr_ctx != NULL) {
    //free audio_dst_data (only the first pointer : regardless if format was plannar : it's alloced as one large block)
    if (ctx->audio_dst_data[0] != NULL) {
      (*_av_free)(ctx->audio_dst_data[0]);
      ctx->audio_dst_data[0] = NULL;
    }
  }
  ctx->audio_pts += nb_samples;
  return JNI_TRUE;
}

static jboolean encoder_addAudio(FFContext *ctx, short *sams, int offset, int length) {
  jboolean ok = JNI_TRUE;

  int frame_size = length;
  if (!ctx->audio_frame_size_variable) {
    frame_size = ctx->audio_frame_size;
    if (ctx->audio_buffer_size > 0) {
      //fill audio_buffer with input samples
      int size = ctx->audio_frame_size - ctx->audio_buffer_size;
      if (size > length) size = length;
      memcpy(ctx->audio_buffer + ctx->audio_buffer_size, sams + offset, size * 2);
      ctx->audio_buffer_size += size;
      if (ctx->audio_buffer_size < ctx->audio_frame_size) return JNI_TRUE;  //frame still not full
      encoder_addAudioFrame(ctx, ctx->audio_buffer, 0, ctx->audio_buffer_size);
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
      return JNI_TRUE;
    }
    if (!encoder_addAudioFrame(ctx, sams, offset, size)) {
      ok = JNI_FALSE;
      break;
    }
    offset += size;
    length -= size;
  }

  return ok;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addAudio
  (JNIEnv *e, jobject c, jshortArray sams, jint offset, jint length)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  if (ctx->audio_codec_ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jshort* sams_ptr = (jshort*)e->GetPrimitiveArrayCritical(sams, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = encoder_addAudio(ctx, sams_ptr, offset, length);

  e->ReleasePrimitiveArrayCritical(sams, sams_ptr, JNI_ABORT);

  return ok;
}

static jboolean encoder_addVideo(FFContext *ctx, int *px)
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
    printf("MediaEncoder:avcodec_send_frame() failed : %d\n", ret);
    return JNI_FALSE;
  }

  while (1) {
    ret = (*_avcodec_receive_packet)(ctx->video_codec_ctx, ctx->pkt);
    if (ret < 0) break;
    ctx->pkt->stream_index = ctx->video_stream->index;
    (*_av_packet_rescale_ts)(ctx->pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
    ctx->last_dts = ctx->pkt->dts;
    ctx->last_pts = ctx->pkt->pts;
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);
    if (ret < 0) {
      printf("MediaEncoder:av_interleaved_write_frame() failed : %d\n", ret);
    }
  }
  ctx->video_pts++;
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addVideo
  (JNIEnv *e, jobject c, jintArray px)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  if (ctx->video_codec_ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jint *px_ptr = (jint*)e->GetPrimitiveArrayCritical(px, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = encoder_addVideo(ctx, (int*)px_ptr);

  e->ReleasePrimitiveArrayCritical(px, px_ptr, JNI_ABORT);

  return ok;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaEncoder_getLastDTS
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  return ctx->last_dts;
}

JNIEXPORT jlong JNICALL Java_javaforce_media_MediaEncoder_getLastPTS
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  return ctx->last_pts;
}

static jboolean encoder_addAudioEncoded(FFContext *ctx, jbyte* data, jint size, jboolean ts, jlong dts, jlong pts) {
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  ctx->pkt->stream_index = ctx->audio_stream->index;

  if (ts) {
    ctx->pkt->dts = dts;
    ctx->pkt->pts = pts;
  } else {
    ctx->pkt->pts = ctx->audio_pts;  //(*_av_rescale_q)(ctx->audio_pts, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
    ctx->pkt->dts = ctx->pkt->pts;
    (*_av_packet_rescale_ts)(ctx->pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  }

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);
  ctx->audio_pts++;
  return ret == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addAudioEncoded
  (JNIEnv *e, jobject c, jbyteArray ba, jint offset, jint length)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jbyte *ba_ptr = (jbyte*)e->GetPrimitiveArrayCritical(ba, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = encoder_addAudioEncoded(ctx, ba_ptr + offset, length, JNI_FALSE, 0, 0);

  e->ReleasePrimitiveArrayCritical(ba, ba_ptr, JNI_ABORT);

  return ok;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addAudioEncodedTS
  (JNIEnv *e, jobject c, jbyteArray ba, jint offset, jint length, jlong dts, jlong pts)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jbyte *ba_ptr = (jbyte*)e->GetPrimitiveArrayCritical(ba, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = encoder_addAudioEncoded(ctx, ba_ptr + offset, length, JNI_TRUE, dts, pts);

  e->ReleasePrimitiveArrayCritical(ba, ba_ptr, JNI_ABORT);

  return ok;
}

static jboolean encoder_addVideoEncoded(FFContext *ctx, jbyte* data, jint size, jboolean key_frame, jboolean ts, jlong dts, jlong pts) {
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = (uint8_t*)data;
  ctx->pkt->size = size;

  ctx->pkt->stream_index = ctx->video_stream->index;
  if (key_frame) {
    ctx->pkt->flags = AV_PKT_FLAG_KEY;
  }

  if (ts) {
    ctx->pkt->dts = dts;
    ctx->pkt->pts = pts;
  } else {
    ctx->pkt->pts = ctx->video_pts;  //(*_av_rescale_q)(ctx->video_pts, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
    ctx->pkt->dts = ctx->pkt->pts;
    (*_av_packet_rescale_ts)(ctx->pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
//    printf("packet:%lld/%lld/%lld\n", ctx->pkt->dts, ctx->pkt->pts, ctx->pkt->duration);
  }

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);
  ctx->video_pts++;
  return ret == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addVideoEncoded
  (JNIEnv *e, jobject c, jbyteArray ba, jint offset, jint length, jboolean key_frame)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jbyte *ba_ptr = (jbyte*)e->GetPrimitiveArrayCritical(ba, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = encoder_addVideoEncoded(ctx, ba_ptr + offset, length, key_frame, JNI_FALSE, 0, 0);

  e->ReleasePrimitiveArrayCritical(ba, ba_ptr, JNI_ABORT);

  return ok;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addVideoEncodedTS
  (JNIEnv *e, jobject c, jbyteArray ba, jint offset, jint length, jboolean key_frame, jlong dts, jlong pts)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jbyte *ba_ptr = (jbyte*)e->GetPrimitiveArrayCritical(ba, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = encoder_addVideoEncoded(ctx, ba_ptr + offset, length, key_frame, JNI_TRUE, dts, pts);

  e->ReleasePrimitiveArrayCritical(ba, ba_ptr, JNI_ABORT);

  return ok;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaEncoder_getAudioFramesize
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->frame_size;
}

static jboolean encoder_flush(FFContext *ctx, AVCodecContext *codec_ctx, AVStream *stream, jboolean endOfStream) {
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;

  int ret;

  //signal end of input
  if (endOfStream) {
    ret = (*_avcodec_send_frame)(codec_ctx, NULL);
    if (ret < 0) {
      printf("MediaEncoder:avcodec_send_frame() failed : %d\n", ret);
      return JNI_FALSE;
    }
  }
  while (1) {
    ret = (*_avcodec_receive_packet)(codec_ctx, ctx->pkt);
    if (ret < 0) break;
    ctx->pkt->stream_index = stream->index;
    (*_av_packet_rescale_ts)(ctx->pkt, codec_ctx->time_base, stream->time_base);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, ctx->pkt);
    if (ret < 0) {
      printf("MediaEncoder:av_interleaved_write_frame() failed : %d\n", ret);
    }
  }
  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaEncoder_flush
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return;
  if (ctx->audio_stream != NULL) {
    encoder_flush(ctx, ctx->audio_codec_ctx, ctx->audio_stream, JNI_FALSE);
  }
  if (ctx->video_stream != NULL) {
    encoder_flush(ctx, ctx->video_codec_ctx, ctx->video_stream, JNI_FALSE);
  }
  if (ctx->io_ctx != NULL) {
    (*_avio_flush)(ctx->io_ctx);
    if (ctx->is_dash) {
//      (*_avio_flush)(ctx->io_ctx_dash);
    }
  }
}

static void encoder_stop(FFContext *ctx)
{
  if (ff_debug_trace) printf("encoder_stop\n");
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
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaEncoder_stop
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return;
  encoder_stop(ctx);
  deleteFFContext(e,c,ctx);
}
