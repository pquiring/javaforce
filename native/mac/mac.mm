#include <Foundation/Foundation.h>
#include <AppKit/AppKit.h>
#include <AVFoundation/AVFoundation.h>

#include <dlfcn.h>
#include <stdlib.h>

//#include <GL/gl.h>

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

//Java7+ BIN : /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/jre/bin
//Java6 BIN : /System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Commands

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

void *jawt;
jboolean (JNICALL *_JAWT_GetAWT)(JNIEnv *e, JAWT *c) = NULL;

JNIEXPORT jboolean JNICALL Java_javaforce_jni_MacNative_macInit
  (JNIEnv *e, jclass c)
{
  jawt = dlopen("jawt.dylib", RTLD_LAZY | RTLD_GLOBAL);
  if (jawt == NULL) {
    printf("Failed to open jawt.dylib\n");
    return JNI_FALSE;
  }
  _JAWT_GetAWT = (jboolean (JNICALL *)(JNIEnv *e, JAWT *c))dlsym(jawt, "JAWT_GetAWT");
  if (_JAWT_GetAWT == NULL) {
    printf("Failed to find JAWT_GetAWT\n");
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

//OpenGL

#include "../common/gl.cpp"

static NSView* getHandle(JNIEnv *e, jobject c) {
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  if (jawt == NULL) return NULL;
  if (_JAWT_GetAWT == NULL) return NULL;

  awt.version = JAWT_VERSION_1_4;
  if (!(*_JAWT_GetAWT)(e, &awt)) {
    printf("JAWT_GetAWT() failed\n");
    return NULL;
  }

  ds = awt.GetDrawingSurface(e, c);
  if (ds == NULL) {
    printf("JAWT.GetDrawingSurface() failed\n");
    return NULL;
  }
  lock = ds->Lock(ds);
  if ((lock & JAWT_LOCK_ERROR) != 0) {
    awt.FreeDrawingSurface(ds);
    printf("JAWT.Lock() failed\n");
    return NULL;
  }
  dsi = ds->GetDrawingSurfaceInfo(ds);
  if (dsi == NULL) {
    printf("JAWT.GetDrawingSurfaceInfo() failed\n");
    return NULL;
  }
  JAWT_MacOSXDrawingSurfaceInfo* wdsi = (JAWT_MacOSXDrawingSurfaceInfo*)dsi->platformInfo;
  if (wdsi == NULL) {
    printf("JAWT HWND == NULL\n");
    return NULL;
  }
  NSView *view = wdsi->cocoaViewRef;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return view;
}

struct GLContext {
  NSView *view;
  NSOpenGLContext *ctx;
  int shared;
  //to avoid using libstdc++ new/delete must be coded by hand
  static GLContext* New() {
    GLContext *ctx = (GLContext*)malloc(sizeof(GLContext));
    memset(ctx, 0, sizeof(GLContext));
    return ctx;
  }
  void Delete() {
    free(this);
  }
};

GLContext* createGLContext(JNIEnv *e, jobject c) {
  GLContext *ctx;
  jclass cls_gl = e->FindClass("javaforce/gl/GL");
  jfieldID fid_gl_ctx = e->GetFieldID(cls_gl, "ctx", "J");
  ctx = (GLContext*)e->GetLongField(c, fid_gl_ctx);
  if (ctx != NULL) {
    printf("OpenGL ctx used twice\n");
    return NULL;
  }
  ctx = GLContext::New();
  e->SetLongField(c,fid_gl_ctx,(jlong)ctx);
  return ctx;
}

GLContext* getGLContext(JNIEnv *e, jobject c) {
  GLContext *ctx;
  jclass cls_gl = e->FindClass("javaforce/gl/GL");
  jfieldID fid_gl_ctx = e->GetFieldID(cls_gl, "ctx", "J");
  ctx = (GLContext*)e->GetLongField(c, fid_gl_ctx);
  return ctx;
}

const unsigned int fmt_attrs[] = {
  //NSOpenGLPFAWindow,  //deprecated ???
  //NSOpenGLPFAAccelerated,  //is not available on my test system
  NSOpenGLPFADoubleBuffer,
  NSOpenGLPFAColorSize,24,
  NSOpenGLPFADepthSize,16,
  0  //zero terminate list
};

JNIEXPORT jboolean JNICALL Java_javaforce_gl_GL_glCreate
  (JNIEnv *e, jobject c, jobject canvas, jlong sharedCtx)
{
  GLContext *ctx = createGLContext(e,c);
  GLContext *ctx_shared = (GLContext*)sharedCtx;

  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc]init];

  ctx->view = getHandle(e, canvas);

  NSOpenGLPixelFormat *fmt = [[NSOpenGLPixelFormat alloc]initWithAttributes:fmt_attrs];
  if (fmt == NULL) {
    printf("NSOpenGLPixelFormat initWithAttributes failed\n");
    return JNI_FALSE;
  }
  if (ctx_shared == NULL) {
    ctx->ctx = [[NSOpenGLContext alloc]initWithFormat:fmt shareContext:nil];
    if (ctx->ctx == NULL) {
      printf("NSOpenGLContext initWithFormat failed\n");
      return JNI_FALSE;
    }
  } else {
    ctx->shared = 1;
    ctx->ctx = ctx_shared->ctx;
  }
  [ctx->ctx setView:ctx->view];
  [ctx->ctx makeCurrentContext];

  [pool release];

  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDelete
  (JNIEnv * e, jobject c)
{
  GLContext *ctx = getGLContext(e,c);
  [NSOpenGLContext clearCurrentContext];
  if (ctx->shared == 0) {
    [ctx->ctx release];
  }
  ctx->Delete();
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glSetContext
  (JNIEnv *e, jobject c)
{
  GLContext *ctx = getGLContext(e,c);
  [ctx->ctx setView:ctx->view];
  [ctx->ctx makeCurrentContext];
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glSwap
  (JNIEnv *e, jobject c)
{
  GLContext *ctx = getGLContext(e,c);
  [ctx->ctx flushBuffer];
}

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
