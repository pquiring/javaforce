//FFMPEG

//Requires FFMPEG/5.1.x or better

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/channel_layout.h>
#include <libavutil/mathematics.h>
#include <libavutil/timestamp.h>
#include <libavutil/opt.h>
#include <libswscale/swscale.h>

#include <chrono>

//returned by Decoder.read()
#define END_FRAME -1
#define NULL_FRAME 0  //could be metadata frame
#define AUDIO_FRAME 1
#define VIDEO_FRAME 2

static jboolean ffmpeg_loaded = JNI_FALSE;

static jboolean ff_debug_log = JNI_FALSE;
static jboolean ff_debug_trace = JNI_FALSE;
static jboolean ff_debug_buffer = JNI_FALSE;
static jboolean ff_debug_box = JNI_FALSE;
static jboolean ff_debug_time_base = JNI_FALSE;

JF_LIB_HANDLE codec = NULL;
JF_LIB_HANDLE device = NULL;
JF_LIB_HANDLE ffilter = NULL;
JF_LIB_HANDLE format = NULL;
JF_LIB_HANDLE util = NULL;
JF_LIB_HANDLE resample = NULL;
JF_LIB_HANDLE postproc = NULL;
JF_LIB_HANDLE scale = NULL;
jboolean shownCopyWarning = JNI_FALSE;

static AVChannelLayout channel_layout_1;
static AVChannelLayout channel_layout_2;
static AVChannelLayout channel_layout_4;

static void copyWarning() {
  printf("Warning : JNI::Get*ArrayElements returned a copy : Performance will be degraded!\n");
  shownCopyWarning = JNI_TRUE;
}

//avcodec functions
AVCodec* (*_avcodec_find_decoder)(int codec_id);
int (*_avcodec_open2)(AVCodecContext *avctx,AVCodec *codec,void* options);
AVCodecContext* (*_avcodec_alloc_context3)(AVCodec *codec);
void (*_avcodec_free_context)(AVCodecContext **ctx);
void (*_av_init_packet)(AVPacket *pkt);
void (*_av_packet_free)(AVPacket **pkt);
void (*_av_packet_free_side_data)(AVPacket *pkt);
//encoding
AVCodec* (*_avcodec_find_encoder)(int codec_id);
//int (*_avpicture_alloc)(AVPicture *pic, int pix_fmt, int width, int height);
//int (*_avpicture_free)(AVPicture *pic);
int (*_avcodec_fill_audio_frame)(AVFrame *frame, int nb_channels, int fmt, void* buf, int bufsize, int align);
//int (*_avcodec_close)(AVCodecContext *cc);
const char* (*_avcodec_get_name)(AVCodecID id);
void (*_av_packet_rescale_ts)(AVPacket *pkt, AVRational src, AVRational dst);
int (*_avcodec_parameters_to_context)(AVCodecContext *ctx, const AVCodecParameters *par);
int (*_avcodec_parameters_from_context)(AVCodecParameters *par, const AVCodecContext *ctx);
int (*_avcodec_version)();
//encoding
int (*_avcodec_send_frame)(AVCodecContext *ctx, const AVFrame *frame);
int (*_avcodec_receive_packet)(AVCodecContext *ctx, const AVPacket *pkt);
//decoding
int (*_avcodec_send_packet)(AVCodecContext *ctx, const AVPacket *pkt);
int (*_avcodec_receive_frame)(AVCodecContext *ctx, const AVFrame *frame);

//avdevice functions

//avfilter functions

//avformat functions
AVOutputFormat* (*_av_guess_format)(const char* shortName, const char* fileName, const char* mimeType);
int (*_av_find_best_stream)(AVFormatContext *ic,int type,int wanted_stream_nb,int related_stream
  ,void** decoder_ret, int flags);
AVIOContext* (*_avio_alloc_context)(void* buffer,int buffer_size,int write_flag,void* opaque
  ,void* read,void* write,void* seek);
AVFormatContext* (*_avformat_alloc_context)();
int (*_avformat_alloc_output_context2)(AVFormatContext** fmt_ctx, AVOutputFormat* ofmt, const char* name, const char* filename);
int (*_avio_close)(void* ctx);
void (*_avformat_free_context)(AVFormatContext *s);
int (*_avformat_open_input)(void** ps,const char* filename,void* fmt,void* options);
int (*_avformat_find_stream_info)(AVFormatContext *ic,void** options);
int (*_av_read_frame)(AVFormatContext *s,AVPacket *pkt);
void* (*_av_find_input_format)(const char* name);
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
int (*_avformat_version)();

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
void* (*_av_dict_get)(AVDictionary* dict, const char* key, void* prev, int flags);
int (*_av_dict_set)(AVDictionary** dictref, const char* key, const char* value, int flags);
int (*_av_frame_make_writable)(AVFrame *frame);
int (*_av_compare_ts)(int64_t ts_a, AVRational tb_a, int64_t ts_b, AVRational tb_b);
int (*_av_frame_get_buffer)(AVFrame *frame, int align);
AVFrame* (*_av_frame_alloc)();
void (*_av_frame_free)(void** frame);
void (*_av_buffer_unref)(AVBufferRef **buf);
int (*_av_channel_layout_copy) (AVChannelLayout* dst, const AVChannelLayout* src);
int (*_avutil_version)();

