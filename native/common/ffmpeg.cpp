//FFMPEG : Compatible with ffmpeg.org and libav.org

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/channel_layout.h>
#include <libavutil/mathematics.h>
#include <libswscale/swscale.h>

#include <chrono>

//returned by Decoder.read()
#define END_FRAME -1
#define NULL_FRAME 0  //could be metadata frame
#define AUDIO_FRAME 1
#define VIDEO_FRAME 2

static jboolean libav_org = JNI_FALSE;
static jboolean loaded = JNI_FALSE;

#define DEBUG_TRAP __asm("int $3");

JF_LIB_HANDLE codec = NULL;
JF_LIB_HANDLE device = NULL;
JF_LIB_HANDLE ffilter = NULL;
JF_LIB_HANDLE format = NULL;
JF_LIB_HANDLE util = NULL;
JF_LIB_HANDLE resample = NULL;
JF_LIB_HANDLE postproc = NULL;
JF_LIB_HANDLE scale = NULL;
jboolean shownCopyWarning = JNI_FALSE;

static void copyWarning() {
  printf("Warning : JNI::Get*ArrayElements returned a copy : Performance will be degraded!\n");
  shownCopyWarning = JNI_TRUE;
}

//avcodec functions
void (*_avcodec_register_all)();
AVCodec* (*_avcodec_find_decoder)(int codec_id);
int (*_avcodec_decode_video2)(AVCodecContext *avctx,AVFrame *picture,int* got_picture_ptr,AVPacket *avpkt);
int (*_avcodec_decode_audio4)(AVCodecContext *avctx,AVFrame *frame,int* got_frame_ptr,AVPacket *avpkt);
int (*_avcodec_open2)(AVCodecContext *avctx,AVCodec *codec,void* options);
AVCodecContext* (*_avcodec_alloc_context3)(AVCodec *codec);
void (*_av_init_packet)(AVPacket *pkt);
void (*_av_free_packet)(AVPacket *pkt);  //free data inside packet (not packet itself)
//encoding
AVCodec* (*_avcodec_find_encoder)(int codec_id);
//int (*_avpicture_alloc)(AVPicture *pic, int pix_fmt, int width, int height);
//int (*_avpicture_free)(AVPicture *pic);
int (*_avcodec_encode_video2)(AVCodecContext *cc, AVPacket *pkt, AVFrame *frame, int* intref);
int (*_avcodec_encode_audio2)(AVCodecContext *cc, AVPacket *pkt, AVFrame *frame, int* intref);
int (*_avcodec_fill_audio_frame)(AVFrame *frame, int nb_channels, int fmt, void* buf, int bufsize, int align);
int (*_avcodec_close)(AVCodecContext *cc);
const char* (*_avcodec_get_name)(AVCodecID id);
void (*_av_packet_rescale_ts)(AVPacket *pkt, AVRational src, AVRational dst);
int (*_avcodec_parameters_to_context)(AVCodecContext *ctx, const AVCodecParameters *par);
int (*_avcodec_parameters_from_context)(AVCodecParameters *par, const AVCodecContext *ctx);

//avdevice functions
void (*_avdevice_register_all)();

//avfilter functions
void (*_avfilter_register_all)();

//avformat functions
void (*_av_register_all)();
void (*_av_register_output_format)(AVOutputFormat *oformat);
AVOutputFormat* (*_av_guess_format)(const char* shortName, const char* fileName, const char* mimeType);
int (*_av_find_best_stream)(AVFormatContext *ic,int type,int wanted_stream_nb,int related_stream
  ,void** decoder_ret, int flags);
AVIOContext* (*_avio_alloc_context)(void* buffer,int buffer_size,int write_flag,void* opaque
  ,void* read,void* write,void* seek);
AVFormatContext* (*_avformat_alloc_context)();
int (*_avio_close)(void* ctx);
void (*_avformat_free_context)(AVFormatContext *s);
int (*_avformat_open_input)(void** ps,const char* filename,void* fmt,void* options);
int (*_avformat_find_stream_info)(AVFormatContext *ic,void** options);
int (*_av_read_frame)(AVFormatContext *s,AVPacket *pkt);
void* (*_av_find_input_format)(const char* name);
void* (*_av_iformat_next)(void* ptr);
int (*_avformat_seek_file)(AVFormatContext *ctx, int stream_idx, int64_t min_ts, int64_t ts, int64_t max_ts, int flags);
int (*_av_seek_frame)(AVFormatContext *ctx, int stream_idx, int64_t ts, int flags);
//encoding
AVStream* (*_avformat_new_stream)(AVFormatContext *fc, AVCodec *codec);
int (*_avformat_write_header)(AVFormatContext *fc, void* opts);
int (*_av_interleaved_write_frame)(AVFormatContext *fc, AVPacket *pkt);
int (*_av_write_frame)(AVFormatContext *fc, AVPacket *pkt);
int (*_avio_flush)(void* io_ctx);
void (*_av_dump_format)(AVFormatContext *fmt_ctx, int index, const char* url, int is_output);
int (*_av_write_trailer)(AVFormatContext *fc);

//avutil functions
void (*_av_image_copy)(uint8_t* dst_data[],int dst_linesizes[]
  , uint8_t* src_data[],int src_linesizes[],int pix_fmt,int width,int height);
int (*_av_get_bytes_per_sample)(int sample_fmt);
void* (*_av_malloc)(int size);
void* (*_av_mallocz)(int size);
void (*_av_free)(void* ptr);
void (*_av_freep)(void** ptr);
int (*_av_image_alloc)(uint8_t* ptrs[],int linesizes[],int w,int h,int pix_fmt,int align);
int (*_av_opt_set)(void* obj,const char* name,const char* val,int search_flags);
int (*_av_opt_set_int)(void* obj,const char* name,int64_t val,int search_flags);
int (*_av_opt_set_sample_fmt)(void* obj,const char* name,int val,int search_flags);
int (*_av_opt_get)(void* obj,const char* name,int search_flags,void* val[]);
int (*_av_opt_get_int)(void* obj,const char* name,int search_flags,int64_t val[]);
//    int (*_av_opt_get_pixel_fmt)(void* obj,const char* name,int search_flags,int val[]);
void* (*_av_opt_find)(void* obj, const char* name, const char* unit, int opt_flags, int search_flags);
void* (*_av_opt_next)(void* obj, void* prev);
void* (*_av_opt_child_next)(void* obj, void* prev);
void (*_av_opt_set_defaults)(void* obj);
int64_t (*_av_rescale_rnd)(int64_t a,int64_t b,int64_t c,AVRounding r);
int (*_av_samples_alloc)(uint8_t* audio_data[],int linesize[],int nb_channels,int nb_samples,int sample_fmt,int align);
int64_t (*_av_rescale_q)(int64_t a, AVRational bq, AVRational cq);
int (*_av_samples_get_buffer_size)(void* linesize, int chs, int samples, int sample_fmt, int align);
int (*_av_log_set_level)(int lvl);
void* (*_av_dict_get)(void* dict, const char* key, void* prev, int flags);
int (*_av_dict_set)(void** dictref, const char* key, const char* value, int flags);
int (*_av_frame_make_writable)(AVFrame *frame);
int (*_av_compare_ts)(int64_t ts_a, AVRational tb_a, int64_t ts_b, AVRational tb_b);
int (*_av_frame_get_buffer)(AVFrame *frame, int align);
AVFrame* (*_av_frame_alloc)();
void (*_av_frame_free)(void** frame);

//swresample functions )(ffmpeg.org)
void* (*_swr_alloc)();
void* (*_swr_alloc_set_opts)(void*, int64_t out_ch_layout, int out_sample_fmt, int out_sample_rate, int64_t in_ch_layout, int in_sample_fmt, int in_sample_rate, int log_offset, void*log_ctx);
int (*_swr_init)(void* ctx);
int64_t (*_swr_get_delay)(void* ctx,int64_t base);
int (*_swr_convert)(void* ctx,uint8_t* out_arg[],int out_count,uint8_t* in_arg[],int in_count);
void (*_swr_free)(void** ctx);

//avresample functions )(libav.org);
void* (*_avresample_alloc_context)();
int (*_avresample_open)(void* ctx);
int (*_avresample_free)(void* ctx);
int64_t (*_avresample_get_delay)(void* ctx);
int (*_avresample_convert)(void* ctx,uint8_t* out_arg[],int out_plane_size, int out_count,uint8_t* in_arg[]
  , int in_plane_size, int in_count);

//swscale functions
void* (*_sws_getContext)(int srcW,int srcH,int srcFormat,int dstW,int dstH,int dstFormat
  ,int flags,void* srcFilter,void* dstFilter, void* param);
int (*_sws_scale)(void* c, uint8_t* srcSlice[],int srcStride[],int srcSliceY,int srcSliceH
  ,uint8_t* dst[],int dstStride[]);
void (*_sws_freeContext)(void* ctx);

static AVPacket *AVPacket_New() {
  AVPacket *pkt = (AVPacket*)(*_av_mallocz)(sizeof(AVPacket));
  return pkt;
}

