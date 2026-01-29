//ffmpeg struct context

struct FFContext {
  JNIEnv *e;  //only valid during native function
  jobject c;  //only valid during native function

  //MediaIO (these can be cached since the MediaIO object should not be GCed)
  jclass cls_mio;
  jmethodID mid_ff_read;
  jmethodID mid_ff_write;
  jmethodID mid_ff_seek;

  //FFM upcalls
  jint (*readFFM)(jbyte* ptr, jint size);
  jint (*writeFFM)(jbyte* ptr, jint size);
  jlong (*seekFFM)(jlong where, jint how);

  //decoder fields
  //alloc:MediaInput.open() MediaOutput.create() free:freeFFContext()
  jobject mio;
  //alloc:MediaInput.openFile(),openIO(),MediaOutput.createFile(),createIO() free:MediaInput.close(),MediaOutput.close()
  AVFormatContext *fmt_ctx;
  //alloc:MediaInput.openIO(),MediaOutput.createFile(),createIO() free:MediaInput.close(),MediaOutput.close()
  AVIOContext *io_ctx;
  bool io_file;

  //usage:MediaInput.openFile() [no alloc, no free]
  AVInputFormat* input_fmt;

  //alloc:decoder_open_codec_context() transfer_to:video_codec_ctx or audio_codec_ctx
  AVCodecContext *codec_ctx;

  int video_stream_idx;
  //decoder:copy_from:fmt_ctx free:with fmt_ctx
  //encoder:alloc:encoder_add_stream() free:with fmt_ctx
  AVStream *video_stream;
  //transfer_from:decoder_open_codec_context() free:MediaInput.close(),MediaOutput.close()
  AVCodecContext *video_codec_ctx;
  //alloc:MediaVideoDecoder.decode(),encoder_init_video() free:MediaVideoDecoder.stop(),encoder_stop(),MediaVideoEncoder.stop()
  void* sws_ctx;
  //alloc:MediaInput.openVideo()openAudio(),MediaAudio/VideoDecoder.start() free:MediaInput.close(),MediaAudio/VideoDecoder.stop()
  uint8_t* decode_buffer;
  int decode_buffer_size;
  //compressed video
  int video_dst_bufsize;
  //decoder:alloc:decoder_open_video_codec() free:MediaInput.close(),MediaVideoDecoder.stop(),MediaDecoder.stop()
  uint8_t* video_dst_data[4];
  int video_dst_linesize[4];
  //rgb video
  int rgb_video_dst_bufsize;
  //decoder:alloc:decoder_open_video_codec() free:MediaInput.close(),MediaVideoDecoder.stop(),MediaDecoder.stop()
  uint8_t* rgb_video_dst_data[4];
  int rgb_video_dst_linesize[4];

  int audio_stream_idx;
  //decoder:copy_from:fmt_ctx free:with fmt_ctx
  //encoder:alloc:encoder_add_stream() free:with fmt_ctx
  AVStream *audio_stream;
  //transfer_from:decoder_open_codec_context() free:MediaInput.close(),MediaOutput.close()
  AVCodecContext *audio_codec_ctx;
  void* swr_ctx;

  //compressed audio
  int src_rate;
  //user audio
  int dst_rate;
  int dst_nb_channels;
  int dst_sample_fmt;
  //alloc/free: MediaAudioDecoder.decode()
  //alloc/free: av_encoder_addAudioFrame()
  //alloc/free: encoder_addAudioFrame()
  uint8_t* audio_dst_data[4];
  int audio_dst_linesize[4];

  //alloc:decoder_alloc_frame(),MediaAudioDecoder.start(),MediaVideoDecoder.start(),MediaOutput.createFile(),createIO(),MediaAudioEncoder.start(),MediaVideoEncoder.start()
  //free:*.close(),*.stop()
  //usage:Media*Decoder=temp MediaOutput:temp Media*Encoder=temp
  AVPacket *pkt;
  jboolean pkt_key_frame;