//swresample functions (ffmpeg.org)
void* (*_swr_alloc)();
int (*_swr_alloc_set_opts2)(void**, AVChannelLayout* out_ch_layout, int out_sample_fmt, int out_sample_rate, AVChannelLayout* in_ch_layout, int in_sample_fmt, int in_sample_rate, int log_offset, void*log_ctx);
int (*_swr_init)(void* ctx);
int64_t (*_swr_get_delay)(void* ctx,int64_t base);
int (*_swr_convert)(void* ctx,uint8_t* out_arg[],int out_count,uint8_t* in_arg[],int in_count);
void (*_swr_free)(void** ctx);

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

static AVOutputFormat *vpx = NULL;

//av_free_packet is deprecated : easy to implement
static void _av_free_packet(AVPacket *pkt) {
  if (pkt) {
    if (pkt->buf) (*_av_buffer_unref)(&pkt->buf);
    pkt->data = NULL;
    pkt->size = 0;
    (*_av_packet_free_side_data)(pkt);
  }
}

static void log_packet(const char* type, const AVFormatContext *fmt_ctx, const AVPacket *pkt)
{
  AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;

  char pts[AV_TS_MAX_STRING_SIZE];
  char pts_tb[AV_TS_MAX_STRING_SIZE];
  char dts[AV_TS_MAX_STRING_SIZE];
  char dts_tb[AV_TS_MAX_STRING_SIZE];
  char dur[AV_TS_MAX_STRING_SIZE];
  char dur_tb[AV_TS_MAX_STRING_SIZE];

  printf("%s:pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s\n", type,
    av_ts_make_string(pts, pkt->pts), av_ts_make_time_string(pts_tb, pkt->pts, time_base),
    av_ts_make_string(dts, pkt->dts), av_ts_make_time_string(dts_tb, pkt->dts, time_base),
    av_ts_make_string(dur, pkt->duration), av_ts_make_time_string(dur_tb, pkt->duration, time_base)
  );
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
  printf("ffmpeg init...");

  util = loadLibrary(utilFile);
  if (util == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), utilFile);
    return JNI_FALSE;
  }

  resample = loadLibrary(resampleFile);
  if (resample == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), resampleFile);
    return JNI_FALSE;
  }

  scale = loadLibrary(scaleFile);
  if (scale == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), scaleFile);
    return JNI_FALSE;
  }

  postproc = loadLibrary(postFile);
  if (postproc == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), postFile);
    return JNI_FALSE;
  }

  codec = loadLibrary(codecFile);
  if (codec == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), codecFile);
    return JNI_FALSE;
  }

  format = loadLibrary(formatFile);
  if (format == NULL)  {
    printf("Could not find(0x%x):%s\n", GetLastError(), formatFile);
    return JNI_FALSE;
  }

  ffilter = loadLibrary(filterFile);
  if (ffilter == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), filterFile);
    return JNI_FALSE;
  }

  device = loadLibrary(deviceFile);
  if (device == NULL) {
    printf("Could not find(0x%x):%s\n", GetLastError(), deviceFile);
    return JNI_FALSE;
  }

  //get functions
  getFunction(codec, (void**)&_avcodec_find_decoder, "avcodec_find_decoder");
  getFunction(codec, (void**)&_avcodec_open2, "avcodec_open2");
  getFunction(codec, (void**)&_avcodec_alloc_context3, "avcodec_alloc_context3");
  getFunction(codec, (void**)&_avcodec_free_context, "avcodec_free_context");
  getFunction(codec, (void**)&_av_init_packet, "av_init_packet");
  getFunction(codec, (void**)&_av_packet_free, "av_packet_free");
  getFunction(codec, (void**)&_av_packet_free_side_data, "av_packet_free_side_data");
  getFunction(codec, (void**)&_avcodec_find_encoder, "avcodec_find_encoder");
//  getFunction(codec, (void**)&_avpicture_alloc, "avpicture_alloc");
//  getFunction(codec, (void**)&_avpicture_free, "avpicture_free");
  getFunction(codec, (void**)&_avcodec_fill_audio_frame, "avcodec_fill_audio_frame");
