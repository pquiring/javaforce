//MediaInput

//returns stream idx >= 0
static int decoder_open_codec_context(FFContext *ctx, AVFormatContext *fmt_ctx, int type)
{
  int ret;
  int stream_idx;
  AVStream *stream;
  AVCodec *codec;
  stream_idx = (*_av_find_best_stream)(ctx->fmt_ctx, type, -1, -1, NULL, 0);
  if (stream_idx < 0) {
    printf("MediaDecoder:av_find_best_stream() failed : %d\n", stream_idx);
    return -1;
  }
  stream = (AVStream*)ctx->fmt_ctx->streams[stream_idx];
  codec = (*_avcodec_find_decoder)(stream->codecpar->codec_id);
  if (codec == NULL) {
    printf("MediaDecoder:avcodec_find_decoder() failed\n");
    return -1;
  }
  ctx->codec_ctx = (*_avcodec_alloc_context3)(codec);
  (*_avcodec_parameters_to_context)(ctx->codec_ctx, stream->codecpar);
//    ctx->codec_ctx->flags |= AV_CODEC_FLAG_LOW_DELAY;
  ret = (*_avcodec_open2)(ctx->codec_ctx, codec, NULL);
  if (ret < 0) {
    printf("MediaDecoder:avcodec_open2() failed : %d\n", ret);
    return -1;
  }
  return stream_idx;
}

static jboolean decoder_open_video_codec(FFContext *ctx, int new_width, int new_height) {
  if (new_width == -1) new_width = ctx->video_codec_ctx->width;
  if (new_height == -1) new_height = ctx->video_codec_ctx->height;
  ctx->width = new_width;
  ctx->height = new_height;
  ctx->video_dst_bufsize = (*_av_image_alloc)(ctx->video_dst_data, ctx->video_dst_linesize
    , ctx->video_codec_ctx->width, ctx->video_codec_ctx->height
    , ctx->video_codec_ctx->pix_fmt, 1);
  if (ctx->video_dst_bufsize < 0) {
    printf("MediaDecoder:av_image_alloc() failed : %d\n", ctx->video_dst_bufsize);
    return JNI_FALSE;
  }
  ctx->rgb_video_dst_bufsize = (*_av_image_alloc)(ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize
    , new_width, new_height
    , AV_PIX_FMT_BGRA, 1);
  if (ctx->rgb_video_dst_bufsize < 0)
  {
    printf("MediaDecoder:av_image_alloc() failed : %d\n", ctx->rgb_video_dst_bufsize);
    return JNI_FALSE;
  }
  if (ctx->e != NULL) {
    ctx->jvideo_length = ctx->rgb_video_dst_bufsize/4;
    ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));
  }
  //create video conversion context
  ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
    , new_width, new_height, AV_PIX_FMT_BGRA
    , SWS_BILINEAR, NULL, NULL, NULL);

  return JNI_TRUE;
}

static jboolean decoder_open_audio_codec(FFContext *ctx, int new_chs, int new_freq) {
  //create audio conversion context
  ctx->swr_ctx = (*_swr_alloc)();
  if (new_chs == -1) new_chs = ctx->audio_codec_ctx->ch_layout.nb_channels;
  ctx->chs = new_chs;
  AVChannelLayout new_layout;
  switch (new_chs) {
    case 1: (*_av_channel_layout_copy)(&new_layout, &channel_layout_1); ctx->dst_nb_channels = 1; break;
    case 2: (*_av_channel_layout_copy)(&new_layout, &channel_layout_2); ctx->dst_nb_channels = 2; break;
    case 4: (*_av_channel_layout_copy)(&new_layout, &channel_layout_4); ctx->dst_nb_channels = 4; break;
    default: printf("MediaDecoder:unknown channels:%d\n", new_chs); return JNI_FALSE;
  }
  AVChannelLayout src_layout;
  (*_av_channel_layout_copy)(&src_layout, &ctx->audio_codec_ctx->ch_layout);
  ctx->dst_sample_fmt = AV_SAMPLE_FMT_S16;
  ctx->src_rate = ctx->audio_codec_ctx->sample_rate;
  if (new_freq == -1) new_freq = ctx->src_rate;
  ctx->freq = new_freq;
  (*_swr_alloc_set_opts2)(&ctx->swr_ctx,
    &new_layout, ctx->dst_sample_fmt, new_freq,
    &src_layout, ctx->audio_codec_ctx->sample_fmt, ctx->src_rate,
    0, NULL);

  int ret;
  ret = (*_swr_init)(ctx->swr_ctx);
  if (ret < 0) {
    printf("MediaDecoder:resample init failed : %d\n", ret);
  }
  ctx->dst_rate = new_freq;

  return JNI_TRUE;
}

