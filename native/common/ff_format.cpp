//MediaFormat native methods

jint getVideoStream(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->video_stream->index;
}

jint getAudioStream(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->audio_stream->index;
}

jint getVideoCodecID(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->video_stream->codecpar->codec_id;
}

jint getAudioCodecID(FFContext *ctx)
{
  if (ctx == NULL) return -1;
  return ctx->audio_stream->codecpar->codec_id;
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
