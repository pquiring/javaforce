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

JNIEXPORT jlong JNICALL Java_javaforce_voip_speex_speexdspinit
  (JNIEnv *e, jclass o, jint sample_rate)
{
  return 0;
}

JNIEXPORT void JNICALL Java_javaforce_voip_speex_speexdspuninit
  (JNIEnv *e, jclass o, jlong ctx)
{
}

JNIEXPORT void JNICALL Java_javaforce_voip_speex_speexdspdenoise
  (JNIEnv *e, jclass o, jlong ctx, jshortArray audio)
{
}

JNIEXPORT void JNICALL Java_javaforce_voip_speex_speexdspecho
  (JNIEnv *e, jclass o, jlong ctx, jshortArray audio_out, jshortArray audio_mic)
{
}

static JNINativeMethod javaforce_voip_speex[] = {
  {"speexdspinit", "(I)J", (void *)&Java_javaforce_voip_speex_speexdspinit},
  {"speexdspuninit", "(J)V", (void *)&Java_javaforce_voip_speex_speexdspuninit},
  {"speexdspdenoise", "(J[S)V", (void *)&Java_javaforce_voip_speex_speexdspdenoise},
  {"speexdspecho", "(J[S[S)V", (void *)&Java_javaforce_voip_speex_speexdspecho},
};

extern "C" void speex_dsp_register(JNIEnv *env);

void speex_dsp_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/voip/speex");
  registerNatives(env, cls, javaforce_voip_speex, sizeof(javaforce_voip_speex)/sizeof(JNINativeMethod));
}
