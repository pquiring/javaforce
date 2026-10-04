//MediaFormat native methods

jint getVideoStream(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  if (ctx->video_stream == NULL) return -1;
  return ctx->video_stream->index;
}

jint getAudioStream(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  if (ctx->audio_stream == NULL) return -1;
  return ctx->audio_stream->index;
}

jint getVideoCodecID(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  if (ctx->video_stream == NULL) return -1;
  if (ctx->video_stream->codecpar == NULL) return -1;
  return conv_av_video_codec_id(ctx->video_stream->codecpar->codec_id);
}

jint getAudioCodecID(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  if (ctx->audio_stream == NULL) return -1;
  if (ctx->audio_stream->codecpar == NULL) return -1;
  return conv_av_audio_codec_id(ctx->audio_stream->codecpar->codec_id);
}

jint getVideoBitRate(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->bit_rate;
}

jint getAudioBitRate(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->bit_rate;
}

char* getVideoMimeType(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  if (ctx->video_stream == NULL) return 0;
  AVBPrint print;
  (*_av_bprint_init)(&print, 0, AV_BPRINT_SIZE_AUTOMATIC);
  if ((*_av_mime_codec_str)(ctx->video_stream->codecpar, ctx->audio_codec_ctx->framerate, &print) < 0) {
    ctx->video_mime[0] = 0;
  } else {
//    printf("video_codec=%s\n", print.str);
    strcpy(ctx->video_mime, print.str);
  }
  (*_av_bprint_finalize)(&print, NULL);
  return ctx->video_mime;
}

char* getAudioMimeType(FFContext *ctx)
{
  if (ctx == NULL) return 0;
  if (ctx->audio_codec_ctx == NULL) return 0;
  if (ctx->audio_stream == NULL) return 0;
  AVBPrint print;
  (*_av_bprint_init)(&print, 0, AV_BPRINT_SIZE_AUTOMATIC);
  if ((*_av_mime_codec_str)(ctx->audio_stream->codecpar, ctx->audio_codec_ctx->framerate, &print) < 0) {
    ctx->audio_mime[0] = 0;
  } else {
//    printf("audio_codec=%s\n", print.str);
    strcpy(ctx->audio_mime, print.str);
  }
  (*_av_bprint_finalize)(&print, NULL);
  return ctx->audio_mime;
}
