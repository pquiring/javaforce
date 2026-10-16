//MediaOutput

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

static int indexOf(const char*str, char ch) {
  int idx = 0;
  while (*str != 0) {
    if (*str == ch) return idx;
    idx++;
    str++;
  }
  return -1;
}

static jboolean encoder_init_video(FFContext *ctx) {
  printf("encoder_init_video:codec_ctx=%p:codec=%p:stream=%p\n", ctx->video_codec_ctx, ctx->video_codec, ctx->video_stream);

  ctx->video_codec_ctx->bit_rate = ctx->config_video_bit_rate;
  ctx->video_codec_ctx->width = ctx->width;
  ctx->video_codec_ctx->height = ctx->height;
  if (ctx->config_fps_1000_1001) {
    ctx->video_codec_ctx->time_base.num = 1000;
    ctx->video_codec_ctx->time_base.den = ctx->fps * 1001;
    if (ctx->video_stream != NULL) {
      ctx->video_stream->time_base.num = 1000;
      ctx->video_stream->time_base.den = ctx->fps * 1001;
    }
    ctx->video_codec_ctx->framerate.num = ctx->fps * 1001;
    ctx->video_codec_ctx->framerate.den = 1000;
  } else {
    ctx->video_codec_ctx->time_base.num = 1;
    ctx->video_codec_ctx->time_base.den = ctx->fps;
    if (ctx->video_stream != NULL) {
      ctx->video_stream->time_base.num = 1;
      ctx->video_stream->time_base.den = ctx->fps * 1024;
    }
    ctx->video_codec_ctx->framerate.num = ctx->fps;
    ctx->video_codec_ctx->framerate.den = 1;
  }
  ctx->video_codec_ctx->gop_size = ctx->config_gop_size;
  ctx->video_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
  ctx->video_codec_ctx->qmin = 2;
  ctx->video_codec_ctx->qmax = 40;

  if (ff_debug_trace) printf("encoder_init_video\n");

  //set video codec options
  AVDictionary *opts = NULL;
  for(int a=0;a<ctx->nOpts;a++) {
    char key[64];
    char value[64];
    const char* kv = ctx->opts[a];
    int idx = indexOf(kv, '=');
    if (idx == -1) continue;
    memcpy(key, kv, idx);
    key[idx] = 0;
    strcpy(value, kv + idx + 1);
    (*_av_opt_set)(ctx->video_codec_ctx, key, value, 0);
  }

  if (ff_debug_trace) printf("encoder_init_video:open:%p:%p\n", ctx->video_codec_ctx, ctx->video_codec);
  //open video codec
  int ret = (*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, &opts);
  (*_av_dict_free)(&opts);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_open2() failed : %d\n", ret);
    return JNI_FALSE;
  }

  if (ctx->video_stream != NULL) {
    if (ff_debug_trace) printf("encoder_init_video:params:%p:%p\n", ctx->video_stream->codecpar, ctx->video_codec_ctx);
    //copy params (after codec is opened)
    ret = (*_avcodec_parameters_from_context)(ctx->video_stream->codecpar, ctx->video_codec_ctx);
    if (ret < 0) {
      printf("MediaEncoder:avcodec_parameters_from_context() failed : %d\n", ret);
      return JNI_FALSE;
    }
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

static AVSampleFormat* get_codec_sample_fmts(AVCodecContext *codec_ctx, AVCodec *codec) {
  AVSampleFormat *fmts;
  (*_avcodec_get_supported_config)(codec_ctx, codec, AV_CODEC_CONFIG_SAMPLE_FORMAT, 0, (void**)&fmts, NULL);
  return fmts;
}

static jboolean encoder_init_audio(FFContext *ctx) {
  printf("encoder_init_audio:codec_ctx=%p:codec=%p:stream=%p\n", ctx->audio_codec_ctx, ctx->audio_codec, ctx->audio_stream);

  AVSampleFormat* sample_fmts = get_codec_sample_fmts(ctx->audio_codec_ctx, ctx->audio_codec);
  if (sample_fmts != NULL) {
    bool have_fmt = false;
    for(int idx=0;;idx++) {
      AVSampleFormat fmt = sample_fmts[idx];
      if (fmt == -1) {
        if (!have_fmt) {
          ctx->audio_codec_ctx->sample_fmt = sample_fmts[0];
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

  //set default audio codec options
  switch (ctx->audio_codec_ctx->codec_id) {
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
  }

  //set audio codec options
  AVDictionary *opts = NULL;
  for(int a=0;a<ctx->nOpts;a++) {
    char key[64];
    char value[64];
    const char* kv = ctx->opts[a];
    int idx = indexOf(kv, '=');
    if (idx == -1) continue;
    memcpy(key, kv, idx);
    key[idx] = 0;
    strcpy(value, kv + idx + 1);
    (*_av_opt_set)(ctx->video_codec_ctx, key, value, 0);
  }

  //open audio codec
  if (ff_debug_log) printf("avcodec_open2\n");
  int ret = (*_avcodec_open2)(ctx->audio_codec_ctx, ctx->audio_codec, &opts);
  (*_av_dict_free)(&opts);
  if (ret < 0) {
    printf("MediaEncoder:avcodec_open2() failed : %d\n", ret);
    return JNI_FALSE;
  }

  if (ctx->audio_codec_ctx->frame_size == 0) {
    ctx->audio_codec_ctx->frame_size = 160;
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

  ctx->FFMCopyMediaIO(mio);

  if (!outputCreateIO_ctx(ctx, format)) {
    freeFFContext(NULL,NULL,ctx);
    ctx = NULL;
  }

  ctx->FFMClearMediaIO();

  return ctx;
}

jint addVideoStream_ctx(FFContext *ctx, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval, jint nOpts, const char** opts)
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
  ctx->nOpts = nOpts;
  ctx->opts = opts;

  if (!encoder_init_video(ctx)) {
    printf("MediaOutput:encoder_init_video() failed\n");
    return -1;
  }

  return ctx->video_stream->index;
}

jint addVideoStream(FFContext *ctx, MediaIO* mio, jint codec_id, jint bit_rate, jint width, jint height, jfloat fps, jint keyFrameInterval, jint nOpts, const char** opts)
{
  if (ctx == NULL) return -1;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = addVideoStream_ctx(ctx, conv_video_codec_id(codec_id), bit_rate, width, height, fps, keyFrameInterval, nOpts, opts);

  ctx->FFMClearMediaIO();

  return res;
}

jint addAudioStream_ctx(FFContext* ctx, jint codec_id, jint bit_rate, jint chs, jint freq, jint nOpts, const char** opts)
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
  ctx->nOpts = nOpts;
  ctx->opts = opts;

  if (!encoder_init_audio(ctx)) {
    printf("MediaOutput:encoder_init_audio() failed\n");
    return -1;
  }

  return ctx->audio_stream->index;
}

jint addAudioStream(FFContext* ctx, MediaIO* mio, jint codec_id, jint bit_rate, jint chs, jint freq, jint nOpts, const char** opts)
{
  if (ctx == NULL) return -1;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = addAudioStream_ctx(ctx, conv_audio_codec_id(codec_id), bit_rate, chs, freq, nOpts, opts);

  ctx->FFMClearMediaIO();

  return res;
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

jboolean outputClose(FFContext* ctx, MediaIO* mio)
{
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = outputClose_ctx(NULL,NULL,ctx);

  ctx->FFMClearMediaIO();

  return res;
}

jboolean writeHeader_ctx(FFContext* ctx)
{
  if (ctx == NULL) return JNI_FALSE;

  printf("write header:fmt_ctx=%p\n", ctx->fmt_ctx);

  int ret = (*_avformat_write_header)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("MediaOutput:avformat_write_header failed : %d\n", ret);
  }
  if (ff_debug_trace) printf("MediaOutput:writeHeader=%d\n", ret);
  return ret >= 0;
}

jboolean writeHeader(FFContext* ctx, MediaIO* mio)
{
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = writeHeader_ctx(ctx);

  ctx->FFMClearMediaIO();

  return res;
}

jboolean writePacket_ctx(FFContext* ctx, jint stream, jbyte* data, jint offset, jint length, jboolean keyFrame)
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

jboolean writePacket(FFContext* ctx, MediaIO* mio, jint stream, jbyte* data, jint offset, jint length, jboolean keyFrame)
{
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = writePacket_ctx(ctx, stream, data, offset, length, keyFrame);

  ctx->FFMClearMediaIO();

  return res;
}