//  getFunction(codec, (void**)&_avcodec_close, "avcodec_close");
  getFunction(codec, (void**)&_avcodec_get_name, "avcodec_get_name");  //for debug output only
  getFunction(codec, (void**)&_av_packet_rescale_ts, "av_packet_rescale_ts");
  getFunction(codec, (void**)&_avcodec_parameters_to_context, "avcodec_parameters_to_context");
  getFunction(codec, (void**)&_avcodec_parameters_from_context, "avcodec_parameters_from_context");
  getFunction(codec, (void**)&_avcodec_version, "avcodec_version");
  getFunction(codec, (void**)&_avcodec_send_frame, "avcodec_send_frame");
  getFunction(codec, (void**)&_avcodec_receive_packet, "avcodec_receive_packet");
  getFunction(codec, (void**)&_avcodec_send_packet, "avcodec_send_packet");
  getFunction(codec, (void**)&_avcodec_receive_frame, "avcodec_receive_frame");

  getFunction(format, (void**)&_av_guess_format, "av_guess_format");
  getFunction(format, (void**)&_av_find_best_stream, "av_find_best_stream");
  getFunction(format, (void**)&_avio_alloc_context, "avio_alloc_context");
  getFunction(format, (void**)&_avformat_alloc_context, "avformat_alloc_context");
  getFunction(format, (void**)&_avformat_alloc_output_context2, "avformat_alloc_output_context2");
  getFunction(format, (void**)&_avio_close, "avio_close");
  getFunction(format, (void**)&_avformat_free_context, "avformat_free_context");
  getFunction(format, (void**)&_avformat_open_input, "avformat_open_input");
  getFunction(format, (void**)&_avformat_find_stream_info, "avformat_find_stream_info");
  getFunction(format, (void**)&_av_read_frame, "av_read_frame");
  getFunction(format, (void**)&_av_find_input_format, "av_find_input_format");
  getFunction(format, (void**)&_avformat_seek_file, "avformat_seek_file");
  getFunction(format, (void**)&_av_seek_frame, "av_seek_frame");
  getFunction(format, (void**)&_avformat_new_stream, "avformat_new_stream");
  getFunction(format, (void**)&_avformat_write_header, "avformat_write_header");
  getFunction(format, (void**)&_av_interleaved_write_frame, "av_interleaved_write_frame");
  getFunction(format, (void**)&_av_write_frame, "av_write_frame");
  getFunction(format, (void**)&_avio_flush, "avio_flush");
  getFunction(format, (void**)&_av_dump_format, "av_dump_format");
  getFunction(format, (void**)&_av_write_trailer, "av_write_trailer");
  getFunction(format, (void**)&_avformat_version, "avformat_version");

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
  getFunction(util, (void**)&_av_buffer_unref, "av_buffer_unref");
  getFunction(util, (void**)&_av_channel_layout_copy, "av_channel_layout_copy");
  getFunction(util, (void**)&_avutil_version, "avutil_version");

  getFunction(scale, (void**)&_sws_getContext, "sws_getContext");
  getFunction(scale, (void**)&_sws_scale, "sws_scale");
  getFunction(scale, (void**)&_sws_freeContext, "sws_freeContext");

  getFunction(resample, (void**)&_swr_alloc, "swr_alloc");
  getFunction(resample, (void**)&_swr_alloc_set_opts2, "swr_alloc_set_opts2");
  getFunction(resample, (void**)&_swr_init, "swr_init");
  getFunction(resample, (void**)&_swr_get_delay, "swr_get_delay");
  getFunction(resample, (void**)&_swr_convert, "swr_convert");
  getFunction(resample, (void**)&_swr_free, "swr_free");

  //print version info
  union {
    int v32;
    struct {
      unsigned char micro, minor, major;
    } v8;
  } version;

  //enable debugging
  if (ff_debug_log) {
    (*_av_log_set_level)(AV_LOG_TRACE);
  }

  //setup AVChannelLayout's to avoid using designated initializers
  channel_layout_1.order = AV_CHANNEL_ORDER_NATIVE;
  channel_layout_1.nb_channels = 1;
  channel_layout_1.u.mask = AV_CH_LAYOUT_MONO;
  channel_layout_1.opaque = NULL;

  channel_layout_2.order = AV_CHANNEL_ORDER_NATIVE;
  channel_layout_2.nb_channels = 2;
  channel_layout_2.u.mask = AV_CH_LAYOUT_STEREO;
  channel_layout_2.opaque = NULL;

  channel_layout_4.order = AV_CHANNEL_ORDER_NATIVE;
  channel_layout_4.nb_channels = 4;
  channel_layout_4.u.mask = AV_CH_LAYOUT_QUAD;
  channel_layout_4.opaque = NULL;

  printf("ok\r\n");

#ifdef _JF_DEBUG
  version.v32 = (*_avutil_version)();
  printf("avutil_version=%d.%d.%d\n", version.v8.major, version.v8.minor, version.v8.micro);
  version.v32 = (*_avcodec_version)();
  printf("avcodec_version=%d.%d.%d\n", version.v8.major, version.v8.minor, version.v8.micro);
  version.v32 = (*_avformat_version)();
  printf("avformat_version=%d.%d.%d\n", version.v8.major, version.v8.minor, version.v8.micro);