static AVOutputFormat *AVOutputFormat_New() {
  AVOutputFormat *ofmt = (AVOutputFormat*)(*_av_mallocz)(sizeof(AVOutputFormat));
  return ofmt;
}

/*
static AVPicture *AVPicture_New() {
  AVPicture *pic = (AVPicture*)(*_av_mallocz)(sizeof(AVPicture));
  return pic;
}
*/

static AVOutputFormat *vpx = NULL;

static void register_vpx() {
  vpx = (*_av_guess_format)("vpx", NULL, NULL);
  if (vpx != NULL) {
    return;
  }
  //basically just clone h264
  AVOutputFormat *h264 = (*_av_guess_format)("h264", NULL, NULL);
  if (h264 == NULL) {
    printf("FFMPEG:Unable to register vpx codec\n");
    return;
  }
  vpx = AVOutputFormat_New();
  vpx->name = "vpx";
  vpx->long_name = "raw vpx";
  vpx->extensions = "vpx";
  vpx->audio_codec = AV_CODEC_ID_NONE;
  vpx->video_codec = AV_CODEC_ID_VP8;
  vpx->write_packet = h264->write_packet;
  vpx->flags = h264->flags;
  (*_av_register_output_format)(vpx);
}

//AVPicture was deprecated in ffmpeg/3.0 - use AVFrame instead - this function does the same as avpicture_alloc() except using AVFrame
int _avframe_alloc(AVFrame *picture, enum AVPixelFormat pix_fmt, int width, int height)
{
  int ret = (*_av_image_alloc)(picture->data, picture->linesize, width, height, pix_fmt, 1);
  if (ret < 0) {
    memset(picture, 0, sizeof(AVFrame));
    return ret;
  }

  return 0;
}

#ifndef _WIN32
int GetLastError() {
  return errno;
}
#endif

static int64_t currentTimeMillis() {
  return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}

static jboolean ffmpeg_init(const char* codecFile, const char* deviceFile, const char* filterFile, const char* formatFile
  , const char* utilFile, const char* scaleFile, const char* postFile, const char* resampleFile)
{
  //load libraries (order is important)

  util = JF_LIB_OPEN(utilFile JF_LIB_OPTS);
  if (util == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), utilFile);
    return JNI_FALSE;
  }

  resample = JF_LIB_OPEN(resampleFile JF_LIB_OPTS);
  if (resample == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), resampleFile);
    return JNI_FALSE;
  }

  scale = JF_LIB_OPEN(scaleFile JF_LIB_OPTS);
  if (scale == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), scaleFile);
    return JNI_FALSE;
  }

  postproc = JF_LIB_OPEN(postFile JF_LIB_OPTS);
  if (postproc == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), postFile);
    return JNI_FALSE;
  }

  codec = JF_LIB_OPEN(codecFile JF_LIB_OPTS);
  if (codec == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), codecFile);
    return JNI_FALSE;
  }

  format = JF_LIB_OPEN(formatFile JF_LIB_OPTS);
  if (format == NULL)  {
    printf("Could not find(0x%x):%s\n", GetLastError(), formatFile);
    return JNI_FALSE;
  }

  ffilter = JF_LIB_OPEN(filterFile JF_LIB_OPTS);
  if (ffilter == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), filterFile);
    return JNI_FALSE;
  }

  device = JF_LIB_OPEN(deviceFile JF_LIB_OPTS);
  if (device == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), deviceFile);
    return JNI_FALSE;
  }

  //get functions
  getFunction(codec, (void**)&_avcodec_register_all, "avcodec_register_all");
  getFunction(codec, (void**)&_avcodec_find_decoder, "avcodec_find_decoder");
  getFunction(codec, (void**)&_avcodec_decode_video2, "avcodec_decode_video2");
  getFunction(codec, (void**)&_avcodec_decode_audio4, "avcodec_decode_audio4");
  getFunction(codec, (void**)&_avcodec_open2, "avcodec_open2");
  getFunction(codec, (void**)&_avcodec_alloc_context3, "avcodec_alloc_context3");
  getFunction(codec, (void**)&_av_init_packet, "av_init_packet");
  getFunction(codec, (void**)&_av_free_packet, "av_free_packet");
  getFunction(codec, (void**)&_avcodec_find_encoder, "avcodec_find_encoder");
