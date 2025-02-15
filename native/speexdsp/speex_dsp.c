#define HAVE_CONFIG_H

#include "buffer.c"
#include "fftwrap.c"
#include "jitter.c"
#include "kiss_fft.c"
#include "kiss_fftr.c"
#include "mdf.c"
#include "preprocess.c"
#include "scal.c"
#include "smallft.c"
#include "filterbank.c"

struct DSP {
  SpeexPreprocessState *pps;
  SpeexEchoState *es;
};

JNIEXPORT jlong JNICALL Java_javaforce_voip_codec_speex_speexdspinit
  (JNIEnv *e, jclass o, jint sample_rate, jint echo_buffers)
{
  int i;
  float f;
  int bufsiz = sample_rate / 50;

  if (echo_buffers == -1) echo_buffers = 10;

  DSP *ctx = (DSP*)malloc(sizeof(DSP));

  SpeexPreprocessState *pps = speex_preprocess_state_init(bufsiz, sample_rate);
  i=1;
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_DENOISE, &i);
#ifndef FIXED_POINT
  i=0;
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_AGC, &i);
  i=8000;
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_AGC_LEVEL, &i);
#endif
  i=0;
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_DEREVERB, &i);
  f=0;
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_DEREVERB_DECAY, &f);
  f=0;
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_DEREVERB_LEVEL, &f);

  SpeexEchoState *es = speex_echo_state_init(bufsiz, bufsiz * echo_buffers);
  speex_echo_ctl(es, SPEEX_ECHO_SET_SAMPLING_RATE, &sample_rate);
  speex_preprocess_ctl(pps, SPEEX_PREPROCESS_SET_ECHO_STATE, es);

  ctx->pps = pps;
  ctx->es = es;
  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_voip_codec_speex_speexdspuninit
  (JNIEnv *e, jclass o, jlong ctx)
{
  if (ctx == 0) return;
  DSP *c_ctx = (DSP*)ctx;
  speex_preprocess_state_destroy(c_ctx->pps);
  speex_echo_state_destroy(c_ctx->es);
}

JNIEXPORT void JNICALL Java_javaforce_voip_codec_speex_speexdspdenoise
  (JNIEnv *e, jclass o, jlong ctx, jshortArray audio)
{
  if (ctx == 0) {
    printf("speex_dsp_denoise() called with null ctx");
    return;
  }
  DSP *c_ctx = (DSP*)ctx;
  jshort* c_audio = (jshort*)e->GetPrimitiveArrayCritical(audio, NULL);
  speex_preprocess_run(c_ctx->pps, c_audio);
  e->ReleasePrimitiveArrayCritical(audio, c_audio, 0);
}

JNIEXPORT void JNICALL Java_javaforce_voip_codec_speex_speexdspecho
  (JNIEnv *e, jclass o, jlong ctx, jshortArray audio_mic, jshortArray audio_spk, jshortArray audio_out)
{
  if (ctx == 0) {
    printf("speex_dsp_echo() called with null ctx");
    return;
  }
  DSP *c_ctx = (DSP*)ctx;
  jshort* c_audio_mic = (jshort*)e->GetPrimitiveArrayCritical(audio_mic, NULL);
  jshort* c_audio_spk = (jshort*)e->GetPrimitiveArrayCritical(audio_spk, NULL);
  jshort* c_audio_out = (jshort*)e->GetPrimitiveArrayCritical(audio_out, NULL);
  speex_echo_cancellation(c_ctx->es, c_audio_mic, c_audio_spk, c_audio_out);
  speex_preprocess_run(c_ctx->pps, c_audio_out);
  e->ReleasePrimitiveArrayCritical(audio_mic, c_audio_mic, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(audio_spk, c_audio_spk, JNI_ABORT);
  e->ReleasePrimitiveArrayCritical(audio_out, c_audio_out, 0);
}

static JNINativeMethod javaforce_voip_codec_speex[] = {
  {"speexdspinit", "(II)J", (void *)&Java_javaforce_voip_codec_speex_speexdspinit},
  {"speexdspuninit", "(J)V", (void *)&Java_javaforce_voip_codec_speex_speexdspuninit},
  {"speexdspdenoise", "(J[S)V", (void *)&Java_javaforce_voip_codec_speex_speexdspdenoise},
  {"speexdspecho", "(J[S[S[S)V", (void *)&Java_javaforce_voip_codec_speex_speexdspecho},
};

extern "C" void speex_dsp_register(JNIEnv *env);

void speex_dsp_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/voip/codec/speex");
  registerNatives(env, cls, javaforce_voip_codec_speex, sizeof(javaforce_voip_codec_speex)/sizeof(JNINativeMethod));
}