#endif

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaCoder_ninit
  (JNIEnv *e, jclass c, jstring jcodec, jstring jdevice, jstring jfilter, jstring jformat, jstring jutil, jstring jresample, jstring jpostproc, jstring jscale)
{
  if (ffmpeg_loaded) return ffmpeg_loaded;

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

  ffmpeg_loaded = JNI_TRUE;

  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaCoder_setLogging
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
  jmethodID mid_ff_read;
  jmethodID mid_ff_write;
  jmethodID mid_ff_seek;

  //decoder fields
  jobject mio;
  AVFormatContext *fmt_ctx;
  AVIOContext *io_ctx;

  void* input_fmt;

  AVCodecContext *codec_ctx;  //returned by open_codec_context()

  int video_stream_idx;
  AVStream *video_stream;
  AVCodecContext *video_codec_ctx;
  void* sws_ctx;
  uint8_t* decode_buffer;
  int decode_buffer_size;
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
  jboolean pkt_key_frame;

  AVFrame *frame;

  jintArray jvideo;
  jshortArray jvideo16;
  int jvideo_length;

  jshortArray jaudio;
  int jaudio_length;

  //additional raw video decoder fields
  AVCodec *video_codec;

  //additional encoder fields
  AVCodec *audio_codec;
  AVOutputFormat *out_fmt;
  int width, height, fps;
  int chs, freq;
  jboolean scaleVideo;
  int org_width, org_height;
  AVFrame *audio_frame, *video_frame;
  AVFrame *src_pic;
  jboolean audio_frame_size_variable;
  jboolean video_delay;
  int audio_frame_size;
  short *audio_buffer;
  int audio_buffer_size;
  int64_t audio_pts;
  int64_t video_pts;

  int64_t last_dts;
  int64_t last_pts;

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
};

#define ffiobufsiz (64 * 1024)

FFContext* createFFContext(JNIEnv *e, jobject c) {
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
  (*_av_free)(ctx);
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

typedef struct mp4_box {
  int size;
  char type[4];
} mp4_box;

static int swap_order(int val) {
  union {
    int i32;
    char i8[4];
  } be, le;
  be.i32 = val;
  le.i8[0] = be.i8[3];
  le.i8[1] = be.i8[2];
  le.i8[2] = be.i8[1];
  le.i8[3] = be.i8[0];
  return le.i32;
}

static int write_packet(FFContext *ctx, void*buf, int size) {
  jbyteArray ba = ctx->e->NewByteArray(size);
  ctx->e->SetByteArrayRegion(ba, 0, size, (jbyte*)buf);
  int write = ctx->e->CallIntMethod(ctx->mio, ctx->mid_ff_write, ctx->c, ba);  //obj, methodID, args[]
  if (ctx->e->ExceptionCheck()) ctx->e->ExceptionClear();
  ctx->e->DeleteLocalRef(ba);
  if (ctx->is_mp4 && ff_debug_box) {
    int pkt_len = size;
    char* buf8 = (char*)buf;
    while (pkt_len > 0) {
      mp4_box* box = (mp4_box*)buf8;
      int box_size = swap_order(box->size);
      if (box_size <= 0) break;  //mid packet?
      printf("box:%c%c%c%c:%d\n", box->type[0], box->type[1], box->type[2], box->type[3], box_size);
      buf8 += box_size;
      pkt_len -= box_size;
    }
  }
  if (ctx->is_mp4 && ff_debug_buffer) {
    char* chbuf = (char*)buf;
    int len = size;
    if (len > 1024) len = 1024;
    printf("buf = [");
    for(int a=0;a<len;a++) {
      char ch = chbuf[a];
      if (ch < 32 || ch > 127) {
        printf("{%02x}", ch & 0xff);
      } else {
        printf("%c", ch);
      }
    }
    if (size > 1024) {
      printf("{...}");
    }
    printf("]\n");
  }
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

  void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
  ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 0, (void*)ctx, (void*)&read_packet, (void*)&write_packet, seekable ? (void*)&seek_packet : NULL);
  if (ctx->io_ctx == NULL) return JNI_FALSE;
//  ctx->io_ctx->direct = 1;
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
  return decoder_open_codecs(ctx, new_width, new_height, new_chs, new_freq);
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

//rawvideo decoder codebase

JNIEXPORT jboolean JNICALL Java_javaforce_media_MediaVideoDecoder_start
  (JNIEnv *e, jobject c, jint codec_id, jint width, jint height)
{
  FFContext *ctx = createFFContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
//  printf("context=%p\n", ctx);
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

  ctx->width = width;
  ctx->height = height;

  ctx->decode_buffer = (uint8_t*)(*_av_malloc)(1024*1024);
  ctx->decode_buffer_size = 1024*1024;

  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaVideoDecoder_stop
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return;
  if (ctx->frame != NULL) {
    (*_av_frame_free)((void**)&ctx->frame);
    ctx->frame = NULL;
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
  if (ctx->jvideo != NULL) {
    e->DeleteGlobalRef(ctx->jvideo);
    ctx->jvideo = NULL;
  }
  if (ctx->jvideo16 != NULL) {
    e->DeleteGlobalRef(ctx->jvideo16);
    ctx->jvideo16 = NULL;
  }
  if (ctx->decode_buffer != NULL) {
    (*_av_free)(ctx->decode_buffer);
    ctx->decode_buffer = NULL;
    ctx->decode_buffer_size = 0;
  }
  deleteFFContext(e,c,ctx);
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_MediaVideoDecoder_decode
  (JNIEnv *e, jobject c, jbyteArray data, jint offset, jint length)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return NULL;
  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  //there should always be 64 bytes after the data to decode
  if (length + 64 > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + 64 > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }
  memcpy(ctx->decode_buffer, dataptr + offset, length);
  uint8_t *pad = dataptr + offset + length;
  for(int a=0;a<64;a++) {
    *(pad++) = 0;
  }

  ctx->pkt->size = length;
  ctx->pkt->data = ctx->decode_buffer;

  int ret = (*_avcodec_send_packet)(ctx->video_codec_ctx, ctx->pkt);
  e->ReleasePrimitiveArrayCritical(data, (jbyte*)dataptr, JNI_ABORT);
  ctx->pkt->data = NULL;
  if (ret < 0) {
    printf("Error:avcodec_send_packet() == %d\n", ret);
    ctx->pkt->size = 0;
    return NULL;
  }

  while (1) {
    ret = (*_avcodec_receive_frame)(ctx->video_codec_ctx, ctx->frame);
    if (ret < 0) break;

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
      //create video conversion context
      ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
        , ctx->width, ctx->height, AV_PIX_FMT_BGRA
        , SWS_BILINEAR, NULL, NULL, NULL);

      int px_count = ctx->width * ctx->height;
      ctx->jvideo_length = px_count;
      ctx->jvideo = (jintArray)ctx->e->NewGlobalRef(ctx->e->NewIntArray(ctx->jvideo_length));
    }

    jint *jvideo_ptr = (jint*)ctx->e->GetPrimitiveArrayCritical(ctx->jvideo, &isCopy);
    if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

    ctx->rgb_video_dst_data[0] = (uint8_t*)jvideo_ptr;
    ctx->rgb_video_dst_linesize[0] = ctx->width * 4;
    (*_sws_scale)(ctx->sws_ctx, ctx->frame->data, ctx->frame->linesize, 0, ctx->video_codec_ctx->height
      , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);

    ctx->e->ReleasePrimitiveArrayCritical(ctx->jvideo, jvideo_ptr, JNI_COMMIT);
  }

  return ctx->jvideo;
}

