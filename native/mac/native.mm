#include <Foundation/Foundation.h>
#include <AppKit/AppKit.h>
#include <AVFoundation/AVFoundation.h>

#include <dlfcn.h>
#include <stdlib.h>

//#include <GL/gl.h>

#include <jni.h>

#include "javaforce_jni_JFNative.h"
#include "javaforce_jni_MacNative.h"
#include "javaforce_gl_GL.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"
#include "javaforce_controls_ni_DAQmx.h"
#include "javaforce_ui_Font.h"
#include "javaforce_ui_Image.h"
#include "javaforce_ui_Window.h"
#include "javaforce_cl_CL.h"

#include "../common/library.h"
#include "../common/
h"

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

//OpenGL

#include "../common/glfw.cpp"

#include "../common/gl.cpp"

void *gl;

jboolean glPlatformInit() {
  gl = dlopen("/System/Library/Frameworks/OpenGL.framework/Versions/A/Libraries/OpenGL.dylib", RTLD_LAZY | RTLD_GLOBAL);
  return gl != NULL;
}

jboolean glGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)dlsym(gl, name);
  if (func != NULL) {
    *funcPtr = func;
    return JNI_TRUE;
  } else {
    printf("OpenGL:Error:Can not find function:%s\n", name);
    return JNI_FALSE;
  }
}

//camera API

#define FRAME_BUFFER_SIZE 2

struct CamContext;  //forward decl

@interface JFDelegate : NSObject
{
  @public CamContext *ctx;
}
- (void)captureOutput:(AVCaptureOutput *)captureOutput
  didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
  fromConnection:(AVCaptureConnection *)connection;
@end

struct CamContext {
  NSArray *devices;
  AVCaptureDevice *device;
  AVCaptureDeviceInput *input;
  AVCaptureVideoDataOutput *output;
  JFDelegate *delegate;
  AVCaptureSession *session;
  int width, height;
  int frameIdx;
  void *frames[FRAME_BUFFER_SIZE];
  jboolean haveFrame;

  //to avoid using libstdc++ new/delete must be coded by hand
  static CamContext* New() {
    CamContext *ctx = (CamContext*)malloc(sizeof(CamContext));
    memset(ctx, 0, sizeof(CamContext));
    return ctx;
  }
  void Delete() {
    free(this);
  }
};

@implementation JFDelegate
- (void)captureOutput:(AVCaptureOutput *)captureOutput
  didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
  fromConnection:(AVCaptureConnection *)connection
{
  CMVideoFormatDescriptionRef videoDescRef = CMSampleBufferGetFormatDescription(sampleBuffer);
  CMVideoDimensions videoDim = CMVideoFormatDescriptionGetDimensions(videoDescRef);
  if (ctx->width == 0) {
    ctx->width = videoDim.width;
    ctx->height = videoDim.height;
  }
  CMBlockBufferRef blockBuffer = CMSampleBufferGetDataBuffer(sampleBuffer);
  int idx = ctx->frameIdx;
  int bytes = ctx->width * ctx->height * 4;
  if (ctx->frames[idx] == NULL) {
    ctx->frames[idx] = malloc(bytes);
  }

  void *temp = ctx->frames[idx];
  char *buf = NULL;
  CMBlockBufferAccessDataBytes(blockBuffer, 0, bytes, temp, &buf);

  if (buf != temp) {
    memcpy(temp, buf, bytes);
  }

  idx++;
  if (idx == FRAME_BUFFER_SIZE) idx = 0;
  ctx->frameIdx = idx;
}
@end

CamContext* createCamContext(JNIEnv *e, jobject c) {
  CamContext *ctx;
  ctx = CamContext::New();
  return ctx;
}

void deleteCamContext(JNIEnv *e, jobject c, CamContext *ctx) {
  if (ctx == NULL) return;
  ctx->Delete();
}

