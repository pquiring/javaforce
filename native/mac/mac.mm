#include <Foundation/Foundation.h>
#include <AppKit/AppKit.h>
#include <AVFoundation/AVFoundation.h>

#include <dlfcn.h>
#include <stdlib.h>

#include <jni.h>

#include "javaforce_jni_MacNative.h"
#include "javaforce_gl_GL.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MacNative_macInit
  (JNIEnv *e, jclass c)
{
  return true;
}

//OpenGL

#include "../common/gl.cpp"

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glInit
  (JNIEnv *e, jobject c)
{
  if (funcs[0].func != NULL) return;  //already done
  void *func;
  void *gl = dlopen("OpenGL.dylib", RTLD_LAZY | RTLD_GLOBAL);
  for(int a=0;a<GL_NO_FUNCS;a++) {
    func = (void*)dlsym(gl, funcs[a].name);
    if (func == NULL) {
      printf("glInit:Error:Can not find function:%s\n", funcs[a].name);
      continue;
    }
    funcs[a].func = func;
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
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  ctx = (CamContext*)e->GetLongField(c, fid_cam_ctx);
  if (ctx != NULL) {
    printf("Camera ctx used twice\n");
    return NULL;
  }
  ctx = CamContext::New();
  e->SetLongField(c,fid_cam_ctx,(jlong)ctx);
  return ctx;
}

CamContext* getCamContext(JNIEnv *e, jobject c) {
  CamContext *ctx;
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  ctx = (CamContext*)e->GetLongField(c, fid_cam_ctx);
  return ctx;
}

void deleteCamContext(JNIEnv *e, jobject c, CamContext *ctx) {
  if (ctx == NULL) return;
  ctx->Delete();
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  e->SetLongField(c,fid_cam_ctx,0);
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraInit
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = createCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraUninit
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  deleteCamContext(e,c,ctx);
  return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_media_Camera_cameraListDevices
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
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

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStart
  (JNIEnv *e, jobject c, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  CamContext *ctx = getCamContext(e,c);
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
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
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
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
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
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return 0;
  return ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_Camera_cameraGetHeight
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return 0;
  return ctx->height;
}

#include "../common/ffmpeg.cpp"