//  getFunction(codec, (void**)&_avpicture_alloc, "avpicture_alloc");
//  getFunction(codec, (void**)&_avpicture_free, "avpicture_free");
  getFunction(codec, (void**)&_avcodec_encode_video2, "avcodec_encode_video2");
  getFunction(codec, (void**)&_avcodec_encode_audio2, "avcodec_encode_audio2");
  getFunction(codec, (void**)&_avcodec_fill_audio_frame, "avcodec_fill_audio_frame");
  getFunction(codec, (void**)&_avcodec_close, "avcodec_close");
  if (!libav_org) {
    getFunction(codec, (void**)&_avcodec_get_name, "avcodec_get_name");  //for debug output only
  }
  getFunction(codec, (void**)&_av_packet_rescale_ts, "av_packet_rescale_ts");
  getFunction(codec, (void**)&_avcodec_parameters_to_context, "avcodec_parameters_to_context");
  getFunction(codec, (void**)&_avcodec_parameters_from_context, "avcodec_parameters_from_context");

  getFunction(device, (void**)&_avdevice_register_all, "avdevice_register_all");

  getFunction(ffilter, (void**)&_avfilter_register_all, "avfilter_register_all");

  getFunction(format, (void**)&_av_register_all, "av_register_all");
  getFunction(format, (void**)&_av_register_output_format, "av_register_output_format");
  getFunction(format, (void**)&_av_guess_format, "av_guess_format");
  getFunction(format, (void**)&_av_find_best_stream, "av_find_best_stream");
  getFunction(format, (void**)&_avio_alloc_context, "avio_alloc_context");
  getFunction(format, (void**)&_avformat_alloc_context, "avformat_alloc_context");
  getFunction(format, (void**)&_avio_close, "avio_close");
  getFunction(format, (void**)&_avformat_free_context, "avformat_free_context");
  getFunction(format, (void**)&_avformat_open_input, "avformat_open_input");
  getFunction(format, (void**)&_avformat_find_stream_info, "avformat_find_stream_info");
  getFunction(format, (void**)&_av_read_frame, "av_read_frame");
  getFunction(format, (void**)&_av_find_input_format, "av_find_input_format");
  getFunction(format, (void**)&_av_iformat_next, "av_iformat_next");
  getFunction(format, (void**)&_avformat_seek_file, "avformat_seek_file");
  getFunction(format, (void**)&_av_seek_frame, "av_seek_frame");
  getFunction(format, (void**)&_avformat_new_stream, "avformat_new_stream");
  getFunction(format, (void**)&_avformat_write_header, "avformat_write_header");
  getFunction(format, (void**)&_av_interleaved_write_frame, "av_interleaved_write_frame");
  getFunction(format, (void**)&_av_write_frame, "av_write_frame");
  getFunction(format, (void**)&_avio_flush, "avio_flush");
  getFunction(format, (void**)&_av_dump_format, "av_dump_format");
  getFunction(format, (void**)&_av_write_trailer, "av_write_trailer");

  getFunction(util, (void**)&_av_image_copy, "av_image_copy");
  getFunction(util, (void**)&_av_get_bytes_per_sample, "av_get_bytes_per_sample");
  getFunction(util, (void**)&_av_malloc, "av_malloc");
  getFunction(util, (void**)&_av_mallocz, "av_mallocz");
  getFunction(util, (void**)&_av_free, "av_free");
  getFunction(util, (void**)&_av_freep, "av_freep");
  getFunction(util, (void**)&_av_image_alloc, "av_image_alloc");
  getFunction(util, (void**)&_av_opt_set, "av_opt_set");
  getFunction(util, (void**)&_av_opt_set_int, "av_opt_set_int");
  getFunction(util, (void**)&_av_opt_set_sample_fmt, "av_opt_set_sample_fmt");
  getFunction(util, (void**)&_av_opt_get, "av_opt_get");
  getFunction(util, (void**)&_av_opt_get_int, "av_opt_get_int");
  getFunction(util, (void**)&_av_opt_find, "av_opt_find");
  getFunction(util, (void**)&_av_opt_next, "av_opt_next");
  getFunction(util, (void**)&_av_opt_child_next, "av_opt_child_next");
  getFunction(util, (void**)&_av_opt_set_defaults, "av_opt_set_defaults");
  getFunction(util, (void**)&_av_rescale_rnd, "av_rescale_rnd");
  getFunction(util, (void**)&_av_samples_alloc, "av_samples_alloc");
  getFunction(util, (void**)&_av_rescale_q, "av_rescale_q");
  getFunction(util, (void**)&_av_samples_get_buffer_size, "av_samples_get_buffer_size");
  getFunction(util, (void**)&_av_log_set_level, "av_log_set_level");
  getFunction(util, (void**)&_av_dict_get, "av_dict_get");
  getFunction(util, (void**)&_av_dict_set, "av_dict_set");
  getFunction(util, (void**)&_av_frame_make_writable, "av_frame_make_writable");
  getFunction(util, (void**)&_av_compare_ts, "av_compare_ts");
  getFunction(util, (void**)&_av_frame_get_buffer, "av_frame_get_buffer");
  getFunction(util, (void**)&_av_frame_alloc, "av_frame_alloc");
  getFunction(util, (void**)&_av_frame_free, "av_frame_free");

  getFunction(scale, (void**)&_sws_getContext, "sws_getContext");
  getFunction(scale, (void**)&_sws_scale, "sws_scale");
  getFunction(scale, (void**)&_sws_freeContext, "sws_freeContext");

  if (!libav_org) {
    getFunction(resample, (void**)&_swr_alloc, "swr_alloc");
    getFunction(resample, (void**)&_swr_alloc_set_opts, "swr_alloc_set_opts");
    getFunction(resample, (void**)&_swr_init, "swr_init");
    getFunction(resample, (void**)&_swr_get_delay, "swr_get_delay");
    getFunction(resample, (void**)&_swr_convert, "swr_convert");
    getFunction(resample, (void**)&_swr_free, "swr_free");
  } else {
    getFunction(resample, (void**)&_avresample_alloc_context, "avresample_alloc_context");
    getFunction(resample, (void**)&_avresample_open, "avresample_open");
    getFunction(resample, (void**)&_avresample_free, "avresample_free");
    getFunction(resample, (void**)&_avresample_get_delay, "avresample_get_delay");
    getFunction(resample, (void**)&_avresample_convert, "avresample_convert");
  }

  //register_all
  (*_avcodec_register_all)();
  (*_avdevice_register_all)();
  (*_avfilter_register_all)();
  (*_av_register_all)();
  register_vpx();

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaCoder_ffmpeg_1init
  (JNIEnv *e, jclass c, jstring jcodec, jstring jdevice, jstring jfilter, jstring jformat, jstring jutil, jstring jresample, jstring jpostproc, jstring jscale, jboolean _libav_org)
{
  if (loaded) return loaded;
  libav_org = _libav_org;

  const char *codecFile = e->GetStringUTFChars(jcodec, NULL);

  const char *deviceFile = e->GetStringUTFChars(jdevice, NULL);

  const char *filterFile = e->GetStringUTFChars(jfilter, NULL);

  const char *formatFile = e->GetStringUTFChars(jformat, NULL);

  const char *utilFile = e->GetStringUTFChars(jutil, NULL);

  const char *resampleFile = e->GetStringUTFChars(jresample, NULL);

  const char *postFile = e->GetStringUTFChars(jpostproc, NULL);

  const char *scaleFile = e->GetStringUTFChars(jscale, NULL);

  jboolean ret = ffmpeg_init(codecFile, deviceFile, filterFile, formatFile, utilFile, resampleFile, postFile, scaleFile);

  e->ReleaseStringUTFChars(jcodec, codecFile);
  e->ReleaseStringUTFChars(jdevice, deviceFile);
  e->ReleaseStringUTFChars(jfilter, filterFile);
  e->ReleaseStringUTFChars(jformat, formatFile);
  e->ReleaseStringUTFChars(jutil, utilFile);
  e->ReleaseStringUTFChars(jresample, resampleFile);
  e->ReleaseStringUTFChars(jpostproc, postFile);
  e->ReleaseStringUTFChars(jscale, scaleFile);

  if (!ret) return JNI_FALSE;

  //get JNI IDs

  loaded = JNI_TRUE;

  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaCoder_ffmpeg_1set_1logging
  (JNIEnv *e, jclass c, jboolean state)
{
  (*_av_log_set_level)(state ? AV_LOG_ERROR : AV_LOG_QUIET);
}

static int ff_min(int a, int b) {
  if (a < b) return a; else return b;
}

struct FFContext {
  JNIEnv *e;  //only valid during native function
  jobject c;  //only valid during native function

  //MediaIO (these can be cached since the MediaIO object should not be GCed)
  jclass cls_mio;
  jmethodID mid_ff_read, mid_ff_write, mid_ff_seek;

  //decoder fields
  jobject mio;
  void *ff_buffer;
  AVFormatContext *fmt_ctx;
  AVIOContext *io_ctx;

  void* input_fmt;

  AVCodecContext *codec_ctx;  //returned by open_codec_context()

  int video_stream_idx;
  AVStream *video_stream;
  AVCodecContext *video_codec_ctx;
  void* sws_ctx;
  //compressed video
  int video_dst_bufsize;
  uint8_t* video_dst_data[4];
  int video_dst_linesize[4];
  //rgb video
  int rgb_video_dst_bufsize;
  uint8_t* rgb_video_dst_data[4];
  int rgb_video_dst_linesize[4];

  int audio_stream_idx;
  AVStream *audio_stream;
  AVCodecContext *audio_codec_ctx;
  void* swr_ctx;

  //compressed audio
  int src_rate;
  //user audio
  int dst_rate;
  int dst_nb_channels;
  int dst_sample_fmt;
  uint8_t* audio_dst_data[4];
  int audio_dst_linesize[4];

  AVPacket *pkt;  //decoders only
  int pkt_size_left;
  jboolean pkt_key_frame;

  AVFrame *frame;

  jintArray jvideo;
  int jvideo_length;
  jint* jvideo_ptr;

  jshortArray jaudio;
  int jaudio_length;

  //additional raw video decoder fields
  AVCodec *video_codec;

  //additional encoder fields
  AVCodec *audio_codec;
  AVOutputFormat *out_fmt;
  int width, height, fps;
  int chs, freq;
  AVFrame *audio_frame, *video_frame;
  AVFrame *src_pic, *dst_pic;
  jboolean audio_frame_size_variable;
  jboolean video_delay;
  int audio_frame_size;
  short *audio_buffer;
  int audio_buffer_size;
  int64_t audio_pts;
  int64_t video_pts;
  AVRational audio_ratio;

  uint8_t* audio_src_data[4];

  /** Set to make fps = fps * 1000 / 1001. */
  jboolean fps_1000_1001;

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

  void GetMediaIO() {
    cls_mio = e->GetObjectClass(mio);
    mid_ff_read = e->GetMethodID(cls_mio, "read", "(Ljavaforce/media/MediaCoder;[B)I");
    mid_ff_write = e->GetMethodID(cls_mio, "write", "(Ljavaforce/media/MediaCoder;[B)I");
    mid_ff_seek = e->GetMethodID(cls_mio, "seek", "(Ljavaforce/media/MediaCoder;JI)J");
  }
};

#define ffiobufsiz (32 * 1024)

FFContext* createFFContext(JNIEnv *e, jobject c) {
  FFContext *ctx;
  jclass cls_coder = e->FindClass("javaforce/media/MediaCoder");
  jfieldID fid_ff_ctx = e->GetFieldID(cls_coder, "ctx", "J");
  ctx = (FFContext*)e->GetLongField(c, fid_ff_ctx);
  if (ctx != NULL) {
    printf("MediaCoder used twice\n");
    return NULL;
  }
  ctx = new FFContext();
  memset(ctx, 0, sizeof(FFContext));
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
  delete ctx;
  jclass cls_coder = e->FindClass("javaforce/media/MediaCoder");
  jfieldID fid_ff_ctx = e->GetFieldID(cls_coder, "ctx", "J");
  e->SetLongField(c,fid_ff_ctx,0);
}

static int read_packet(FFContext *ctx, void*buf, int size) {
  jbyteArray ba = ctx->e->NewByteArray(size);
  jint read = ctx->e->CallIntMethod(ctx->mio, ctx->mid_ff_read, ctx->c, ba);
  if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
  if (read > 0) {
    ctx->e->GetByteArrayRegion(ba, 0, read, (jbyte*)buf);
  }
  ctx->e->DeleteLocalRef(ba);
  return read;
}

static int write_packet(FFContext *ctx, void*buf, int size) {
  jbyteArray ba = ctx->e->NewByteArray(size);
  ctx->e->SetByteArrayRegion(ba, 0, size, (jbyte*)buf);
  int write = ctx->e->CallIntMethod(ctx->mio, ctx->mid_ff_write, ctx->c, ba);  //obj, methodID, args[]
  if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
  ctx->e->DeleteLocalRef(ba);
  return write;
}

static jlong zero = 0;

static jlong seek_packet(FFContext *ctx, jlong offset, int how) {
  if (how == AVSEEK_SIZE) { //return size of file
    jlong curpos = ctx->e->CallLongMethod(ctx->mio, ctx->mid_ff_seek, ctx->c, zero, SEEK_CUR);
    if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
    jlong filesize = ctx->e->CallLongMethod(ctx->mio, ctx->mid_ff_seek, ctx->c, zero, SEEK_END);
    if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
    ctx->e->CallLongMethod(ctx->mio, ctx->mid_ff_seek, ctx->c, curpos, SEEK_SET);
    if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
    return filesize;
  }
  jlong ret = ctx->e->CallLongMethod(ctx->mio, ctx->mid_ff_seek, ctx->c, offset, how);
  if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
  return ret;
}

//decoder codebase

//returns stream idx >= 0
static int open_codec_context(FFContext *ctx, AVFormatContext *fmt_ctx, int type)
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
//    ctx->codec_ctx->flags |= CODEC_FLAG_LOW_DELAY;
    if ((ret = (*_avcodec_open2)(ctx->codec_ctx, codec, NULL)) < 0) {
      return ret;
    }
  }
  return stream_idx;
}