JNIEXPORT jshortArray JNICALL Java_javaforce_media_MediaVideoDecoder_decode16
  (JNIEnv *e, jobject c, jbyteArray data, jint offset, jint length)
{
  int64_t p_start = currentTimeMillis();
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return NULL;
  jboolean isCopy;
  uint8_t *dataptr = (uint8_t*)(jbyte*)e->GetPrimitiveArrayCritical(data, &isCopy);
  if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

  //there should always be 64 bytes after the data to decode
  if (length + 64 > ctx->decode_buffer_size) {
    (*_av_free)(ctx->decode_buffer);
    while (length + 64 > ctx->decode_buffer_size) {
      ctx->decode_buffer_size <<= 1;
    }
    ctx->decode_buffer = (uint8_t*)(*_av_malloc)(ctx->decode_buffer_size);
  }
  memcpy(ctx->decode_buffer, dataptr + offset, length);
  uint8_t *pad = dataptr + offset + length;
  for(int a=0;a<64;a++) {
    *(pad++) = 0;
  }

  ctx->pkt->size = length;
  ctx->pkt->data = ctx->decode_buffer;

  int ret = (*_avcodec_send_packet)(ctx->video_codec_ctx, ctx->pkt);
  e->ReleasePrimitiveArrayCritical(data, (jbyte*)dataptr, JNI_ABORT);
  ctx->pkt->data = NULL;
  if (ret < 0) {
    printf("Error:avcodec_send_packet() == %d\n", ret);
    ctx->pkt->size = 0;
    return NULL;
  }

  while (1) {
    ret = (*_avcodec_receive_frame)(ctx->video_codec_ctx, ctx->frame);
    if (ret < 0) break;

    //setup conversion once width/height are known
    if (ctx->jvideo16 == NULL) {
      if (ctx->video_codec_ctx->width == 0 || ctx->video_codec_ctx->height == 0) {
        printf("MediaVideoDecoder : width/height not known yet\n");
        return NULL;
      }
      if (ctx->width == -1 && ctx->height == -1) {
        ctx->width = ctx->video_codec_ctx->width;
        ctx->height = ctx->video_codec_ctx->height;
      }
      //create video conversion context
      ctx->sws_ctx = (*_sws_getContext)(ctx->video_codec_ctx->width, ctx->video_codec_ctx->height, ctx->video_codec_ctx->pix_fmt
        , ctx->width, ctx->height, AV_PIX_FMT_BGR555
        , SWS_BILINEAR, NULL, NULL, NULL);

      int px_count = ctx->width * ctx->height;
      ctx->jvideo_length = px_count;
      ctx->jvideo16 = (jshortArray)ctx->e->NewGlobalRef(ctx->e->NewShortArray(ctx->jvideo_length));
    }

    jshort *jvideo_ptr = (jshort*)ctx->e->GetPrimitiveArrayCritical(ctx->jvideo16, &isCopy);
    if (!shownCopyWarning && isCopy == JNI_TRUE) copyWarning();

    ctx->rgb_video_dst_data[0] = (uint8_t*)jvideo_ptr;
    ctx->rgb_video_dst_linesize[0] = ctx->width * 2;
    (*_sws_scale)(ctx->sws_ctx, ctx->frame->data, ctx->frame->linesize, 0, ctx->video_codec_ctx->height
      , ctx->rgb_video_dst_data, ctx->rgb_video_dst_linesize);

    ctx->e->ReleasePrimitiveArrayCritical(ctx->jvideo16, jvideo_ptr, JNI_COMMIT);
  }

  return ctx->jvideo16;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaVideoDecoder_getWidth
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_MediaVideoDecoder_getHeight
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0;
  if (ctx->video_codec_ctx == NULL) return 0;
  return ctx->video_codec_ctx->height;
}

