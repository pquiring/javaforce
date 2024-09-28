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

#include "register.h"

//returned by Decoder.read()
#define END_FRAME -1
#define NULL_FRAME 0  //could be metadata frame
#define AUDIO_FRAME 1
#define VIDEO_FRAME 2

static jboolean ffmpeg_loaded = JNI_FALSE;

static jboolean ff_debug_log = JNI_FALSE;
static jboolean ff_debug_trace = JNI_TRUE;
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
int (*_av_find_best_stream)(AVFormatContext *ic,int type,int wanted_stream_nb,int related_stream, void** decoder_ret, int flags);
AVIOContext* (*_avio_alloc_context)(void* buffer,int buffer_size,int write_flag,void* opaque, void* read,void* write,void* seek);
AVFormatContext* (*_avformat_alloc_context)();
int (*_avformat_alloc_output_context2)(AVFormatContext** fmt_ctx, AVOutputFormat* ofmt, const char* name, const char* filename);
int (*_avio_open)(AVIOContext** io_ctx, const char* file, int flags);
int (*_avio_close)(void* ctx);
void (*_avformat_free_context)(AVFormatContext *s);
int (*_avformat_open_input)(void** ps,const char* filename,void* fmt,void* options);
int (*_avformat_find_stream_info)(AVFormatContext *ic,void** options);
int (*_av_read_frame)(AVFormatContext *s,AVPacket *pkt);
AVInputFormat* (*_av_find_input_format)(const char* name);
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
int (*_avformat_query_codec)(const AVOutputFormat *ofmt, int codec_id, int std_compliance);

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

//swresample functions (audio resample)
void* (*_swr_alloc)();
int (*_swr_alloc_set_opts2)(void**, AVChannelLayout* out_ch_layout, int out_sample_fmt, int out_sample_rate, AVChannelLayout* in_ch_layout, int in_sample_fmt, int in_sample_rate, int log_offset, void*log_ctx);
int (*_swr_init)(void* ctx);
int64_t (*_swr_get_delay)(void* ctx,int64_t base);
int (*_swr_convert)(void* ctx,uint8_t* out_arg[],int out_count,uint8_t* in_arg[],int in_count);
void (*_swr_free)(void** ctx);

//swscale functions (video rescale)
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