static jboolean open_codecs(FFContext *ctx, int new_width, int new_height, int new_chs, int new_freq) {
  AVCodecContext *codec_ctx;
  if ((ctx->video_stream_idx = open_codec_context(ctx, ctx->fmt_ctx, AVMEDIA_TYPE_VIDEO)) >= 0) {
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

  if ((ctx->audio_stream_idx = open_codec_context(ctx, ctx->fmt_ctx, AVMEDIA_TYPE_AUDIO)) >= 0) {
    ctx->audio_stream = (AVStream*)ctx->fmt_ctx->streams[ctx->audio_stream_idx];
    ctx->audio_codec_ctx = ctx->codec_ctx;
    //create audio conversion context
    if (libav_org)
      ctx->swr_ctx = (*_avresample_alloc_context)();
    else
      ctx->swr_ctx = (*_swr_alloc)();
    if (new_chs == -1) new_chs = ctx->audio_codec_ctx->channels;
    int64_t new_layout;
    switch (new_chs) {
      case 1: new_layout = AV_CH_LAYOUT_MONO; ctx->dst_nb_channels = 1; break;
      case 2: new_layout = AV_CH_LAYOUT_STEREO; ctx->dst_nb_channels = 2; break;
      case 4: new_layout = AV_CH_LAYOUT_QUAD; ctx->dst_nb_channels = 4; break;
      default: return JNI_FALSE;
    }
    int64_t src_layout = ctx->audio_codec_ctx->channel_layout;
    if (src_layout == 0) {
      switch (ctx->audio_codec_ctx->channels) {
        case 1: src_layout = AV_CH_LAYOUT_MONO; break;
        case 2: src_layout = AV_CH_LAYOUT_STEREO; break;
        case 4: src_layout = AV_CH_LAYOUT_QUAD; break;
        default: return JNI_FALSE;
      }
    }
    ctx->dst_sample_fmt = AV_SAMPLE_FMT_S16;
    ctx->src_rate = ctx->audio_codec_ctx->sample_rate;
    if (new_freq == -1) new_freq = ctx->src_rate;
    if (libav_org) {
      (*_av_opt_set_int)(ctx->swr_ctx, "in_channel_count",     ctx->audio_codec_ctx->channels, 0);
      (*_av_opt_set_int)(ctx->swr_ctx, "in_sample_rate",        ctx->src_rate, 0);
      (*_av_opt_set_sample_fmt)(ctx->swr_ctx, "in_sample_fmt",  ctx->audio_codec_ctx->sample_fmt, 0);
      (*_av_opt_set_int)(ctx->swr_ctx, "out_channel_count",    new_chs, 0);
      (*_av_opt_set_int)(ctx->swr_ctx, "out_sample_rate",       new_freq, 0);
      (*_av_opt_set_sample_fmt)(ctx->swr_ctx, "out_sample_fmt", ctx->dst_sample_fmt, 0);
    } else {
      ctx->swr_ctx = (*_swr_alloc_set_opts)(ctx->swr_ctx,
        new_layout, ctx->dst_sample_fmt, new_freq, src_layout,
        ctx->audio_codec_ctx->sample_fmt, ctx->src_rate,
        0, NULL);
    }

    int ret;
    if (libav_org)
      ret = (*_avresample_open)(ctx->swr_ctx);
    else
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
  ctx->pkt_size_left = 0;

  return JNI_TRUE;
}

/**
 * Starts demuxing/decoding a stream.
 * @param new_width - scale video to new width (-1 = use stream width)
 * @param new_height - scale video to new height (-1 = use stream height)
 * @param new_chs - # of channels to mix to (-1 = use stream channels)
 * @param new_freq - output sampling rate (-1 = use stream rate)
 * @param seekable - can you seek input? (true=file JNI_FALSE=stream)
 * NOTE : Audio output is always 16bit
 */

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_start
  (JNIEnv *e, jobject c, jobject mio, jint new_width, jint new_height, jint new_chs, jint new_freq, jboolean seekable)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  ctx->mio = e->NewGlobalRef(mio);
  ctx->GetMediaIO();

  ctx->ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ctx->ff_buffer, ffiobufsiz, 0, (void*)ctx, (void*)&read_packet, (void*)&write_packet, seekable ? (void*)&seek_packet : NULL);
  ctx->fmt_ctx = (*_avformat_alloc_context)();
  ctx->fmt_ctx->pb = ctx->io_ctx;
  int res;
  if ((res = (*_avformat_open_input)((void**)&ctx->fmt_ctx, "stream", NULL, NULL)) != 0) {
    printf("avformat_open_input() failed : %d\n", res);
    return JNI_FALSE;
  }
  if ((res = (*_avformat_find_stream_info)(ctx->fmt_ctx, NULL)) < 0) {
    printf("avformat_find_stream_info() failed : %d\n", res);
    return JNI_FALSE;
  }
  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);
  return open_codecs(ctx, new_width, new_height, new_chs, new_freq);
}

/**
 * Alternative start that works with files.
 *
 * Example: start("/dev/video0", "v4l2", ...);
 *
 * NOTE:input_format may be NULL
 */

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_startFile
  (JNIEnv *e, jobject c, jstring file, jstring input_format, jint new_width, jint new_height, jint new_chs, jint new_freq)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  int res;
  ctx->fmt_ctx = (*_avformat_alloc_context)();
  const char *cinput_format = e->GetStringUTFChars(input_format, NULL);
  if (cinput_format != NULL) {
    ctx->input_fmt = (*_av_find_input_format)(cinput_format);
    if (ctx->input_fmt == NULL) {
      printf("FFMPEG:av_find_input_format failed:%s\n", cinput_format);
      e->ReleaseStringUTFChars(input_format, cinput_format);
      return JNI_FALSE;
    }
  }
  e->ReleaseStringUTFChars(input_format, cinput_format);
  const char *cfile = e->GetStringUTFChars(file, NULL);
  if ((res = (*_avformat_open_input)((void**)&ctx->fmt_ctx, cfile, ctx->input_fmt, NULL)) != 0) {
    e->ReleaseStringUTFChars(file, cfile);
    printf("avformat_open_input() failed : %d\n", res);
    return JNI_FALSE;
  }
  e->ReleaseStringUTFChars(file, cfile);

  (*_av_dump_format)(ctx->fmt_ctx, 0, "memory_io", 0);

  if ((res = (*_avformat_find_stream_info)(ctx->fmt_ctx, NULL)) < 0) {
    printf("avformat_find_stream_info() failed : %d\n", res);
    return JNI_FALSE;
  }
  return open_codecs(ctx, new_width, new_height, new_chs, new_freq);
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaDecoder_stop
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->io_ctx != NULL) {
    (*_avio_flush)(ctx->io_ctx);
    ctx->io_ctx = NULL;
    ctx->ff_buffer = NULL;
  }
  if (ctx->fmt_ctx != NULL) {
    (*_avformat_free_context)(ctx->fmt_ctx);
    ctx->fmt_ctx = NULL;
  }
  if (ctx->frame != NULL) {
    (*_av_frame_free)((void**)&ctx->frame);
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
    if (libav_org)
      (*_avresample_free)(&ctx->swr_ctx);
    else
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
    (*_av_free_packet)(ctx->pkt);
    (*_av_free)(ctx->pkt);
    ctx->pkt = NULL;
  }
  deleteFFContext(e,c,ctx);
}

