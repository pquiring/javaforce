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

DSP* speexCreate(jint sample_rate, jint echo_buffers) {
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

  return ctx;
}

void speexFree(DSP* ctx) {
  if (ctx == NULL) return;
  speex_preprocess_state_destroy(ctx->pps);
  speex_echo_state_destroy(ctx->es);
}

void speexDenoise(DSP* ctx, jshort* audio, jint length) {
  if (ctx == NULL) return;
  speex_preprocess_run(ctx->pps, audio);
}

void speexEcho(DSP* ctx, jshort* audio_mic, jshort* audio_spk, jshort* audio_out, jint length) {
  if (ctx == NULL) return;
  speex_echo_cancellation(ctx->es, audio_mic, audio_spk, audio_out);
  speex_preprocess_run(ctx->pps, audio_out);
}

extern "C" {
  JNIEXPORT DSP* (*_speexCreate)(jint,jint) = &speexCreate;
  JNIEXPORT void (*_speexFree)(DSP*) = &speexFree;
  JNIEXPORT void (*_speexDenoise)(DSP*,jshort*,jint) = &speexDenoise;
  JNIEXPORT void (*_speexEcho)(DSP*,jshort*,jshort*,jshort*,jint) = &speexEcho;

  JNIEXPORT jboolean SpeexAPIinit() {return JNI_TRUE;}
}
