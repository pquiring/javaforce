//decoder codebase

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