/** Reads next frame in stream and returns what type it was : AUDIO_FRAME, VIDEO_FRAME, NULL_FRAME or END_FRAME */
JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_read
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  //read another frame
  if (ctx->pkt_size_left == 0) {
    if ((*_av_read_frame)(ctx->fmt_ctx, ctx->pkt) >= 0) {
      ctx->pkt_size_left = ctx->pkt->size;
      ctx->pkt_key_frame = ((ctx->pkt->flags & 0x0001) == 0x0001);
    } else {
      return END_FRAME;
    }
  }

  //try to decode another frame
  if (ctx->pkt->stream_index == ctx->video_stream_idx) {
    //extract a video frame
    int got_frame = 0;
    int ret = (*_avcodec_decode_video2)(ctx->video_codec_ctx, ctx->frame, &got_frame, ctx->pkt);
    if (ret < 0) {
      ctx->pkt_size_left = 0;
      printf("Error:%d\n", ret);
      return NULL_FRAME;
    }
    (*_av_free_packet)(ctx->pkt);
    ctx->pkt_size_left = 0;  //use entire packet
    if (got_frame == 0) return NULL_FRAME;
    (*_av_image_copy)(ctx->video_dst_data, ctx->video_dst_linesize
      , ctx->frame->data, ctx->frame->linesize
      , ctx->video_codec_ctx->pix_fmt, ctx->video_codec_ctx->width, ctx->video_codec_ctx->height);
    //convert image to RGBA format
    (*_sws_scale)(ctx->sws_ctx, ctx->video_dst_data, ctx->video_dst_linesize, 0, ctx->video_codec_ctx->height
      , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);
    return VIDEO_FRAME;
  }

  if (ctx->pkt->stream_index == ctx->audio_stream_idx) {
    //extract an audio frame
    int got_frame = 0;
    int ret = (*_avcodec_decode_audio4)(ctx->audio_codec_ctx, ctx->frame, &got_frame, ctx->pkt);
    if (ret < 0) {
      ctx->pkt_size_left = 0;
      printf("Error:%d\n", ret);
      return NULL_FRAME;
    }
    ret = ff_min(ctx->pkt_size_left, ret);
    ctx->pkt_size_left -= ret;
    if (ctx->pkt_size_left == 0) {
      (*_av_free_packet)(ctx->pkt);
    }
    if (got_frame == 0) {
      return NULL_FRAME;
    }
//    int unpadded_linesize = frame.nb_samples * avutil.av_get_bytes_per_sample(audio_codec_ctx.sample_fmt);
    //convert to new format
    int dst_nb_samples;
    if (libav_org) {
      dst_nb_samples = (int)(*_av_rescale_rnd)((*_avresample_get_delay)(ctx->swr_ctx)
        + ctx->frame->nb_samples, ctx->dst_rate, ctx->src_rate, AV_ROUND_UP);
    } else {
      dst_nb_samples = (int)(*_av_rescale_rnd)((*_swr_get_delay)(ctx->swr_ctx, ctx->src_rate)
        + ctx->frame->nb_samples, ctx->dst_rate, ctx->src_rate, AV_ROUND_UP);
    }
    if (((*_av_samples_alloc)(ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->dst_nb_channels
      , dst_nb_samples, ctx->dst_sample_fmt, 1)) < 0) return NULL_FRAME;
    int converted_nb_samples = 0;
    if (libav_org) {
      converted_nb_samples = (*_avresample_convert)(ctx->swr_ctx, ctx->audio_dst_data, 0, dst_nb_samples
        , ctx->frame->extended_data, 0, ctx->frame->nb_samples);
    } else {
      converted_nb_samples = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, dst_nb_samples
        , ctx->frame->extended_data, ctx->frame->nb_samples);
    }
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
    return AUDIO_FRAME;
  }

  //discard unknown packet
  (*_av_free_packet)(ctx->pkt);
  ctx->pkt_size_left = 0;  //use entire packet

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
  return ctx->jaudio;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getWidth
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getHeight
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_MediaDecoder_getFrameRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
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
  if (ctx->fmt_ctx == NULL) return 0;
  if (ctx->fmt_ctx->duration << 1 == 0) return 0;  //0x8000000000000000
  return ctx->fmt_ctx->duration / AV_TIME_BASE;  //return in seconds
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getSampleRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->sample_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getChannels
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
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
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->bit_rate;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaDecoder_getAudioBitRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->bit_rate;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_isKeyFrame
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  return ctx->pkt_key_frame;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaDecoder_resize
  (JNIEnv *e, jobject c, jint new_width, jint new_height)
{
  FFContext *ctx = getFFContext(e,c);
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

//rawvideo decoder codebase

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaVideoDecoder_start
  (JNIEnv *e, jobject c, jint codec_id, jint width, jint height)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  printf("context=%p\n", ctx);
  ctx->video_codec = (*_avcodec_find_decoder)(codec_id);
  if (ctx->video_codec == NULL) {
    printf("MediaVideoDecoder : codec == null\n");
    return JNI_FALSE;
  }
  ctx->video_codec_ctx = (*_avcodec_alloc_context3)(ctx->video_codec);

  //set default values
  ctx->video_codec_ctx->codec_id = (AVCodecID)codec_id;
  ctx->video_codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;

  if (((*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, NULL)) < 0) {
    printf("avcodec_open2() failed\n");
    return JNI_FALSE;
  }

  if ((ctx->frame = (*_av_frame_alloc)()) == NULL) return JNI_FALSE;

  ctx->pkt = AVPacket_New();
  (*_av_init_packet)(ctx->pkt);
  ctx->pkt->data = NULL;
  ctx->pkt->size = 0;
  ctx->pkt_size_left = 0;

  ctx->width = width;
  ctx->height = height;

  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaVideoDecoder_stop
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->frame != NULL) {
    (*_av_frame_free)((void**)&ctx->frame);
  }
  if (ctx->video_codec_ctx != NULL) {
    (*_avcodec_close)(ctx->video_codec_ctx);
    (*_av_free)(ctx->video_codec_ctx);
    ctx->video_codec_ctx = NULL;
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
  if (ctx->pkt != NULL) {
    (*_av_free_packet)(ctx->pkt);
    (*_av_free)(ctx->pkt);
    ctx->pkt = NULL;
  }
  if (ctx->jvideo != NULL) {
#ifdef JFDK
    e->RELEASE_INT_ARRAY(ctx->jvideo, ctx->jvideo_ptr, JNI_ABORT);
#endif
    e->DeleteGlobalRef(ctx->jvideo);
    ctx->jvideo = NULL;
  }
  deleteFFContext(e,c,ctx);
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_MediaVideoDecoder_decode
  (JNIEnv *e, jobject c, jbyteArray data, jint offset, jint length)
{
  int64_t p_start = currentTimeMillis();
  FFContext *ctx = getFFContext(e,c);
  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GET_BYTE_ARRAY(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  ctx->pkt->size = length;
  ctx->pkt->data = dataptr + offset;

/*
  uint8_t* pdata = ctx->pkt->data;
  int size = ctx->pkt->size;
  for(int a=0;a<size-4;a++,pdata++) {
    uint32_t* data32 = (uint32_t*)pdata;
    if (((*data32) & 0xffffff) == 0x010000) {
      for(int b=0;b<4;b++) {
        printf("%02x ", pdata[b]);
      }
      printf("...");
    }
  }
  printf(" [%d]\n", size);
*/

  int64_t p_1 = currentTimeMillis();
  int64_t d_1 = p_1 - p_start;

  int got_frame = 0;
  int ret = (*_avcodec_decode_video2)(ctx->video_codec_ctx, ctx->frame, &got_frame, ctx->pkt);
  e->RELEASE_BYTE_ARRAY(data, (jbyte*)dataptr, JNI_ABORT);
  ctx->pkt->data = NULL;
  if (ret < 0) {
    printf("Error:avcodec_decode_video2() == %d\n", ret);
    ctx->pkt->size = 0;
    return NULL;
  }
  if (got_frame == 0) {
    printf("no frame!\n");
    return NULL;
  }

  int64_t p_2 = currentTimeMillis();
  int64_t d_2 = p_2 - p_1;

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
    if ((ctx->video_dst_bufsize = (*_av_image_alloc)(ctx->video_dst_data, ctx->video_dst_linesize
      , ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt, 1)) < 0)
    {
      printf("av_image_alloc() failed\n");
      return NULL;
    }
    if ((ctx->rgb_video_dst_bufsize = (*_av_image_alloc)(ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize
      , ctx->width, ctx->height, AV_PIX_FMT_BGRA, 1)) < 0)
    {
      printf("av_image_alloc(RGB) failed\n");
      return NULL;
    }
    //create video conversion context
    ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
      , ctx->width, ctx->height, AV_PIX_FMT_BGRA
      , SWS_BILINEAR, NULL, NULL, NULL);

    int px_count = ctx->width * ctx->height;
    ctx->jvideo_length = px_count;
    ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));
#ifdef JFDK
    jboolean isCopy;
    ctx->jvideo_ptr = (jint*)ctx->e->GET_INT_ARRAY(ctx->jvideo, &isCopy);
    if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();
#endif
  }

  (*_av_image_copy)(ctx->video_dst_data, ctx->video_dst_linesize
    , ctx->frame->data, ctx->frame->linesize
    , ctx->video_codec_ctx->pix_fmt, ctx->video_codec_ctx->width, ctx->video_codec_ctx->height);

  int64_t p_3 = currentTimeMillis();
  int64_t d_3 = p_3 - p_2;

  //convert image to RGBA format
  (*_sws_scale)(ctx->sws_ctx, ctx->video_dst_data, ctx->video_dst_linesize, 0, ctx->video_codec_ctx->height
    , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);

  int64_t p_4 = currentTimeMillis();
  int64_t d_4 = p_4 - p_3;

#ifdef JFDK
  memcpy(ctx->jvideo_ptr, (const jint*)ctx->rgb_video_dst_data[0], ctx->jvideo_length * 4);
#else
  e->SetIntArrayRegion(ctx->jvideo, 0, ctx->jvideo_length, (const jint*)ctx->rgb_video_dst_data[0]);
#endif

  int64_t p_5 = currentTimeMillis();
  int64_t d_5 = p_5 - p_4;

//  printf("decode profile:%lld %lld %lld %lld %lld\n", d_1, d_2, d_3, d_4, d_5);

  return ctx->jvideo;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaVideoDecoder_getWidth
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaVideoDecoder_getHeight
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_MediaVideoDecoder_getFrameRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->video_codec_ctx == NULL) return 0;
  if (ctx->video_codec_ctx->time_base.num == 0) return 0;
  if (ctx->video_codec_ctx->ticks_per_frame == 0) return 0;
  return ctx->video_codec_ctx->time_base.den / ctx->video_codec_ctx->time_base.num / ctx->video_codec_ctx->ticks_per_frame;
}