JNIEXPORT jfloat JNICALL Java_javaforce_media_MediaVideoDecoder_getFrameRate
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return 0.0f;
  if (ctx->video_codec_ctx == NULL) return 0;
  if (ctx->video_codec_ctx->framerate.den == 0) return 0;
  return ctx->video_codec_ctx->framerate.num / ctx->video_codec_ctx->framerate.den;
}

//encoder codebase

static jboolean encoder_add_stream(FFContext *ctx, int codec_id) {
  printf("encoder_add_stream\n");
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
      printf("Unknown/Unsupported stream:%d\n", codec->type);
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
  if (ret < 0) return JNI_FALSE;

  if (ff_debug_trace) printf("encoder_init_video:params:%p:%p\n", ctx->video_stream->codecpar, ctx->video_codec_ctx);
  //copy params (after codec is opened)
  ret = (*_avcodec_parameters_from_context)(ctx->video_stream->codecpar, ctx->video_codec_ctx);
  if (ret < 0) {
    printf("avcodec_parameters_from_context() failed!\n");
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
      printf("av_frame_get_buffer() failed! %d\n", ret);
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
    printf("av_frame_alloc() failed!\n");
    return JNI_FALSE;
  }
  ctx->video_frame->width = ctx->width;
  ctx->video_frame->height = ctx->height;
  ctx->video_frame->format = ctx->video_codec_ctx->pix_fmt;
  if (ff_debug_trace) printf("encoder_init_video\n");
  ret = (*_av_frame_get_buffer)(ctx->video_frame, get_size_alignment(ctx->width, ctx->height));
  if (ret < 0) {
    printf("av_frame_get_buffer() failed! %d\n", ret);
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

static jboolean encoder_init_audio(FFContext *ctx) {
  printf("encoder_init_audio:codec_ctx=%p:codec=%p:stream=%p\n", ctx->audio_codec_ctx, ctx->audio_codec, ctx->audio_stream);

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
  ctx->audio_stream->time_base.num = 1;
  ctx->audio_stream->time_base.den = ctx->freq;

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
        ctx->audio_stream->time_base.num = 1;
        ctx->audio_stream->time_base.den = 48000;
      }
      break;
    }
    default: {
      printf("Unknown audio codec:0x%x\n", ctx->audio_codec_ctx->codec_id);
      break;
    }
  }
  if (ctx->config_compressionLevel != -1) {
    ctx->video_codec_ctx->compression_level = ctx->config_compressionLevel;
  }

  //open audio codec
  int ret = (*_avcodec_open2)(ctx->audio_codec_ctx, ctx->audio_codec, NULL);
  if (ret < 0) {
    printf("avcodec_open2() failed!\n");
    return JNI_FALSE;
  }

  //copy params (after codec is opened)
  ret = (*_avcodec_parameters_from_context)(ctx->audio_stream->codecpar, ctx->audio_codec_ctx);
  if (ret < 0) {
    printf("avcodec_parameters_from_context() failed!\n");
    return JNI_FALSE;
  }

  ctx->audio_frame = (*_av_frame_alloc)();
  if (ctx->audio_frame == NULL) {
    printf("av_frame_alloc() failed!\n");
    return JNI_FALSE;
  }
  ctx->audio_frame->format = ctx->audio_codec_ctx->sample_fmt;
  ctx->audio_frame->sample_rate = ctx->freq;
  (*_av_channel_layout_copy)(&ctx->audio_frame->ch_layout, &ctx->audio_codec_ctx->ch_layout);
  ctx->audio_frame_size = ctx->audio_codec_ctx->frame_size * ctx->chs;  //max samples that encoder will accept
  ctx->audio_frame_size_variable = (ctx->audio_codec->capabilities & AV_CODEC_CAP_VARIABLE_FRAME_SIZE) != 0;
  ctx->audio_frame->nb_samples = ctx->audio_codec_ctx->frame_size;
  ret = (*_av_frame_get_buffer)(ctx->audio_frame, 0);
  if (ret < 0) {
    printf("av_frame_get_buffer() failed! %d\n", ret);
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
    printf("resample init failed:%d\n", ret);
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

static jboolean encoder_start(FFContext *ctx, const char *codec, jboolean doVideo, jboolean doAudio, void*read, void*write, void*seek) {
  printf("context2=%p\n", _avformat_alloc_output_context2);
  (*_avformat_alloc_output_context2)(&ctx->fmt_ctx, NULL, codec, NULL);
  if (ctx->fmt_ctx == NULL) {
    printf("Error:Unable to find codec:%s\n", codec);
    return JNI_FALSE;
  }
  printf("encoder_start:fmt_ctx=%p:out_fmt=%p\n", ctx->fmt_ctx, ctx->fmt_ctx->oformat);
  if (strcmp(codec, "dash") == 0) {
    ctx->is_dash = 1;
  }
  else if (strcmp(codec, "mp4") == 0) {
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
    void *ff_buffer = (*_av_mallocz)(ffiobufsiz);
    ctx->io_ctx = (*_avio_alloc_context)(ff_buffer, ffiobufsiz, 1, (void*)ctx, read, write, seek);
    if (ctx->io_ctx == NULL) return JNI_FALSE;
  //  ctx->io_ctx->direct = 1;
    printf("io_ctx=%p\n", ctx->io_ctx);
    ctx->fmt_ctx->pb = ctx->io_ctx;
  }
  ctx->fmt_ctx->io_open = &io_open;
  ctx->fmt_ctx->io_close2 = &io_close2;
  ctx->fmt_ctx->opaque = ctx;
  ctx->out_fmt = (AVOutputFormat*)ctx->fmt_ctx->oformat;
  if (ff_debug_trace) printf("encoder_start\n");
  if ((ctx->out_fmt->video_codec != AV_CODEC_ID_NONE) && doVideo) {
    if (!encoder_add_stream(ctx, ctx->out_fmt->video_codec)) {
      printf("encoder_add_stream:video failed!\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if ((ctx->out_fmt->audio_codec != AV_CODEC_ID_NONE) && doAudio) {
    if (!encoder_add_stream(ctx, ctx->out_fmt->audio_codec)) {
      printf("encoder_add_stream:audio failed!\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if (ctx->video_stream != NULL) {
    if (!encoder_init_video(ctx)) {
      printf("encoder_init_video failed!\n");
      return JNI_FALSE;
    }
  }
  if (ff_debug_trace) printf("encoder_start\n");
  if (ctx->audio_stream != NULL) {
    if (!encoder_init_audio(ctx)) {
      printf("encoder_init_audio failed!\n");
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
    printf("avformat_write_header failed! %d\n", ret);
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

  const char *ccodec = e->GetStringUTFChars(codec, NULL);
  jboolean ret = encoder_start(ctx, ccodec, doVideo, doAudio, (void*)&read_packet, (void*)&write_packet, (void*)&seek_packet);
  e->ReleaseStringUTFChars(codec, ccodec);

  return ret;
}

static jboolean encoder_addAudioFrame(FFContext *ctx, short *sams, int offset, int length)
{
  int nb_samples = length / ctx->chs;
  int buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, AV_SAMPLE_FMT_S16, 0);
  void* samples_data = (*_av_mallocz)(buffer_size);
  //copy sams -> samples_data
  memcpy(samples_data, sams + offset, length * 2);
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);

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
    ret = (*_swr_convert)(ctx->swr_ctx, ctx->audio_dst_data, nb_samples
      , ctx->audio_src_data, nb_samples);
  } else {
    ctx->audio_dst_data[0] = (uint8_t*)samples_data;
  }
  (*_av_frame_make_writable)(ctx->audio_frame);  //ensure we can write to it now
  ctx->audio_frame->nb_samples = nb_samples;
  buffer_size = (*_av_samples_get_buffer_size)(NULL, ctx->chs, nb_samples, ctx->audio_codec_ctx->sample_fmt, 0);
  int res = (*_avcodec_fill_audio_frame)(ctx->audio_frame, ctx->chs, ctx->audio_codec_ctx->sample_fmt, ctx->audio_dst_data[0]
    , buffer_size, 0);
  if (res < 0) {
    printf("avcodec_fill_audio_frame() failed:%d\n", res);
    return JNI_FALSE;
  }
  ctx->audio_frame->pts = ctx->audio_pts;  //(*_av_rescale_q)(ctx->audio_pts, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  int ret = (*_avcodec_send_frame)(ctx->audio_codec_ctx, ctx->audio_frame);
  if (ret < 0) {
    printf("avcodec_send_frame() failed!%d\n", ret);
    return JNI_FALSE;
  }
  while (1) {
    ret = (*_avcodec_receive_packet)(ctx->audio_codec_ctx, pkt);
    if (ret < 0) break;
    pkt->stream_index = ctx->audio_stream->index;
    (*_av_packet_rescale_ts)(pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
    ctx->last_dts = pkt->dts;
    ctx->last_pts = pkt->pts;
//    log_packet("audio", ctx->fmt_ctx, pkt);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
    if (ret < 0) {
      printf("av_interleaved_write_frame() failed!\n");
    }
  }
  (*_av_packet_free)(&pkt);
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
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
  ctx->video_frame->pts = ctx->video_pts;  //(*_av_rescale_q)(ctx->video_pts, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
  int ret = (*_avcodec_send_frame)(ctx->video_codec_ctx, ctx->video_frame);
  if (ret < 0) {
    printf("avcodec_send_frame() failed!\n");
    return JNI_FALSE;
  }

  while (1) {
    ret = (*_avcodec_receive_packet)(ctx->video_codec_ctx, pkt);
    if (ret < 0) break;
    pkt->stream_index = ctx->video_stream->index;
    (*_av_packet_rescale_ts)(pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
//    printf("packet:%lld/%lld/%lld\n", pkt->dts, pkt->pts, pkt->duration);
    ctx->last_dts = pkt->dts;
    ctx->last_pts = pkt->pts;
//    log_packet("video", ctx->fmt_ctx, pkt);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
    if (ret < 0) {
      printf("av_interleaved_write_frame() failed!\n");
    }
  }
  (*_av_packet_free)(&pkt);
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
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
  pkt->data = (uint8_t*)data;
  pkt->size = size;
  pkt->stream_index = ctx->audio_stream->index;

  if (ts) {
    pkt->dts = dts;
    pkt->pts = pts;
  } else {
    pkt->pts = ctx->audio_pts;  //(*_av_rescale_q)(ctx->audio_pts, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
    pkt->dts = pkt->pts;
    (*_av_packet_rescale_ts)(pkt, ctx->audio_codec_ctx->time_base, ctx->audio_stream->time_base);
  }

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
  pkt->data = NULL;
  pkt->size = 0;
  (*_av_packet_free)(&pkt);
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
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
  pkt->data = (uint8_t*)data;
  pkt->size = size;
  pkt->stream_index = ctx->video_stream->index;
  if (key_frame) {
    pkt->flags = AV_PKT_FLAG_KEY;
  }

  if (ts) {
    pkt->dts = dts;
    pkt->pts = pts;
  } else {
    pkt->pts = ctx->video_pts;  //(*_av_rescale_q)(ctx->video_pts, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
    pkt->dts = pkt->pts;
    (*_av_packet_rescale_ts)(pkt, ctx->video_codec_ctx->time_base, ctx->video_stream->time_base);
//    printf("packet:%lld/%lld/%lld\n", pkt->dts, pkt->pts, pkt->duration);
  }

  int ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
  pkt->data = NULL;
  pkt->size = 0;
  (*_av_packet_free)(&pkt);
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
  AVPacket *pkt = AVPacket_New();
  (*_av_init_packet)(pkt);
  int ret;

  //signal end of input
  if (endOfStream) {
    ret = (*_avcodec_send_frame)(codec_ctx, NULL);
    if (ret < 0) {
      printf("avcodec_send_frame() failed!\n");
      return JNI_FALSE;
    }
  }
  while (1) {
    ret = (*_avcodec_receive_packet)(codec_ctx, pkt);
    if (ret < 0) break;
    pkt->stream_index = stream->index;
    (*_av_packet_rescale_ts)(pkt, codec_ctx->time_base, stream->time_base);
    ret = (*_av_interleaved_write_frame)(ctx->fmt_ctx, pkt);
    if (ret < 0) {
      printf("av_interleaved_write_frame() failed!\n");
    }
  }
  (*_av_packet_free)(&pkt);
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
    printf("av_write_trailer() failed! %d\n", ret);
  }
  if (ff_debug_trace) printf("encoder_stop\n");
  if (ctx->io_ctx != NULL) {
    (*_avio_flush)(ctx->io_ctx);
    (*_av_free)(ctx->io_ctx->buffer);
    (*_av_free)(ctx->io_ctx);
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
}

JNIEXPORT void JNICALL Java_javaforce_media_MediaEncoder_stop
  (JNIEnv *e, jobject c)
{
  FFContext *ctx = getFFContext(e,c);
  if (ctx == NULL) return;
  encoder_stop(ctx);
  deleteFFContext(e,c,ctx);
}