  //deceder:alloc:decoder_alloc_frame(),Media*Decoder.start() free:MediaInput.close(),Media*Decoder.stop()
  AVFrame *frame;

  jintArray jvideo;
  int jvideo_length;

  jshortArray jaudio;
  int jaudio_length;

  //additional raw video decoder fields
  //reference only
  AVCodec *video_codec;

  //encoder fields
  //reference only
  AVCodec *audio_codec;
  //reference only (from fmt_ctx)
  AVOutputFormat *out_fmt;
  int width, height, fps;
  int chs, freq;
  jboolean scaleVideo;
  int org_width, org_height;
  //alloc:encoder_init_audio/video free:encoder_stop(),MediaOutput.close(),Media*Encoder.stop()
  AVFrame *audio_frame, *video_frame;
  //alloc:encoder_init_video() free:encoder_stop(), MediaOutput.close()
  AVFrame *src_pic;
  jboolean audio_frame_size_variable;
  jboolean video_delay;
  int audio_frame_size;
  //alloc:encoder_init_audio() free:encoder_stop(), MediaOutput.close()
  short *audio_buffer;
  int audio_buffer_size;

  int64_t audio_pts;
  int64_t video_pts;

  int64_t last_dts;
  int64_t last_pts;
  int64_t last_duration;

  //temp usage
  uint8_t* audio_src_data[4];

  jboolean is_dash;
  jboolean is_mp4;

  /** Set to make fps = fps * 1000 / 1001. */
  jboolean config_fps_1000_1001;

  /** Number of frames per group of pictures (GOP).
   * Determines how often key frame is generated.
   * Default = 12
   */
  int config_gop_size;

  /** Video bit rate.
   * Default = 400000
   */
  int config_video_bit_rate;

  /** Audio bit rate.
   * Default = 128000
   */
  int config_audio_bit_rate;

  int config_compressionLevel;

  /** ProfileLevel (1=baseline 2=main 3=high) */
  int config_profileLevel;

  void GetMediaIO() {
    cls_mio = e->GetObjectClass(mio);
    mid_ff_read = e->GetMethodID(cls_mio, "read", "(Ljavaforce/media/MediaCoder;[B)I");
    mid_ff_write = e->GetMethodID(cls_mio, "write", "(Ljavaforce/media/MediaCoder;[B)I");
    mid_ff_seek = e->GetMethodID(cls_mio, "seek", "(Ljavaforce/media/MediaCoder;JI)J");
  }

  char errmsg[256];

  char* error_string(int errnum) {
    (*_av_strerror)(errnum, errmsg, 256);
    return errmsg;
  }
};

//check for unfreed resources
void ff_check(FFContext *ctx) {
  if (ctx->fmt_ctx != NULL) printf("Warning:FF:fmt_ctx not freed!\n");
  if (ctx->io_ctx != NULL) printf("Warning:FF:io_ctx not freed!\n");
//  if (ctx->video_stream != NULL) printf("Warning:FF:video_stream not freed!\n");
  if (ctx->video_codec_ctx != NULL) printf("Warning:FF:video_codec_ctx not freed!\n");
  if (ctx->sws_ctx != NULL) printf("Warning:FF:sws_ctx not freed!\n");
  if (ctx->decode_buffer != NULL) printf("Warning:FF:decode_buffer not freed!\n");
  if (ctx->video_dst_data[0] != NULL) printf("Warning:FF:video_dst_data[0] not freed!\n");
  if (ctx->rgb_video_dst_data[0] != NULL) printf("Warning:FF:rgb_video_dst_data[0] not freed!\n");
//  if (ctx->audio_stream != NULL) printf("Warning:FF:audio_stream not freed!\n");
  if (ctx->audio_codec_ctx != NULL) printf("Warning:FF:audio_codec_ctx not freed!\n");
  if (ctx->audio_dst_data[0] != NULL) printf("Warning:FF:audio_dst_data[0] not freed!\n");
  if (ctx->frame != NULL) printf("Warning:FF:frame not freed!\n");
  if (ctx->audio_frame != NULL) printf("Warning:FF:audio_frame not freed!\n");
  if (ctx->video_frame != NULL) printf("Warning:FF:video_frame not freed!\n");
  if (ctx->src_pic != NULL) printf("Warning:FF:src_pic not freed!\n");
  if (ctx->audio_buffer != NULL) printf("Warning:FF:audio_buffer not freed!\n");
  if (ctx->audio_src_data[0] != NULL) printf("Warning:FF:audio_src_data[0] not freed!\n");
  if (ctx->sws_ctx != NULL) printf("Warning:FF:sws_ctx not freed!\n");
  if (ctx->sws_ctx != NULL) printf("Warning:FF:sws_ctx not freed!\n");
  if (ctx->sws_ctx != NULL) printf("Warning:FF:sws_ctx not freed!\n");
  if (ctx->sws_ctx != NULL) printf("Warning:FF:sws_ctx not freed!\n");
  if (ctx->sws_ctx != NULL) printf("Warning:FF:sws_ctx not freed!\n");
}