//encoder codebase

static jboolean add_stream(FFContext *ctx, int codec_id) {
//  printf("add_stream(codec_id=0x%x)\r\n", codec_id);
  AVCodecContext *codec_ctx;
  AVStream *stream;
  AVCodec *codec;

  codec = (*_avcodec_find_encoder)(codec_id);
  if (codec == NULL) return JNI_FALSE;
  stream = (*_avformat_new_stream)(ctx->fmt_ctx, codec);
  if (stream == NULL) return JNI_FALSE;
  stream->id = ctx->fmt_ctx->nb_streams-1;
  codec_ctx = (*_avcodec_alloc_context3)(codec);

  switch (codec->type) {
    case AVMEDIA_TYPE_AUDIO:
      if (codec->sample_fmts != NULL) {
        bool have_fmt = false;
        for(int idx=0;;idx++) {
          AVSampleFormat fmt = codec->sample_fmts[idx];
          if (fmt == -1) {
            if (!have_fmt) {
              codec_ctx->sample_fmt = codec->sample_fmts[0];
            }
            break;
          }
          printf("available sample format:%d\n", fmt);
          if (fmt == AV_SAMPLE_FMT_S16) {
            //preferred format
            have_fmt = true;
            codec_ctx->sample_fmt = fmt;
            break;
          }
          if (fmt == AV_SAMPLE_FMT_S16P && !have_fmt) {
            //second preferred format
            have_fmt = true;
            codec_ctx->sample_fmt = fmt;
            break;
          }
        };
      } else {
        codec_ctx->sample_fmt = AV_SAMPLE_FMT_S16;
      }

      codec_ctx->bit_rate = ctx->config_audio_bit_rate;
      codec_ctx->sample_rate = ctx->freq;
      codec_ctx->channels = ctx->chs;
      printf("audio:%d %d %d\n", ctx->config_audio_bit_rate, ctx->freq, ctx->chs);
      switch (ctx->chs) {
        case 1: codec_ctx->channel_layout = AV_CH_LAYOUT_MONO; break;
        case 2: codec_ctx->channel_layout = AV_CH_LAYOUT_STEREO; break;
        case 4: codec_ctx->channel_layout = AV_CH_LAYOUT_QUAD; break;
      }
      stream->time_base.num = 1;
      stream->time_base.den = ctx->freq;

      ctx->audio_ratio.num = 1;
      ctx->audio_ratio.den = ctx->freq;

      ctx->audio_stream = stream;
      ctx->audio_codec = codec;
      ctx->audio_codec_ctx = codec_ctx;
      break;
    case AVMEDIA_TYPE_VIDEO:
      codec_ctx->bit_rate = ctx->config_video_bit_rate;
      codec_ctx->width = ctx->width;
      codec_ctx->height = ctx->height;
      if (ctx->fps_1000_1001) {
        codec_ctx->time_base.num = 1000;
        codec_ctx->time_base.den = ctx->fps * 1001;
        stream->time_base.num = 1000;
        stream->time_base.den = ctx->fps * 1001;
      } else {
        codec_ctx->time_base.num = 1;
        codec_ctx->time_base.den = ctx->fps;
        stream->time_base.num = 1;
        stream->time_base.den = ctx->fps;
      }
      codec_ctx->gop_size = ctx->config_gop_size;
      codec_ctx->keyint_min = ctx->config_gop_size;
      codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
      if (codec_ctx->codec_id == AV_CODEC_ID_H264) {
        (*_av_opt_set)(codec_ctx->priv_data, "profile", "baseline", 0);
        (*_av_opt_set)(codec_ctx->priv_data, "preset", "slow", 0);
      }

      ctx->video_stream = stream;
      ctx->video_codec = codec;
      ctx->video_codec_ctx = codec_ctx;
      break;
    case AVMEDIA_TYPE_UNKNOWN:
    case AVMEDIA_TYPE_DATA:
    case AVMEDIA_TYPE_SUBTITLE:
    case AVMEDIA_TYPE_ATTACHMENT:
    case AVMEDIA_TYPE_NB:
      break;
  }

  if ((ctx->out_fmt->flags & AVFMT_GLOBALHEADER) != 0) {
//    codec_ctx->flags |= CODEC_FLAG_GLOBAL_HEADER;
  }

  return JNI_TRUE;
}