static void AVPacket_dump(AVPacket *pkt, const char* msg) {
  printf("AVPacket:%s:pts=%llx dts=%llx duration=%lld pos=%lld time_base=%d/%d\n", msg, pkt->pts, pkt->dts, pkt->duration, pkt->pos, pkt->time_base.num, pkt->time_base.den);
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
  getFunction(format, (void**)&_avio_open, "avio_open");
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
  getFunction(format, (void**)&_avformat_query_codec, "avformat_query_codec");

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

#include "ff_context.h"

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

//include code bases

#include "ff_format.cpp"

#include "ff_decoder.cpp"

#include "ff_input.cpp"

#include "ff_av_decoder.cpp"

#include "ff_encoder.cpp"

#include "ff_output.cpp"

#include "ff_av_encoder.cpp"

//JNI registration

static JNINativeMethod javaforce_media_MediaCoder[] = {
  {"ninit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_media_MediaCoder_ninit},
  {"setLogging", "(Z)V", (void *)&Java_javaforce_media_MediaCoder_setLogging},
};

static JNINativeMethod javaforce_media_MediaFormat[] = {
  {"ngetVideoStream", "(J)I", (void *)&Java_javaforce_media_MediaFormat_ngetVideoStream},
  {"ngetAudioStream", "(J)I", (void *)&Java_javaforce_media_MediaFormat_ngetAudioStream},
  {"ngetVideoCodecID", "(J)I", (void *)&Java_javaforce_media_MediaFormat_ngetVideoCodecID},
  {"ngetAudioCodecID", "(J)I", (void *)&Java_javaforce_media_MediaFormat_ngetAudioCodecID},
  {"ngetVideoBitRate", "(J)I", (void *)&Java_javaforce_media_MediaFormat_ngetVideoBitRate},
  {"ngetAudioBitRate", "(J)I", (void *)&Java_javaforce_media_MediaFormat_ngetAudioBitRate},
};

static JNINativeMethod javaforce_media_MediaDecoder[] = {
  {"start", "(Ljavaforce/media/MediaIO;IIIIZ)Z", (void *)&Java_javaforce_media_MediaDecoder_start},
  {"startFile", "(Ljava/lang/String;Ljava/lang/String;IIII)Z", (void *)&Java_javaforce_media_MediaDecoder_startFile},
  {"stop", "()V", (void *)&Java_javaforce_media_MediaDecoder_stop},
  {"read", "()I", (void *)&Java_javaforce_media_MediaDecoder_read},
  {"getVideo", "()[I", (void *)&Java_javaforce_media_MediaDecoder_getVideo},
  {"getAudio", "()[S", (void *)&Java_javaforce_media_MediaDecoder_getAudio},
  {"getWidth", "()I", (void *)&Java_javaforce_media_MediaDecoder_getWidth},
  {"getHeight", "()I", (void *)&Java_javaforce_media_MediaDecoder_getHeight},
  {"getFrameRate", "()F", (void *)&Java_javaforce_media_MediaDecoder_getFrameRate},
  {"getDuration", "()J", (void *)&Java_javaforce_media_MediaDecoder_getDuration},
  {"getSampleRate", "()I", (void *)&Java_javaforce_media_MediaDecoder_getSampleRate},
  {"getChannels", "()I", (void *)&Java_javaforce_media_MediaDecoder_getChannels},
  {"getBitsPerSample", "()I", (void *)&Java_javaforce_media_MediaDecoder_getBitsPerSample},
  {"seek", "(J)Z", (void *)&Java_javaforce_media_MediaDecoder_seek},
  {"getVideoBitRate", "()I", (void *)&Java_javaforce_media_MediaDecoder_getVideoBitRate},
  {"getAudioBitRate", "()I", (void *)&Java_javaforce_media_MediaDecoder_getAudioBitRate},
  {"isKeyFrame", "()Z", (void *)&Java_javaforce_media_MediaDecoder_isKeyFrame},
  {"resize", "(II)Z", (void *)&Java_javaforce_media_MediaDecoder_resize},
};

static JNINativeMethod javaforce_media_MediaEncoder[] = {
  {"nstart", "(Ljavaforce/media/MediaIO;IIIIILjava/lang/String;II)Z", (void *)&Java_javaforce_media_MediaEncoder_nstart},
  {"nstartFile", "(Ljava/lang/String;IIIIILjava/lang/String;II)Z", (void *)&Java_javaforce_media_MediaEncoder_nstartFile},
  {"addAudio", "([SII)Z", (void *)&Java_javaforce_media_MediaEncoder_addAudio},
  {"addVideo", "([I)Z", (void *)&Java_javaforce_media_MediaEncoder_addVideo},
  {"getAudioFramesize", "()I", (void *)&Java_javaforce_media_MediaEncoder_getAudioFramesize},
  {"addAudioEncoded", "([BII)Z", (void *)&Java_javaforce_media_MediaEncoder_addAudioEncoded},
  {"addVideoEncoded", "([BIIZ)Z", (void *)&Java_javaforce_media_MediaEncoder_addVideoEncoded},
  {"stop", "()V", (void *)&Java_javaforce_media_MediaEncoder_stop},
  {"flush", "()V", (void *)&Java_javaforce_media_MediaEncoder_flush},
};

static JNINativeMethod javaforce_media_MediaInput[] = {
  {"nopenFile", "(Ljava/lang/String;Ljava/lang/String;)J", (void *)&Java_javaforce_media_MediaInput_nopenFile},
  {"nopenIO", "(Ljavaforce/media/MediaIO;)J", (void *)&Java_javaforce_media_MediaInput_nopenIO},
  {"ngetDuration", "(J)J", (void *)&Java_javaforce_media_MediaInput_ngetDuration},
  {"ngetVideoWidth", "(J)I", (void *)&Java_javaforce_media_MediaInput_ngetVideoWidth},
  {"ngetVideoHeight", "(J)I", (void *)&Java_javaforce_media_MediaInput_ngetVideoHeight},
  {"ngetVideoFrameRate", "(J)F", (void *)&Java_javaforce_media_MediaInput_ngetVideoFrameRate},
  {"ngetVideoKeyFrameInterval", "(J)I", (void *)&Java_javaforce_media_MediaInput_ngetVideoKeyFrameInterval},
  {"ngetAudioChannels", "(J)I", (void *)&Java_javaforce_media_MediaInput_ngetAudioChannels},
  {"ngetAudioSampleRate", "(J)I", (void *)&Java_javaforce_media_MediaInput_ngetAudioSampleRate},
  {"nclose", "(J)Z", (void *)&Java_javaforce_media_MediaInput_nclose},
  {"nopenvideo", "(JII)Z", (void *)&Java_javaforce_media_MediaInput_nopenvideo},
  {"nopenaudio", "(JII)Z", (void *)&Java_javaforce_media_MediaInput_nopenaudio},
  {"nread", "(J)I", (void *)&Java_javaforce_media_MediaInput_nread},
  {"ngetPacketKeyFrame", "(J)Z", (void *)&Java_javaforce_media_MediaInput_ngetPacketKeyFrame},
  {"ngetPacketData", "(J[BII)I", (void *)&Java_javaforce_media_MediaInput_ngetPacketData},
  {"nseek", "(JJ)Z", (void *)&Java_javaforce_media_MediaInput_nseek},
};

static JNINativeMethod javaforce_media_MediaOutput[] = {
  {"ncreateFile", "(Ljava/lang/String;Ljava/lang/String;)J", (void *)&Java_javaforce_media_MediaOutput_ncreateFile},
  {"ncreateIO", "(Ljavaforce/media/MediaIO;Ljava/lang/String;)J", (void *)&Java_javaforce_media_MediaOutput_ncreateIO},
  {"naddVideoStream", "(JIIIIFI)I", (void *)&Java_javaforce_media_MediaOutput_naddVideoStream},
  {"naddAudioStream", "(JIIII)I", (void *)&Java_javaforce_media_MediaOutput_naddAudioStream},
  {"nclose", "(J)Z", (void *)&Java_javaforce_media_MediaOutput_nclose},
  {"nwriteHeader", "(J)Z", (void *)&Java_javaforce_media_MediaOutput_nwriteHeader},
  {"nwritePacket", "(JI[BIIZ)Z", (void *)&Java_javaforce_media_MediaOutput_nwritePacket},
};

static JNINativeMethod javaforce_media_MediaAudioDecoder[] = {
  {"nstart", "(III)J", (void *)&Java_javaforce_media_MediaAudioDecoder_nstart},
  {"nstop", "(J)V", (void *)&Java_javaforce_media_MediaAudioDecoder_nstop},
  {"ndecode", "(J[BII)[S", (void *)&Java_javaforce_media_MediaAudioDecoder_ndecode},
  {"ngetChannels", "(J)I", (void *)&Java_javaforce_media_MediaAudioDecoder_ngetChannels},
  {"ngetSampleRate", "(J)I", (void *)&Java_javaforce_media_MediaAudioDecoder_ngetSampleRate},
};

static JNINativeMethod javaforce_media_MediaVideoDecoder[] = {
  {"nstart", "(III)J", (void *)&Java_javaforce_media_MediaVideoDecoder_nstart},
  {"nstop", "(J)V", (void *)&Java_javaforce_media_MediaVideoDecoder_nstop},
  {"ndecode", "(J[BII)[I", (void *)&Java_javaforce_media_MediaVideoDecoder_ndecode},
  {"ngetWidth", "(J)I", (void *)&Java_javaforce_media_MediaVideoDecoder_ngetWidth},
  {"ngetHeight", "(J)I", (void *)&Java_javaforce_media_MediaVideoDecoder_ngetHeight},
  {"ngetFrameRate", "(J)F", (void *)&Java_javaforce_media_MediaVideoDecoder_ngetFrameRate},
};

static JNINativeMethod javaforce_media_MediaAudioEncoder[] = {
  {"nstart", "(IIII)J", (void *)&Java_javaforce_media_MediaAudioEncoder_nstart},
  {"nstop", "(J)V", (void *)&Java_javaforce_media_MediaAudioEncoder_nstop},
  {"nencode", "(J[SII)[B", (void *)&Java_javaforce_media_MediaAudioEncoder_nencode},
  {"ngetAudioFramesize", "(J)I", (void *)&Java_javaforce_media_MediaAudioEncoder_ngetAudioFramesize},
};

static JNINativeMethod javaforce_media_MediaVideoEncoder[] = {
  {"nstart", "(IIIIFI)J", (void *)&Java_javaforce_media_MediaVideoEncoder_nstart},
  {"nstop", "(J)V", (void *)&Java_javaforce_media_MediaVideoEncoder_nstop},
  {"nencode", "(J[III)[B", (void *)&Java_javaforce_media_MediaVideoEncoder_nencode},
};

extern "C" void ffmpeg_register(JNIEnv *env);

void ffmpeg_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/media/MediaCoder");
  registerNatives(env, cls, javaforce_media_MediaCoder, sizeof(javaforce_media_MediaCoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaFormat");
  registerNatives(env, cls, javaforce_media_MediaFormat, sizeof(javaforce_media_MediaFormat)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaDecoder");
  registerNatives(env, cls, javaforce_media_MediaDecoder, sizeof(javaforce_media_MediaDecoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaEncoder");
  registerNatives(env, cls, javaforce_media_MediaEncoder, sizeof(javaforce_media_MediaEncoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaInput");
  registerNatives(env, cls, javaforce_media_MediaInput, sizeof(javaforce_media_MediaInput)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaOutput");
  registerNatives(env, cls, javaforce_media_MediaOutput, sizeof(javaforce_media_MediaOutput)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaAudioDecoder");
  registerNatives(env, cls, javaforce_media_MediaAudioDecoder, sizeof(javaforce_media_MediaAudioDecoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaVideoDecoder");
  registerNatives(env, cls, javaforce_media_MediaVideoDecoder, sizeof(javaforce_media_MediaVideoDecoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaAudioEncoder");
  registerNatives(env, cls, javaforce_media_MediaAudioEncoder, sizeof(javaforce_media_MediaAudioEncoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaVideoEncoder");
  registerNatives(env, cls, javaforce_media_MediaVideoEncoder, sizeof(javaforce_media_MediaVideoEncoder)/sizeof(JNINativeMethod));
}