#define ffiobufsiz (64 * 1024)

//reflection ctx

FFContext* createFFContext(JNIEnv *e, jobject c) {
  if (!ffmpeg_loaded) {
    printf("MediaCoder.init() not called!\n");
    return NULL;
  }
  FFContext *ctx;
  jclass cls_coder = e->FindClass("javaforce/media/MediaCoder");
  jfieldID fid_ff_ctx = e->GetFieldID(cls_coder, "ctx", "J");
  ctx = (FFContext*)e->GetLongField(c, fid_ff_ctx);
  if (ctx != NULL) {
    printf("MediaCoder used twice\n");
    return NULL;
  }
  ctx = (FFContext*)(*_av_mallocz)(sizeof(FFContext));
  e->SetLongField(c,fid_ff_ctx,(jlong)ctx);
  ctx->e = e;
  ctx->c = c;
  return ctx;
}

FFContext* getFFContext(JNIEnv *e, jobject c) {
  FFContext *ctx;
  jclass cls_coder = e->FindClass("javaforce/media/MediaCoder");
  jfieldID fid_ff_ctx = e->GetFieldID(cls_coder, "ctx", "J");
  ctx = (FFContext*)e->GetLongField(c, fid_ff_ctx);
  if (ctx == NULL) return NULL;
  ctx->e = e;
  ctx->c = c;
  return ctx;
}

void deleteFFContext(JNIEnv *e, jobject c, FFContext *ctx) {
  if (ctx == NULL) return;
  if (ctx->mio != NULL) {
    e->DeleteGlobalRef(ctx->mio);
    ctx->mio = NULL;
  }
  ff_check(ctx);
  (*_av_free)(ctx);
  jclass cls_coder = e->FindClass("javaforce/media/MediaCoder");
  jfieldID fid_ff_ctx = e->GetFieldID(cls_coder, "ctx", "J");
  e->SetLongField(c,fid_ff_ctx,0);
}

//jlong ctx

FFContext* newFFContext(JNIEnv *e, jobject c) {
  if (!ffmpeg_loaded) {
    printf("MediaCoder.init() not called!\n");
    return NULL;
  }
  FFContext *ctx;
  ctx = (FFContext*)(*_av_mallocz)(sizeof(FFContext));
  ctx->e = e;
  ctx->c = c;
  return ctx;
}

FFContext* castFFContext(JNIEnv *e, jobject c, jlong ctxptr) {
  if (ctxptr == 0) return NULL;
  FFContext* ctx = (FFContext*)ctxptr;
  ctx->e = e;
  ctx->c = c;
  return ctx;
}

void freeFFContext(JNIEnv *e, jobject c, FFContext *ctx) {
  if (ctx == NULL) return;
  if (ctx->mio != NULL) {
    e->DeleteGlobalRef(ctx->mio);
    ctx->mio = NULL;
  }
  ff_check(ctx);
  (*_av_free)(ctx);
}