static jboolean open_video(FFContext *ctx) {
  int ret = (*_avcodec_open2)(ctx->video_codec_ctx, ctx->video_codec, NULL);
  if (ret < 0) return JNI_FALSE;
  ctx->video_frame = (*_av_frame_alloc)();
  if (ctx->video_frame == NULL) return JNI_FALSE;
  ctx->dst_pic = (*_av_frame_alloc)();
  ret = _avframe_alloc(ctx->dst_pic, ctx->video_codec_ctx->pix_fmt, ctx->video_codec_ctx->width, ctx->video_codec_ctx->height);
  if (ret < 0) return JNI_FALSE;
  if (ctx->video_codec_ctx->pix_fmt != AV_PIX_FMT_BGRA) {
    ctx->src_pic = (*_av_frame_alloc)();
    ret = _avframe_alloc(ctx->src_pic, AV_PIX_FMT_BGRA, ctx->video_codec_ctx->width, ctx->video_codec_ctx->height);
    if (ret < 0) return JNI_FALSE;
    ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, AV_PIX_FMT_BGRA
      , ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt, SWS_BICUBIC, NULL, NULL, NULL);
  }
  //get caps
  ctx->video_delay = (ctx->video_codec->capabilities & AV_CODEC_CAP_DELAY) != 0;
  //set width/height/format
  ctx->video_frame->width = ctx->width;
  ctx->video_frame->height = ctx->height;
  ctx->video_frame->format = ctx->video_codec_ctx->pix_fmt;
  ret = (*_av_frame_get_buffer)(ctx->video_frame, 32);
  if (ret < 0) {
    printf("av_frame_get_buffer() failed! %d\n", ret);
    return JNI_FALSE;
  }
  //copy data/linesize pointers from dst_pic to frame
  for(int a=0;a<8;a++) {
    ctx->video_frame->data[a] = ctx->dst_pic->data[a];
    ctx->video_frame->linesize[a] = ctx->dst_pic->linesize[a];
  }
  //copy params
  ret = (*_avcodec_parameters_from_context)(ctx->video_stream->codecpar, ctx->video_codec_ctx);
  if (ret < 0) {
    printf("avcodec_parameters_from_context() failed!\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

static jboolean open_audio(FFContext *ctx) {
  //audio_codec_ctx = AVCodecContext
  //audio_codec = AVCodec
  printf("open_audio(type=0x%x,ctx.id=0x%x,id=0x%x)\r\n", ctx->audio_codec_ctx->codec_type, ctx->audio_codec_ctx->codec_id, ctx->audio_codec->id);
  int ret = (*_avcodec_open2)(ctx->audio_codec_ctx, ctx->audio_codec, NULL);
  if (ret < 0) {
    printf("avcodec_open2() failed!\n");
    return JNI_FALSE;
  }
  ctx->audio_frame = (*_av_frame_alloc)();
  if (ctx->audio_frame == NULL) {
    printf("av_frame_alloc() failed!\n");
    return JNI_FALSE;
  }
  ctx->audio_frame->format = ctx->audio_codec_ctx->sample_fmt;
  ctx->audio_frame->sample_rate = ctx->freq;
  ctx->audio_codec_ctx->sample_rate = ctx->freq;
  ctx->audio_frame->channel_layout = ctx->audio_codec_ctx->channel_layout;
  ctx->audio_frame->channels = ctx->chs;
  if (ctx->audio_codec_ctx->frame_size == 0) {
    ctx->audio_codec_ctx->frame_size = 2048 * ctx->chs * 2;  //default frame size
  }
  ctx->audio_frame_size = ctx->audio_codec_ctx->frame_size * ctx->chs;  //max samples that encoder will accept
  ctx->audio_frame_size_variable = (ctx->audio_codec->capabilities & AV_CODEC_CAP_VARIABLE_FRAME_SIZE) != 0;
  ctx->audio_frame->nb_samples = ctx->audio_codec_ctx->frame_size;
  ret = (*_av_frame_get_buffer)(ctx->audio_frame, 0);
  if (ret < 0) {
    printf("av_frame_get_buffer() failed! %d\n", ret);
    return JNI_FALSE;
  }
  if (!ctx->audio_frame_size_variable) {
    ctx->audio_buffer = (short*)malloc(ctx->audio_frame_size * 2);
    ctx->audio_buffer_size = 0;
  }
  //copy params
  printf("codec_type=%d\r\n", ctx->audio_codec_ctx->codec_type);
  ret = (*_avcodec_parameters_from_context)(ctx->audio_stream->codecpar, ctx->audio_codec_ctx);
  if (ret < 0) {
    printf("avcodec_parameters_from_context() failed!\n");
    return JNI_FALSE;
  }
  if (ctx->audio_codec_ctx->sample_fmt == AV_SAMPLE_FMT_S16) {
    return JNI_TRUE;
  }
  //create audio conversion context
  if (libav_org)
    ctx->swr_ctx = (*_avresample_alloc_context)();
  else
    ctx->swr_ctx = (*_swr_alloc)();
  if (libav_org) {
    (*_av_opt_set_int)(ctx->swr_ctx, "in_channel_count",      ctx->chs, 0);
    (*_av_opt_set_int)(ctx->swr_ctx, "in_sample_rate",        ctx->freq, 0);
    (*_av_opt_set_sample_fmt)(ctx->swr_ctx, "in_sample_fmt",  AV_SAMPLE_FMT_S16, 0);
    (*_av_opt_set_int)(ctx->swr_ctx, "out_channel_count",     ctx->chs, 0);
    (*_av_opt_set_int)(ctx->swr_ctx, "out_sample_rate",       ctx->freq, 0);
    (*_av_opt_set_sample_fmt)(ctx->swr_ctx, "out_sample_fmt", ctx->audio_codec_ctx->sample_fmt, 0);
  } else {
    ctx->swr_ctx = (*_swr_alloc_set_opts)(ctx->swr_ctx,
      ctx->audio_codec_ctx->channel_layout, ctx->audio_codec_ctx->sample_fmt, ctx->freq,
      ctx->audio_codec_ctx->channel_layout, AV_SAMPLE_FMT_S16, ctx->freq,
      0, NULL);
  }

  printf("conversion:%d to %d\n", AV_SAMPLE_FMT_S16, ctx->audio_codec_ctx->sample_fmt);
  if (libav_org)
    ret = (*_avresample_open)(ctx->swr_ctx);
  else
    ret = (*_swr_init)(ctx->swr_ctx);
  if (ret < 0) {
    printf("resample init failed:%d\n", ret);
  }
  return JNI_TRUE;
}

//libav.org does not provide this function : easy to implement
//see http://ffmpeg.org/doxygen/trunk/mux_8c_source.html#l00148
static AVFormatContext *_avformat_alloc_output_context2(const char *codec) {
  AVFormatContext *fmt_ctx = (*_avformat_alloc_context)();
  fmt_ctx->oformat = (*_av_guess_format)(codec, NULL, NULL);
  if (fmt_ctx->oformat == NULL) {
    printf("av_guess_format() failed! (codec=%s)\n", codec);
    return NULL;
  }
  if (fmt_ctx->oformat->priv_data_size > 0) {
    fmt_ctx->priv_data = (*_av_mallocz)(fmt_ctx->oformat->priv_data_size);
    if (fmt_ctx->oformat->priv_class != NULL) {
      *(const AVClass**)fmt_ctx->priv_data = fmt_ctx->oformat->priv_class;
      (*_av_opt_set_defaults)(fmt_ctx->priv_data);
    }
  } else {
    fmt_ctx->priv_data = NULL;
  }
  return fmt_ctx;
}

static jboolean encoder_start(FFContext *ctx, const char *codec, jboolean doVideo, jboolean doAudio, void*read, void*write, void*seek) {
  ctx->fmt_ctx = _avformat_alloc_output_context2(codec);
  if (ctx->fmt_ctx == NULL) {
    printf("Error:Unable to find codec:%s\n", codec);
    return JNI_FALSE;
  }
  ctx->ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ctx->ff_buffer, ffiobufsiz, 1, (void*)ctx, read, write, seek);
  if (ctx->io_ctx == NULL) return JNI_FALSE;
  ctx->fmt_ctx->pb = ctx->io_ctx;
  ctx->out_fmt = ctx->fmt_ctx->oformat;
  if ((ctx->out_fmt->video_codec != AV_CODEC_ID_NONE) && doVideo) {
    if (!add_stream(ctx, ctx->out_fmt->video_codec)) {
      printf("add_stream:video failed!\n");
      return JNI_FALSE;
    }
  }
  if ((ctx->out_fmt->audio_codec != AV_CODEC_ID_NONE) && doAudio) {
    if (!add_stream(ctx, ctx->out_fmt->audio_codec)) {
      printf("add_stream:audio failed!\n");
      return JNI_FALSE;
    }
  }
  if (ctx->video_stream != NULL) {
    if (!open_video(ctx)) {
      printf("open_video failed!\n");
      return JNI_FALSE;
    }
  }
  if (ctx->audio_stream != NULL) {
    if (!open_audio(ctx)) {
      printf("open_audio failed!\n");
      return JNI_FALSE;
    }
  }
  int ret = (*_avformat_write_header)(ctx->fmt_ctx, NULL);
  if (ret < 0) {
    printf("avformat_write_header failed! %d\n", ret);
  }
  if (ctx->audio_frame != NULL) {
    ctx->audio_pts = 0;
  }
  if (ctx->video_frame != NULL) {
    ctx->video_pts = 0;
  }
  (*_av_dump_format)(ctx->fmt_ctx, 0, "dump.avi", 1);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_start
  (JNIEnv *e, jobject c, jobject mio, jint width, jint height, jint fps, jint chs, jint freq, jstring codec, jboolean doVideo, jboolean doAudio)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  if (doVideo && (width <= 0 || height <= 0)) {
    return JNI_FALSE;
  }
  if (doAudio && (chs <= 0 || freq <= 0)) {
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

  ctx->fps_1000_1001 = e->GetBooleanField(c, fid_fps_1000_1001);
  ctx->config_gop_size = e->GetIntField(c, fid_framesPerKeyFrame);
  ctx->config_video_bit_rate = e->GetIntField(c, fid_videoBitRate);
  ctx->config_audio_bit_rate = e->GetIntField(c, fid_audioBitRate);
  ctx->width = width;
  ctx->height = height;
  ctx->fps = fps;
  ctx->chs = chs;
  ctx->freq = freq;

  const char *ccodec = e->GetStringUTFChars(codec, NULL);
  jboolean ret = encoder_start(ctx, ccodec, doVideo, doAudio, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  e->ReleaseStringUTFChars(codec, ccodec);

  return ret;
}

static jboolean addAudioFrame(FFContext *ctx, short *sams, int offset, int length)
{
  int nb_samples = length / ctx->chs;
  int buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, AV_SAMPLE_FMT_S16, 0);
  void* samples_data = (*_av_mallocz)(buffer_size);
  //copy sams -> samples_data
  memcpy(samples_data, sams + offset, length * 2);
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
//  printf("buffer:%d %d %d\n", buffer_size, nb_samples, length);

  if (ctx->swr_ctx != NULL) {
    //convert sample format (some codecs do not support S16)
    //sample rate is not changed
    if (((*_av_samples_alloc)(ctx->audio_dst_data, ctx->audio_dst_linesize, ctx->chs
      , nb_samples, ctx->audio_codec_ctx->sample_fmt, 0)) < 0)
    {
      printf("av_samples_alloc failed!\n");
      return JNI_FALSE;
    }
    ctx->audio_src_data[0] = (uint8_t*)samples_data;
    int ret;
    if (libav_org)
      ret = (*_avresample_convert)(ctx->swr_ctx, ctx->audio_dst_data, 0, nb_samples
        , ctx->audio_src_data, 0, nb_samples);
    else
      ret = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, nb_samples
        , ctx->audio_src_data, nb_samples);
//    printf("convert = %d\n", ret);
  } else {
    ctx->audio_dst_data[0] = (uint8_t*)samples_data;
  }
  ctx->audio_frame->nb_samples = nb_samples;
  buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, ctx->audio_codec_ctx->sample_fmt, 0);
  (*_av_frame_make_writable)(ctx->audio_frame);  //ensure we can write to it now
  int res = (*_avcodec_fill_audio_frame)(ctx->audio_frame, ctx->chs, ctx->audio_codec_ctx->sample_fmt, ctx->audio_dst_data[0]
    , buffer_size, 0);
  if (res < 0) {
    printf("avcodec_fill_audio_frame() failed:%d\n", res);
    return JNI_FALSE;
  }
  int got_frame = 0;
  ctx->audio_frame->pts = (*_av_rescale_q)(ctx->audio_pts, ctx->audio_ratio, ctx->audio_codec_ctx->time_base);
  int ret = (*_avcodec_encode_audio2)(ctx->audio_codec_ctx, pkt, ctx->audio_frame, &got_frame);
  if (ret < 0) {
    printf("avcodec_encode_audio2() failed!%d\n", ret);
    return JNI_FALSE;
  }
  if (got_frame && pkt->size > 0) {
    pkt->stream_index = ctx->audio_stream->index;
    (*_av_packet_rescale_ts)(pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
//printf("audio : write_frame() : %lld, %lld, %d, %d, %d\n", pkt->pts, pkt->dts, pkt->duration, pkt->stream_index, pkt->size);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
    if (ret < 0) {
      printf("av_interleaved_write_frame() failed!\n");
      return JNI_FALSE;
    }
  }
  (*_av_free_packet)(pkt);
  (*_av_free)(pkt);
  (*_av_free)(samples_data);
  if (ctx->swr_ctx != NULL) {
    //free audio_dst_data (only the first pointer : regardless if format was plannar : it's alloced as one large block)
    if (ctx->audio_dst_data[0] != NULL) {
      (*_av_free)(ctx->audio_dst_data[0]);
      ctx->audio_dst_data[0] = NULL;
    }
  }
  ctx->audio_pts += nb_samples;
  return ret == 0;
}

static jboolean addAudio(FFContext *ctx, short *sams, int offset, int length) {
  jboolean ok = JNI_TRUE;

  int frame_size = length;
  if (!ctx->audio_frame_size_variable) {
    frame_size = ctx->audio_frame_size;
    if (ctx->audio_buffer_size > 0) {
      printf("warning : filling partial frame\n");
      //fill audio_buffer with input samples
      int size = ctx->audio_frame_size - ctx->audio_buffer_size;
      if (size > length) size = length;
      memcpy(ctx->audio_buffer + ctx->audio_buffer_size, sams + offset, size * 2);
      ctx->audio_buffer_size += size;
      if (ctx->audio_buffer_size < ctx->audio_frame_size) return JNI_TRUE;  //frame still not full
      addAudioFrame(ctx, ctx->audio_buffer, 0, ctx->audio_buffer_size);
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
      printf("save partial frame\n");
      memcpy(ctx->audio_buffer, sams + offset, size * 2);
      ctx->audio_buffer_size = size;
      return JNI_TRUE;
    }
    if (!addAudioFrame(ctx, sams, offset, size)) {
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

  if (ctx->audio_codec_ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jshort* sams_ptr = (jshort*)e->GET_SHORT_ARRAY(sams, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = addAudio(ctx, sams_ptr, offset, length);

  e->RELEASE_SHORT_ARRAY(sams, sams_ptr, JNI_ABORT);

  return ok;
}

static jboolean addVideo(FFContext *ctx, int *px)
{
  int length = ctx->width * ctx->height * 4;
  if (ctx->video_codec_ctx->pix_fmt != AV_PIX_FMT_BGRA) {
    //copy px -> ctx->src_pic->data[0];
    memcpy(ctx->src_pic->data[0], px, length);
    (*_sws_scale)(ctx->sws_ctx, ctx->src_pic->data, ctx->src_pic->linesize, 0, ctx->video_codec_ctx->height
      , ctx->dst_pic->data, ctx->dst_pic->linesize);
  } else {
    //copy px -> ctx->dst_pic->data[0];
    memcpy(ctx->dst_pic->data[0], px, length);
  }
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);

  int got_frame = 0;
  ctx->video_frame->pts = ctx->video_pts;
  int ret = (*_avcodec_encode_video2)(ctx->video_codec_ctx, pkt, ctx->video_frame, &got_frame);
  if (ret < 0) {
    printf("avcodec_encode_video2() failed!\n");
    return JNI_FALSE;
  }

/*
  printf("pts=%lld dts=%lld data=%p size=%d flags=%x side_data=%p, duration=%lld pos=%lld\n"
    ,pkt->pts,pkt->dts,pkt->data,pkt->size,pkt->flags,pkt->side_data,pkt->duration,pkt->pos
  );
  uint8_t* data = pkt->data;
  int size = pkt->size;
  for(int a=0;a<size-4;a++,data++) {
    uint32_t* data32 = (uint32_t*)data;
    if (((*data32) & 0xffffff) == 0x010000) {
      for(int b=0;b<4;b++) {
        printf("%02x ", data[b]);
      }
      printf("...");
    }
  }
  printf(" [%d]\n", size);
*/

  if (got_frame != 0 && pkt->size > 0) {
    pkt->stream_index = ctx->video_stream->index;
    (*_av_packet_rescale_ts)(pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
//printf("video : write_frame() : %lld, %lld, %d, %d\n", pkt->pts, pkt->dts, pkt->duration, pkt->stream_index);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
    (*_av_free_packet)(pkt);
    (*_av_free)(pkt);
    pkt = NULL;
  }
  ctx->video_pts++;
  return ret == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addVideo
  (JNIEnv *e, jobject c, jintArray px)
{
  FFContext *ctx = getFFContext(e,c);

  if (ctx->video_codec_ctx == NULL) return JNI_FALSE;

  jboolean isCopy;
  jint *px_ptr = (jint*)e->GET_INT_ARRAY(px, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = addVideo(ctx, (int*)px_ptr);

  e->RELEASE_INT_ARRAY(px, px_ptr, JNI_ABORT);

  return ok;
}

static jboolean addVideoEncoded(FFContext *ctx, jbyte* data, jint size, jboolean key_frame) {
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
  pkt->data = (uint8_t*)data;
  pkt->size = size;
  pkt->stream_index = ctx->video_stream->index;
  pkt->pts = ctx->video_pts;
  pkt->dts = ctx->video_pts;
  if (key_frame) {
    pkt->flags = AV_PKT_FLAG_KEY;
  }
  (*_av_packet_rescale_ts)(pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);

/*
  printf("pts=%lld dts=%lld data=%p size=%d flags=%x side_data=%p, duration=%lld pos=%lld\n"
    ,pkt->pts,pkt->dts,pkt->data,pkt->size,pkt->flags,pkt->side_data,pkt->duration,pkt->pos
  );
  for(int a=0;a<size-4;a++,data++) {
    uint32_t* data32 = (uint32_t*)data;
    if (((*data32) & 0xffffff) == 0x010000) {
      for(int b=0;b<4;b++) {
        printf("%02x ", data[b]);
      }
      printf("...");
    }
  }
  printf(" [%d]\n", size);
*/

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
  pkt->data = NULL;
  pkt->size = 0;
  (*_av_free_packet)(pkt);
  (*_av_free)(pkt);
  ctx->video_pts++;
  return ret == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaEncoder_addVideoEncoded
  (JNIEnv *e, jobject c, jbyteArray ba, jint offset, jint length, jboolean key_frame)
{
  FFContext *ctx = getFFContext(e,c);

  jboolean isCopy;
  jbyte *ba_ptr = (jbyte*)e->GET_BYTE_ARRAY(ba, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  jboolean ok = addVideoEncoded(ctx, ba_ptr + offset, length, key_frame);

  e->RELEASE_BYTE_ARRAY(ba, ba_ptr, JNI_ABORT);

  return ok;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaEncoder_getAudioFramesize
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx->audio_codec_ctx == NULL) return 0;
  return ctx->audio_codec_ctx->frame_size;
}

static jboolean flush(FFContext *ctx) {
  if (ctx->audio_frame == NULL) return JNI_FALSE;
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);

  int got_frame = 0;
  int ret = (*_avcodec_encode_audio2)(ctx->audio_codec_ctx, pkt, NULL, &got_frame);
  if (ret < 0) {
    printf("avcodec_encode_audio2() failed!\n");
    return JNI_FALSE;
  }
  if (got_frame != 0 && pkt->size > 0) {
    pkt->stream_index = ctx->audio_stream->index;
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
    (*_av_free_packet)(pkt);
    (*_av_free)(pkt);
    pkt = NULL;
    if (ret < 0) printf("av_interleaved_write_frame() failed!\n");
    return ret == 0;
  }
  return JNI_FALSE;
}

static void encoder_stop(FFContext *ctx)
{
  //flush audio encoder
  while (flush(ctx)) {}
  int ret = (*_av_write_trailer)(ctx->fmt_ctx);
  if (ret < 0) {
    printf("av_write_trailer() failed! %d\n", ret);
  }
  if (ctx->io_ctx != NULL) {
    (*_avio_flush)(ctx->io_ctx);
    ctx->io_ctx = NULL;
    ctx->ff_buffer = NULL;
  }
  if (ctx->audio_stream != NULL) {
    (*_avcodec_close)(ctx->audio_codec_ctx);
    ctx->audio_stream = NULL;
  }
  if (ctx->video_stream != NULL) {
    (*_avcodec_close)(ctx->video_codec_ctx);
    ctx->video_stream = NULL;
  }
  if (ctx->audio_frame != NULL) {
    (*_av_frame_free)((void**)&ctx->audio_frame);
  }
  if (ctx->video_frame != NULL) {
    (*_av_frame_free)((void**)&ctx->video_frame);
  }
  if (ctx->fmt_ctx != NULL) {
    (*_avformat_free_context)(ctx->fmt_ctx);
    ctx->fmt_ctx = NULL;
  }
  if (ctx->src_pic != NULL) {
    (*_av_frame_free)((void**)&ctx->src_pic);
  }
  if (ctx->dst_pic != NULL) {
    (*_av_frame_free)((void**)&ctx->dst_pic);
  }
  if (ctx->sws_ctx != NULL) {
    (*_sws_freeContext)(ctx->sws_ctx);
    ctx->sws_ctx = NULL;
  }
  if (ctx->swr_ctx != NULL) {
    if (libav_org)
      (*_avresample_free)(&ctx->swr_ctx);
    else
      (*_swr_free)(&ctx->swr_ctx);
    ctx->swr_ctx = NULL;
  }
  if (ctx->audio_buffer != NULL) {
    free(ctx->audio_buffer);
    ctx->audio_buffer = NULL;
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