JNIEXPORT jlong JNICALL Java_javaforce_media_Camera_cameraInit
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = createCamContext(e,c);
  if (ctx == NULL) return 0;
  return (jlong)ctx;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraUninit
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  deleteCamContext(e,c,ctx);
  return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_media_Camera_cameraListDevices
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return NULL;
  ctx->devices = [[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo]
    arrayByAddingObjectsFromArray:[AVCaptureDevice devicesWithMediaType:AVMediaTypeMuxed]];
  int cnt = [ctx->devices count];

  jclass strcls = e->FindClass("java/lang/String");
  jobjectArray strs = e->NewObjectArray(cnt, strcls, NULL);
  for(int a=0;a<cnt;a++) {
    AVCaptureDevice *device = [ctx->devices objectAtIndex:a];
    e->SetObjectArrayElement(strs, a, e->NewStringUTF([[device localizedName] UTF8String]));
  }
  return strs;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_media_Camera_cameraListModes
  (JNIEnv *e, jobject c, jlong ctxptr, jint deviceIdx)
{
  return NULL;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStart
  (JNIEnv *e, jobject c, jlong ctxptr, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  if (deviceIdx >= [ctx->devices count]) return JNI_FALSE;

  ctx->device = [ctx->devices objectAtIndex:deviceIdx];
  ctx->session = [[AVCaptureSession alloc] init];

  NSError *error = nil;

  ctx->input = [AVCaptureDeviceInput deviceInputWithDevice:ctx->device error:&error];

  if (ctx->input == NULL) {
    printf("AVCatpureDeviceInput deviceInputWithDevice failed!\n");
    return JNI_FALSE;
  }
  [ctx->session addInput:ctx->input];

  ctx->output = [[AVCaptureVideoDataOutput alloc] init];

  ctx->delegate = [[JFDelegate alloc] init];
  ctx->delegate->ctx = ctx;

  dispatch_queue_t queue = dispatch_queue_create("JFQueue", NULL);

  [ctx->output setSampleBufferDelegate:(id <AVCaptureVideoDataOutputSampleBufferDelegate>)ctx->delegate queue:queue];

  [ctx->session addOutput:ctx->output];

#ifdef __APPLE__
  NSDictionary *newSettings = @{ (NSString *)kCVPixelBufferPixelFormatTypeKey : @(kCVPixelFormatType_32BGRA) };
#else
  //this is not valid : just testing compilation...
  NSDictionary *newSettings = [NSDictionary dictionaryWithObject:(id)kCVPixelFormatType_32BGRA forKey:(id)kCVPixelBufferPixelFormatTypeKey];
#endif

  //NOTE : width/height are determined on first frame received (there is no API to determine ahead of time)

  [ctx->session startRunning];

  //TODO : wait till width/height are valid
/*
  while (ctx->width == 0) {
    //sleep???
  };
*/

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;

  if (ctx->session == NULL) return JNI_FALSE;

  [ctx->session stopRunning];

  for(int a=0;a<FRAME_BUFFER_SIZE;a++) {
    if (ctx->frames[a] != NULL) {
      free(ctx->frames[a]);
      ctx->frames[a] = NULL;
    }
  }

  if (ctx->input != NULL) {
    [ctx->input release];
    ctx->input = NULL;
  }

  if (ctx->output != NULL) {
    [ctx->output release];
    ctx->output = NULL;
  }

  if (ctx->session != NULL) {
    [ctx->session release];
    ctx->session = NULL;
  }

  return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_Camera_cameraGetFrame
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return NULL;
//  if (ctx->frames == NULL) return NULL;
  if (!ctx->haveFrame) return NULL;

  int size = ctx->width * ctx->height;
  int idx = ctx->frameIdx;
  idx--;
  if (idx == -1) idx = FRAME_BUFFER_SIZE-1;
  void *px = ctx->frames[idx];
  if (px == NULL) return NULL;

  jintArray jpx = e->NewIntArray(size);
  e->SetIntArrayRegion(jpx, 0, size, (const jint*)px);

  return jpx;
}

JNIEXPORT jint JNICALL Java_javaforce_media_Camera_cameraGetWidth
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return 0;
  return ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_Camera_cameraGetHeight
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext *ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return 0;
  return ctx->height;
}

#include "../common/ffmpeg.cpp"

#include "../common/opencl.cpp"

#include "../common/ni.cpp"

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

static JNINativeMethod javaforce_media_Camera[] = {
  {"cameraInit", "()J", (void *)&Java_javaforce_media_Camera_cameraInit},
  {"cameraUninit", "(J)Z", (void *)&Java_javaforce_media_Camera_cameraUninit},
  {"cameraListDevices", "(J)[Ljava/lang/String;", (void *)&Java_javaforce_media_Camera_cameraListDevices},
  {"cameraListModes", "(JI)[Ljava/lang/String;", (void *)&Java_javaforce_media_Camera_cameraListModes},
  {"cameraStart", "(JIII)Z", (void *)&Java_javaforce_media_Camera_cameraStart},
  {"cameraStop", "(J)Z", (void *)&Java_javaforce_media_Camera_cameraStop},
  {"cameraGetFrame", "(J)[I", (void *)&Java_javaforce_media_Camera_cameraGetFrame},
  {"cameraGetWidth", "(J)I", (void *)&Java_javaforce_media_Camera_cameraGetWidth},
  {"cameraGetHeight", "(J)I", (void *)&Java_javaforce_media_Camera_cameraGetHeight},
};

extern "C" void camera_register(JNIEnv *env);

void camera_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/media/Camera");
  registerNatives(env, cls, javaforce_media_Camera, sizeof(javaforce_media_Camera)/sizeof(JNINativeMethod));
}

#include "../common/register.cpp"