static jboolean decoder_alloc_frame(FFContext *ctx) {
  if ((ctx->frame = (*_av_frame_alloc)()) == NULL) {
    printf("MediaDecoder:av_frame_alloc() failed\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

static jboolean decoder_alloc_packet(FFContext *ctx) {
  ctx->pkt = AVPacket_New();
  return JNI_TRUE;
}

void input_open_streams(FFContext *ctx) {
  ctx->video_stream_idx = decoder_open_codec_context(ctx, ctx->fmt_ctx, AVMEDIA_TYPE_VIDEO);
  if (ctx->video_stream_idx < 0) {
    printf("MediaDecoder:input_open_streams() : no video stream found\n");
  } else {
    ctx->video_stream = (AVStream*)ctx->fmt_ctx->streams[ctx->video_stream_idx];
    ctx->video_codec_ctx = ctx->codec_ctx;
    ctx->codec_ctx = NULL;
  }

  ctx->audio_stream_idx = decoder_open_codec_context(ctx, ctx->fmt_ctx, AVMEDIA_TYPE_AUDIO);
  if (ctx->audio_stream_idx < 0) {
    printf("MediaDecoder:input_open_streams() : no audio stream found\n");
  } else {
    ctx->audio_stream = (AVStream*)ctx->fmt_ctx->streams[ctx->audio_stream_idx];
    ctx->audio_codec_ctx = ctx->codec_ctx;
    ctx->codec_ctx = NULL;
  }
}

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

  input_open_streams(ctx);

  if (ff_debug_log) printf("dump_format\n");
  (*_av_dump_format)(ctx->fmt_ctx, 0, "file_io", 0);

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

  if (ff_debug_log) printf("inputOpenFile:ctx=%p\n", ctx);
  return ctx;
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

  input_open_streams(ctx);

  if (ff_debug_log) printf("dump_format\n");
  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);

  decoder_alloc_frame(ctx);
  decoder_alloc_packet(ctx);

  return JNI_TRUE;
}

FFContext* inputOpenIO(MediaIO* mio)
{
  FFContext *ctx = newFFContext(NULL,NULL);
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = inputOpenIO_ctx(ctx);

  ctx->FFMClearMediaIO();

  if (!res) {
    freeFFContext(NULL,NULL,ctx);
    ctx = NULL;
  }

  if (ff_debug_log) printf("inputOpenIO:ctx=%p\n", ctx);
  return ctx;
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

jboolean inputClose(FFContext *ctx, MediaIO *mio) {
  if (ctx == NULL) return JNI_FALSE;
  ctx->FFMCopyMediaIO(mio);

  jboolean res = inputClose_ctx(NULL, NULL, ctx);

  ctx->FFMClearMediaIO();

  return res;
}

jboolean inputOpenVideo_ctx(FFContext* ctx, jint new_width, jint new_height)
{
  if (ctx == NULL) return JNI_FALSE;

  decoder_open_video_codec(ctx, new_width, new_height);

  if (ctx->decode_buffer == NULL) {
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
    ctx->decode_buffer_size = 1024*1024;
  }

  return JNI_TRUE;
}

jboolean inputOpenVideo(FFContext* ctx, MediaIO *mio, jint new_width, jint new_height)
{
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = inputOpenVideo_ctx(ctx, new_width, new_height);

  ctx->FFMClearMediaIO();

  return res;
}

jboolean inputOpenAudio_ctx(FFContext* ctx, jint new_chs, jint new_freq)
{
  if (ctx == NULL) return JNI_FALSE;

  decoder_open_audio_codec(ctx, new_chs, new_freq);

  if (ctx->decode_buffer == NULL) {
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
    ctx->decode_buffer_size = 1024*1024;
  }

  return JNI_TRUE;
}

jboolean inputOpenAudio(FFContext* ctx, MediaIO *mio, jint new_chs, jint new_freq)
{
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = inputOpenAudio_ctx(ctx, new_chs, new_freq);

  ctx->FFMClearMediaIO();

  return res;
}

jlong getDuration(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->fmt_ctx == NULL) return 0;
  if (ctx->fmt_ctx->duration << 1 == 0) return 0;  //0x8000000000000000
  return ctx->fmt_ctx->duration / AV_TIME_BASE;  //return in seconds
}

jint getVideoWidth(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

jint getVideoHeight(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
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

jint getVideoKeyFrameInterval(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  if (ctx->video_codec_ctx == NULL) return -1;
  return ctx->video_codec_ctx->gop_size;
}

jint getAudioChannels(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  return ctx->audio_codec_ctx->ch_layout.nb_channels;
}

jint getAudioSampleRate(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

jint inputRead_ctx(FFContext* ctx)
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

jint inputRead(FFContext* ctx, MediaIO* mio)
{
  if (ctx == NULL) return 0;

  ctx->FFMCopyMediaIO(mio);

  jint res = inputRead_ctx(ctx);

  ctx->FFMClearMediaIO();

  return res;
}

jboolean getPacketKeyFrame(FFContext* ctx)
{
  if (ctx == NULL) return JNI_FALSE;

  return ctx->pkt_key_frame;
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

jboolean inputSeek_ctx(FFContext* ctx, jlong seconds)
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

jboolean inputSeek(FFContext* ctx, MediaIO* mio, jlong seconds)
{
  if (ctx == NULL) return JNI_FALSE;

  ctx->FFMCopyMediaIO(mio);

  jboolean res = inputSeek_ctx(ctx, seconds);

  ctx->FFMClearMediaIO();

  return res;
}
