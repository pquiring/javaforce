#include <dlfcn.h>
#include <stdlib.h>
#include <fcntl.h>  //open
#include <termios.h>  //com ports
#include <unistd.h>  //close select stat
#include <stdio.h>
#ifndef __FreeBSD__
#include <linux/videodev2.h>  //V4L2
#endif
#include <sys/ioctl.h>  //ioctl
#include <sys/mman.h>  //mmap
#include <sys/inotify.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <signal.h>
#include <errno.h>
#include <string.h>  //memcpy
#include <utime.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <security/pam_appl.h>
#include <ncurses.h>  //wtimeout wgetch

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "javaforce_jni_LnxNative.h"
#include "javaforce_gl_GL.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"
#include "javaforce_controls_ni_DAQmx.h"
#include "javaforce_media_VideoBuffer.h"
#include "javaforce_ui_Font.h"
#include "javaforce_ui_Image.h"
#include "javaforce_ui_Window.h"
#include "javaforce_net_PacketCapture.h"

#ifdef __arm__
  #define __RASPBERRY_PI__
#endif

#ifdef __aarch64__
  #define __RASPBERRY_PI__
#endif

#ifdef __RASPBERRY_PI__
  #include "gpio.c"
  #include "i2c.c"
#endif

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

void* jawt = NULL;
jboolean (JNICALL *_JAWT_GetAWT)(JNIEnv *e, JAWT *c) = NULL;

void* x11 = NULL;
void* (*_XOpenDisplay)(void*);
void (*_XCloseDisplay)(void*);

void* xgl = NULL;
void* (*_glXCreateContext)(void *x11, void *vi, void *shareList, int directRender);
int (*_glXDestroyContext)(void *x11, void *ctx);
int (*_glXMakeCurrent)(void *x11, int win, void *ctx);
void* (*_glXGetProcAddress)(const char *name);
void (*_glXSwapBuffers)(void *x11, int win);
void* (*_glXChooseVisual)(void *x11, int res, int *attrs);

void *v4l2 = NULL;

int (*_v4l2_open)(const char *file, int oflag, ...);
int (*_v4l2_close)(int fd);
int (*_v4l2_dup)(int fd);
int (*_v4l2_ioctl)(int fd, unsigned long int request, ...);
int (*_v4l2_read)(int fd, void* buffer, size_t n);
void* (*_v4l2_mmap)(void *start, size_t length, int prot, int flags, int fd, int64_t offset);
int (*_v4l2_munmap)(void *_start, size_t length);

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_lnxInit
  (JNIEnv *e, jclass c, jstring libX11_so, jstring libgl_so, jstring libv4l2_so)
{
  if (jawt == NULL) {
    jawt = dlopen("libjawt.so", RTLD_LAZY | RTLD_GLOBAL);
    if (jawt == NULL) {
      printf("dlopen(libjawt.so) failed\n");
      return JNI_FALSE;
    }
    _JAWT_GetAWT = (jboolean (JNICALL *)(JNIEnv *e, JAWT *c))dlsym(jawt, "JAWT_GetAWT");
  }
  if (x11 == NULL && libX11_so != NULL) {
    const char *clibX11_so = e->GetStringUTFChars(libX11_so,NULL);
    x11 = dlopen(clibX11_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libX11_so, clibX11_so);
    if (x11 == NULL) {
      printf("dlopen(libX11.so) failed!\n");
      return JNI_FALSE;
    }
    _XOpenDisplay = (void* (*)(void*))dlsym(xgl, "XOpenDisplay");
    _XCloseDisplay = (void (*)(void*))dlsym(xgl, "XCloseDisplay");
  }
  if (xgl == NULL && libgl_so != NULL) {
    const char *clibgl_so = e->GetStringUTFChars(libgl_so,NULL);
    xgl = dlopen(clibgl_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libgl_so, clibgl_so);
    if (xgl == NULL) {
      printf("dlopen(libGL.so) failed\n");
      return JNI_FALSE;
    }
    _glXCreateContext = (void* (*)(void*,void*,void*,int))dlsym(xgl, "glXCreateContext");
    _glXDestroyContext = (int (*)(void*, void*))dlsym(xgl, "glXDestroyContext");
    _glXMakeCurrent = (int (*)(void*,int,void*))dlsym(xgl, "glXMakeCurrent");
    _glXGetProcAddress = (void* (*)(const char *))dlsym(xgl, "glXGetProcAddress");
    _glXSwapBuffers = (void (*)(void*,int))dlsym(xgl, "glXSwapBuffers");
    _glXChooseVisual = (void* (*)(void*,int,int*))dlsym(xgl, "glXChooseVisual");
  }
  if (v4l2 == NULL && libv4l2_so != NULL) {
    const char *clibv4l2_so = e->GetStringUTFChars(libv4l2_so,NULL);
    v4l2 = dlopen(clibv4l2_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libv4l2_so, clibv4l2_so);
    if (v4l2 == NULL) {
      printf("dlopen(libv4l2.so) failed\n");
      return JNI_FALSE;
    }
    _v4l2_open = (int (*)(const char *file, int oflag, ...))dlsym(v4l2, "v4l2_open");
    _v4l2_close = (int (*)(int))dlsym(v4l2, "v4l2_close");
    _v4l2_dup = (int (*)(int))dlsym(v4l2, "v4l2_dup");
    _v4l2_ioctl = (int (*)(int, unsigned long int, ...))dlsym(v4l2, "v4l2_ioctl");
    _v4l2_read = (int (*)(int, void*, size_t))dlsym(v4l2, "v4l2_read");
    _v4l2_mmap = (void* (*)(void *, size_t, int, int, int, int64_t))dlsym(v4l2, "v4l2_mmap");
    _v4l2_munmap = (int (*)(void *, size_t))dlsym(v4l2, "v4l2_munmap");
  }

  return JNI_TRUE;
}

static long getX11ID(JNIEnv *e, jobject c) {
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  if (jawt == NULL) return 0;
  if (_JAWT_GetAWT == NULL) return 0;

  awt.version = JAWT_VERSION_1_4;
  if (!(*_JAWT_GetAWT)(e, &awt)) {
    printf("JAWT_GetAWT() failed\n");
    return 0;
  }

  ds = awt.GetDrawingSurface(e, c);
  if (ds == NULL) {
    printf("JAWT.GetDrawingSurface() failed\n");
    return 0;
  }
  lock = ds->Lock(ds);
  if ((lock & JAWT_LOCK_ERROR) != 0) {
    awt.FreeDrawingSurface(ds);
    printf("JAWT.Lock() failed\n");
    return 0;
  }
  dsi = ds->GetDrawingSurfaceInfo(ds);
  if (dsi == NULL) {
    printf("JAWT.GetDrawingSurfaceInfo() failed\n");
    return 0;
  }
  JAWT_X11DrawingSurfaceInfo* xdsi = (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;
  if (xdsi == NULL) {
    printf("JAWT.platformInfo == NULL\n");
    return 0;
  }
  long handle = xdsi->drawable;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

#include "../common/glfw.cpp"

#include "../common/gl.cpp"

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nseticon
  (JNIEnv *e, jclass c, jlong id, jstring filename, jint x, jint y)
{
  //TODO
}

//this func must be called only when a valid OpenGL context is set
JNIEXPORT jboolean JNICALL Java_javaforce_gl_GL_glInit
  (JNIEnv *e, jclass c)
{
  if (funcs[0].func != NULL) return JNI_TRUE;  //already done
  if (xgl == NULL) return JNI_FALSE;
  void *func;
  for(int a=0;a<GL_NO_FUNCS;a++) {
    func = (void*)(*_glXGetProcAddress)(funcs[a].name);  //get OpenGL 1.x function
    if (func == NULL) {
      func = (void*)dlsym(xgl, funcs[a].name);  //get OpenGL 2.0+ function
      if (func == NULL) {
        printf("glInit:Error:Can not find function:%s\n", funcs[a].name);
        continue;
      }
    }
    funcs[a].func = func;
  }
  return JNI_TRUE;
}

//camera API

#ifndef __FreeBSD__

#define MAX_NUM_CAMERAS 32

struct mmapbuffer {
  void* start;
  int length;
};

struct CamContext {
  int cameraDeviceCount;
  char **cameraDeviceNames;
  int camerafd;
  int api;
  int *px;
  jintArray jpx;
  int width, height, bytesperline, imagesize, mmapbuffers_type;
  void *read_buffer;
  struct mmapbuffer mmapbuffers[2];
};

CamContext* createCamContext(JNIEnv *e, jobject c) {
  CamContext *ctx;
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  ctx = (CamContext*)e->GetLongField(c, fid_cam_ctx);
  if (ctx != NULL) {
    printf("Camera ctx used twice\n");
    return NULL;
  }
  ctx = (CamContext*)malloc(sizeof(CamContext));
  memset(ctx, 0, sizeof(CamContext));
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
  free(ctx);
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  e->SetLongField(c,fid_cam_ctx,0);
}


static void resetCameraList(CamContext *ctx) {
  if (ctx->cameraDeviceNames != NULL) {
    for(int a=0;a<ctx->cameraDeviceCount;a++) {
      free(ctx->cameraDeviceNames[a]);
    }
    free(ctx->cameraDeviceNames);
    ctx->cameraDeviceNames = NULL;
  }
  ctx->cameraDeviceCount = 0;
}

static int xioctl(int fd, int req, void* arg) {
  int r;
  do {
    r = (*_v4l2_ioctl)(fd, req, arg);
  } while (r == -1 && errno == EINTR);
  return r;
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
  resetCameraList(ctx);
  int size = sizeof(char*) * MAX_NUM_CAMERAS;
  ctx->cameraDeviceNames = (char**)malloc(size);
  memset(ctx->cameraDeviceNames, 0, size);
  char name[16];
  int idx = 0;
  while(idx < MAX_NUM_CAMERAS) {
    sprintf(name, "/dev/video%d", idx);
    int camerafd = (*_v4l2_open)(name, O_RDWR | O_NONBLOCK);
    if (camerafd == -1) break;
    (*_v4l2_close)(camerafd);
    ctx->cameraDeviceNames[ctx->cameraDeviceCount] = (char*)malloc(strlen(name) + 1);
    strcpy(ctx->cameraDeviceNames[ctx->cameraDeviceCount], name);
    ctx->cameraDeviceCount++;
    idx++;
  }
  jclass strcls = e->FindClass("java/lang/String");
  jobjectArray strs = e->NewObjectArray(ctx->cameraDeviceCount, strcls, NULL);
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    e->SetObjectArrayElement(strs, a, e->NewStringUTF(ctx->cameraDeviceNames[a]));
  }
  return strs;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_media_Camera_cameraListModes
  (JNIEnv *e, jobject c, jint deviceIdx)
{
  return NULL;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStart
  (JNIEnv *e, jobject c, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  if (deviceIdx >= ctx->cameraDeviceCount) return JNI_FALSE;
  errno = 0;
  char name[32];
  sprintf(name, "/dev/video%d", deviceIdx);
  ctx->camerafd = (*_v4l2_open)(name, O_RDWR | O_NONBLOCK);
  if (ctx->camerafd == -1) {
    printf("LnxCamera:Failed to open camera\n");
    return JNI_FALSE;
  }
  v4l2_capability caps;
  int res = xioctl(ctx->camerafd, VIDIOC_QUERYCAP, &caps);
  if (res == -1) {
    printf("LnxCamera:Failed to get camera caps:%d\n", errno);
    return JNI_FALSE;
  }
  if ((caps.capabilities & V4L2_CAP_VIDEO_CAPTURE) == 0) {
    printf("LnxCamera:Device is not a camera:%d\n", errno);
    return JNI_FALSE;
  }
  ctx->api = caps.capabilities & V4L2_CAP_STREAMING;
  if (ctx->api == 0) {
    ctx->api = caps.capabilities & V4L2_CAP_READWRITE;
    if (ctx->api == 0) {
      printf("LnxCamera:Camera doesn't support read/write or streaming mode\n");
      return JNI_FALSE;
    }
  }
  //enum formats
  for(int fdi = 0;;fdi++) {
    v4l2_fmtdesc fmtdesc;
    memset(&fmtdesc, 0, sizeof(fmtdesc));
    fmtdesc.index = fdi;
    fmtdesc.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    if (xioctl(ctx->camerafd, VIDIOC_ENUM_FMT, &fmtdesc) == -1) {
      break;
    }
    printf("LnxCamera:fmtdescription=%x,%s\n",fmtdesc.pixelformat,fmtdesc.description);
    //enum framesize (width/height)
    for(int fsi=0;;fsi++) {
      v4l2_frmsizeenum frmsize;
      memset(&frmsize, 0, sizeof(v4l2_frmsizeenum));
      frmsize.index = fsi;
      frmsize.pixel_format = fmtdesc.pixelformat;
      if (xioctl(ctx->camerafd, VIDIOC_ENUM_FRAMESIZES, &frmsize) == -1) {
        break;
      }
      switch (frmsize.type) {
        case V4L2_FRMSIZE_TYPE_DISCRETE:
          printf("LnxCamera:size=%dx%d\n", frmsize.discrete.width, frmsize.discrete.height);
          break;
        case V4L2_FRMSIZE_TYPE_CONTINUOUS:
        case V4L2_FRMSIZE_TYPE_STEPWISE:
          printf("LnxCamera:size={%d-%d:step=%d}x{%d-%d:step=%d}\n"
            , frmsize.stepwise.min_width, frmsize.stepwise.max_width, frmsize.stepwise.step_width
            , frmsize.stepwise.min_height, frmsize.stepwise.max_height, frmsize.stepwise.step_height);
          break;
      }
    }
  }
  if (desiredWidth == -1 || desiredHeight == -1) {
    desiredWidth = 640;
    desiredHeight = 480;
  }
  //set crop
  v4l2_cropcap cropcap;
  memset(&cropcap, 0, sizeof(v4l2_cropcap));
  cropcap.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
  xioctl(ctx->camerafd, VIDIOC_CROPCAP, &cropcap);  //ignore errors
  v4l2_crop crop;
  memset(&crop, 0, sizeof(v4l2_crop));
  crop.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
  crop.c.left = cropcap.defrect.left;
  crop.c.top = cropcap.defrect.top;
  crop.c.width = cropcap.defrect.width;
  crop.c.height = cropcap.defrect.height;
  xioctl(ctx->camerafd, VIDIOC_S_CROP, &crop);  //ignore errors
  //set format
  v4l2_format fmt;
  memset(&fmt, 0, sizeof(v4l2_format));
  fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
  if (xioctl(ctx->camerafd, VIDIOC_G_FMT, &fmt) == -1) {
    printf("LnxCamera:Failed to get video format:%d\n", errno);
    return JNI_FALSE;
  }
  fmt.fmt.pix.width = desiredWidth;
  fmt.fmt.pix.height = desiredHeight;
  fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_BGR24;  //force format (libv4l2 will emulate if needed)
  fmt.fmt.pix.field = V4L2_FIELD_INTERLACED;  //show both top/bottom field interlaced
  if (xioctl(ctx->camerafd, VIDIOC_S_FMT, &fmt) == -1) {
    printf("LnxCamera:Failed to set video format:%d\n", errno);
    return JNI_FALSE;
  }
  //NOTE:width/height may have changed
  ctx->width = fmt.fmt.pix.width;
  ctx->height = fmt.fmt.pix.height;
  ctx->bytesperline = fmt.fmt.pix.bytesperline;
  if (ctx->bytesperline < ctx->width*3) ctx->bytesperline = ctx->width*3;
  ctx->imagesize = fmt.fmt.pix.sizeimage;
  printf("final format:=%dx%d (pixfmt=0x%x) (size=%d)\n", ctx->width, ctx->height, fmt.fmt.pix.pixelformat, ctx->imagesize);

  switch (ctx->api) {
    case V4L2_CAP_READWRITE:
      //init reading
      ctx->read_buffer = malloc(ctx->imagesize);
      break;
    case V4L2_CAP_STREAMING:
      //init streaming
      v4l2_requestbuffers requestbuffers;
      memset(&requestbuffers, 0, sizeof(v4l2_requestbuffers));
      requestbuffers.count = 2;
      requestbuffers.memory = V4L2_MEMORY_MMAP;
      requestbuffers.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
      if (xioctl(ctx->camerafd, VIDIOC_REQBUFS, &requestbuffers) == -1) {
        printf("LnxCamera:Failed to alloc MMAP Buffers:%d\n", errno);
        return JNI_FALSE;
      }
      if (requestbuffers.count != 2) {
        printf("LnxCamera:Failed to allocate MMAP Buffers (count != 2)\n");
        return JNI_FALSE;
      }
      ctx->mmapbuffers_type = requestbuffers.type;
      v4l2_buffer *buffer;
      for(int i=0;i<2;i++) {
        buffer = (v4l2_buffer*)malloc(sizeof(v4l2_buffer));
        memset(buffer, 0, sizeof(v4l2_buffer));
        buffer->type = ctx->mmapbuffers_type;
        buffer->memory = V4L2_MEMORY_MMAP;
        buffer->index = i;
        if (xioctl(ctx->camerafd, VIDIOC_QUERYBUF, buffer) == -1) {
          printf("LnxCamera:Failed to query mmap buffer\n");
          return JNI_FALSE;
        }
        ctx->mmapbuffers[i].length = buffer->length;
        ctx->mmapbuffers[i].start = (*_v4l2_mmap)(NULL, buffer->length, PROT_READ | PROT_WRITE, MAP_SHARED, ctx->camerafd, buffer->m.offset);
        if (xioctl(ctx->camerafd, VIDIOC_QBUF, buffer) == -1) {
          printf("LnxCamera:Failed to queue buffer");
          return JNI_FALSE;
        }
      }
      int type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
      if (xioctl(ctx->camerafd, VIDIOC_STREAMON, &type) == -1) {
        printf("LnxCamera:Failed to start stream:%d\n", errno);
        return JNI_FALSE;
      }
      break;
  }
  ctx->px = (int*)malloc(ctx->width * ctx->height * 4);
  ctx->jpx = e->NewIntArray(ctx->width * ctx->height);
  ctx->jpx = (jintArray)e->NewGlobalRef(ctx->jpx);  //ensure it is not gc'ed
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStop
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  if (ctx->camerafd == -1) return JNI_FALSE;
  switch (ctx->api) {
    case V4L2_CAP_READWRITE:
      if (ctx->read_buffer != NULL) {
        free(ctx->read_buffer);
        ctx->read_buffer = NULL;
      }
      break;
    case V4L2_CAP_STREAMING:
      int type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
      xioctl(ctx->camerafd, VIDIOC_STREAMOFF, &type);
      //free buffers
      for(int i=0;i<2;i++) {
        (*_v4l2_munmap)(ctx->mmapbuffers[i].start, ctx->mmapbuffers[i].length);
      }
      break;
  }
  close(ctx->camerafd);
  ctx->camerafd = -1;
  ctx->api = -1;
  if (ctx->jpx != NULL) {
    e->DeleteGlobalRef(ctx->jpx);
    ctx->jpx = NULL;
  }
  if (ctx->px != NULL) {
    free(ctx->px);
    ctx->px = NULL;
  }
  return JNI_TRUE;
}

//convert RGB24 to RGB32 (it's actually BGR but that's because Java is BE)
static void copyFrame(void* ptr, int length, CamContext *ctx) {
  //copy each line
  char *src = (char*)ptr;
  char *dst = (char*)ctx->px;
  int wrap = ctx->bytesperline - (ctx->width * 3);
  for(int y=0;y<ctx->height;y++) {
    for(int x=0;x<ctx->width;x++) {
      *(dst++) = *(src++);
      *(dst++) = *(src++);
      *(dst++) = *(src++);
      *(dst++) = 0xff;  //alpha (opaque)
    }
    src += wrap;
  }
}

static int* getFrame_stream(CamContext *ctx) {
  v4l2_buffer buffer;
  for(int i=0;i<2;i++) {
    memset(&buffer, 0, sizeof(v4l2_buffer));
    buffer.type = ctx->mmapbuffers_type;
    buffer.memory = V4L2_MEMORY_MMAP;
    buffer.index = i;
    if (xioctl(ctx->camerafd, VIDIOC_QUERYBUF, &buffer) == -1) {
      printf("LnxCamera:Failed to query buffer\n");
      return NULL;
    }
    if ((buffer.flags & V4L2_BUF_FLAG_DONE) == 0) continue;
    //dequeue
    if (xioctl(ctx->camerafd, VIDIOC_DQBUF, &buffer) == -1) {
      printf("LnxCamera:Failed to dequeue buffer\n");
      return NULL;
    }
    //read memory
    copyFrame(ctx->mmapbuffers[i].start, buffer.bytesused, ctx);
    //requeue
    if (xioctl(ctx->camerafd, VIDIOC_QBUF, &buffer) == -1) {
      printf("LnxCamera:Failed to queue buffer\n");
      return NULL;
    }
    return ctx->px;
  }
  return NULL;
}

static int* getFrame_read(CamContext *ctx) {
  if ((*_v4l2_read)(ctx->camerafd, ctx->read_buffer, ctx->imagesize) <= 0) return NULL;
  copyFrame(ctx->read_buffer, ctx->imagesize, ctx);
  return ctx->px;
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_Camera_cameraGetFrame
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return NULL;
  int *img;
  switch (ctx->api) {
    case V4L2_CAP_READWRITE:
      img = getFrame_read(ctx);
      break;
    case V4L2_CAP_STREAMING:
      img = getFrame_stream(ctx);
      break;
  }
  if (img == NULL) return NULL;
  jint *jpxptr = e->GetIntArrayElements(ctx->jpx,NULL);
  memcpy(jpxptr, img, ctx->width * ctx->height * 4);
  e->ReleaseIntArrayElements(ctx->jpx, jpxptr, 0);
  return ctx->jpx;
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

#endif

//com port API

static termios orgattrs;

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_comOpen
  (JNIEnv *e, jclass c, jstring str, jint baud)
{
  int baudcode = -1;
  switch (baud) {
    case 9600: baudcode = 015; break;
    case 19200: baudcode = 016; break;
    case 38400: baudcode = 017; break;
    case 57600: baudcode = 010001; break;
    case 115200: baudcode = 010002; break;
  }
  if (baudcode == -1) {
    printf("LnxCom:Unknown baud rate\n");
    return 0;
  }
  const char *cstr = e->GetStringUTFChars(str,NULL);
  int fd = open(cstr, O_RDWR | O_NOCTTY);
  e->ReleaseStringUTFChars(str, cstr);
  if (fd == -1) {
    printf("LnxCom:invalid handle\n");
    return 0;
  }

  tcgetattr(fd, &orgattrs);
  termios attrs;
  memset(&attrs, 0, sizeof(termios));
  attrs.c_cflag = baudcode | CS8 | CLOCAL | CREAD;

  attrs.c_cc[VMIN]  =  1;          // block until at least 1 char
  attrs.c_cc[VTIME] =  5;          // 0.5 seconds read timeout

  tcflush(fd, TCIFLUSH);
  tcsetattr(fd, TCSANOW, &attrs);
  return fd;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_comClose
  (JNIEnv *e, jclass c, jint fd)
{
  tcsetattr(fd, TCSANOW, &orgattrs);
  close(fd);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_comRead
  (JNIEnv *e, jclass c, jint fd, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int readAmt = read(fd, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return readAmt;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_comWrite
  (JNIEnv *e, jclass c, jint fd, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int writeAmt = write(fd, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return writeAmt;
}

//pty API

struct Pty {
  int master;
  char *slaveName;
  jboolean closed;
};

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_ptyAlloc
  (JNIEnv *e, jclass c)
{
  Pty *pty;
  pty = (Pty*)malloc(sizeof(Pty));
  memset(pty, 0, sizeof(Pty));
  return (jlong)pty;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyFree
  (JNIEnv *e, jclass c, jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  free(pty);
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_LnxNative_ptyOpen
  (JNIEnv *e, jclass c, jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  pty->master = posix_openpt(O_RDWR | O_NOCTTY);
  if (pty->master == -1) {
    printf("LnxPty:failed to alloc pty\n");
    pty->master = 0;
    return NULL;
  }
  pty->slaveName = ptsname(pty->master);
  if (pty->slaveName == NULL) {
    printf("LnxPty:slaveName == NULL\n");
    return NULL;
  }
  if (grantpt(pty->master) != 0) {
    printf("LnxPty:grantpt() failed\n");
    return NULL;
  }
  if (unlockpt(pty->master) != 0) {
    printf("LnxPty:unlockpt() failed\n");
    return NULL;
  }
  return e->NewStringUTF(pty->slaveName);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyClose
  (JNIEnv *e, jclass c, jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  if (pty->master != 0) close(pty->master);
  pty->closed = JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_ptyRead
  (JNIEnv *e, jclass c, jlong ctx, jbyteArray ba)
{
  Pty *pty = (Pty*)ctx;

  timeval timeout;
  timeout.tv_sec = 1;  //1 full second
  timeout.tv_usec = 0;
  fd_set read_set;
  FD_SET(pty->master, &read_set);

  fd_set error_set;
  FD_SET(pty->master, &error_set);

  int res = select(pty->master+1, &read_set, NULL, &error_set, &timeout);
  if (res == -1) {
    printf("LnxPty:select() : unknown error:%d:%d\n", res ,errno);
    return -1;
  }
  if (res == 0) {
    //timeout
    return 0;
  }

  if (pty->closed) {
    printf("LnxPty:select() : closed\n");
    return -1;
  }

  if (FD_ISSET(pty->master, &error_set)) {
    printf("LnxPty:select() : error_set\n");
    return -1;
  }
  if (FD_ISSET(pty->master, &read_set)) {
    jbyte *baptr = e->GetByteArrayElements(ba,NULL);
    int readAmt = read(pty->master, baptr, e->GetArrayLength(ba));
    e->ReleaseByteArrayElements(ba, baptr, 0);
    if (readAmt < 0) {
      printf("LnxPty:read() failed:%d:%d\n", readAmt, errno);
      return -1;
    }
    return readAmt;
  }
  //Warning:this does happen until the child process opens the pty
  //printf("LnxPty:select() : unknown reason:%d:%d\n", res, errno);
  return 0;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyWrite
  (JNIEnv *e, jclass c, jlong ctx, jbyteArray ba)
{
  Pty *pty = (Pty*)ctx;
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int res = write(pty->master, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptySetSize
  (JNIEnv *e, jclass c, jlong ctx, jint x, jint y)
{
  Pty *pty = (Pty*)ctx;
  winsize size;
  memset(&size, 0, sizeof(winsize));
  size.ws_row = (short)y;
  size.ws_col = (short)x;
  size.ws_xpixel = (short)(x*8);
  size.ws_ypixel = (short)(y*8);
  ioctl(pty->master, TIOCSWINSZ, &size);
}

#ifdef __FreeBSD__
#define execvpe exect  //different name
#define IUTF8 0  //not supported
#endif

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_ptyChildExec
  (JNIEnv *e, jclass c, jstring slaveName, jstring cmd, jobjectArray args, jobjectArray env)
{
  const char *cslaveName = e->GetStringUTFChars(slaveName,NULL);
  int slave = open(cslaveName, O_RDWR);
  e->ReleaseStringUTFChars(slaveName, cslaveName);
  if (slave == -1) {
    printf("LnxPty:unable to open slave pty\n");
    exit(0);
  }
  if (setsid() == -1) {
    printf("LnxPty:unable to setsid\n");
    exit(0);
  }
  termios attrs;
  memset(&attrs, 0, sizeof(termios));
  tcgetattr(slave, &attrs);
  // Assume input is UTF-8; this allows character-erase to be correctly performed in cooked mode.
  attrs.c_iflag |= IUTF8;
  // Humans don't need XON/XOFF flow control of output, and it only serves to confuse those who accidentally hit ^S or ^Q, so turn it off.
  attrs.c_iflag &= ~IXON;
  // ???
  attrs.c_cc[VERASE] = 127;
  tcsetattr(slave, TCSANOW, &attrs);
  dup2(slave, STDIN_FILENO);
  dup2(slave, STDOUT_FILENO);
  dup2(slave, STDERR_FILENO);
  signal(SIGINT, SIG_DFL);
  signal(SIGQUIT, SIG_DFL);
  signal(SIGCHLD, SIG_DFL);

  //build args
  int nargs = e->GetArrayLength(args);
  char **cargs = (char **)malloc((nargs+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nargs;a++) {
    jstring jstr = (jstring)e->GetObjectArrayElement(args, a);
    const char *cstr = e->GetStringUTFChars(jstr,NULL);
    int sl = strlen(cstr);
    cargs[a] = (char*)malloc(sl+1);
    strcpy(cargs[a], cstr);
    e->ReleaseStringUTFChars(jstr, cstr);
  }
  cargs[nargs] = NULL;

  //build env
  int nenv = e->GetArrayLength(env);
  char **cenv = (char **)malloc((nenv+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nenv;a++) {
    jstring jstr = (jstring)e->GetObjectArrayElement(env, a);
    const char *cstr = e->GetStringUTFChars(jstr,NULL);
    int sl = strlen(cstr);
    cenv[a] = (char*)malloc(sl+1);
    strcpy(cenv[a], cstr);
    e->ReleaseStringUTFChars(jstr, cstr);
  }
  cenv[nenv] = NULL;

  const char *ccmd = e->GetStringUTFChars(cmd,NULL);
  execvpe(ccmd, cargs, cenv);
  return 0;
}

//X11

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_x11_1get_1id
  (JNIEnv *e, jclass c, jobject window)
{
  return getX11ID(e,window);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1set_1desktop
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = XOpenDisplay(NULL);
  int ret;
  Atom* states = (Atom*)malloc(sizeof(Atom) * 4);
  Atom* types = (Atom*)malloc(sizeof(Atom) * 1);
  for(int a=0;a<2;a++) {
    Atom state = XInternAtom(display, "_NET_WM_STATE", 0);
    states[0] = XInternAtom(display, "_NET_WM_STATE_BELOW", 0);
    states[1] = XInternAtom(display, "_NET_WM_STATE_SKIP_PAGER", 0);
    states[2] = XInternAtom(display, "_NET_WM_STATE_SKIP_TASKBAR", 0);
    states[3] = XInternAtom(display, "_NET_WM_STATE_STICKY", 0);
    ret = XChangeProperty(display, xid, state, XA_ATOM, 32, PropModeReplace, (const unsigned char*)states, 4);
    Atom type = XInternAtom(display, "_NET_WM_WINDOW_TYPE", 0);
    types[0] = XInternAtom(display, "_NET_WM_WINDOW_TYPE_DESKTOP", 0);
    ret = XChangeProperty(display, xid, type, XA_ATOM, 32, PropModeReplace, (const unsigned char*)types, 1);
  }
  free(states);
  free(types);
  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1set_1dock
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = XOpenDisplay(NULL);
  int ret;
  Atom* states = (Atom*)malloc(sizeof(Atom) * 4);
  Atom* types = (Atom*)malloc(sizeof(Atom) * 1);
  for(int a=0;a<2;a++) {
    Atom state = XInternAtom(display, "_NET_WM_STATE", 0);
    states[0] = XInternAtom(display, "_NET_WM_STATE_ABOVE", 0);
    states[1] = XInternAtom(display, "_NET_WM_STATE_SKIP_PAGER", 0);
    states[2] = XInternAtom(display, "_NET_WM_STATE_SKIP_TASKBAR", 0);
    states[3] = XInternAtom(display, "_NET_WM_STATE_STICKY", 0);
    ret = XChangeProperty(display, xid, state, XA_ATOM, 32, PropModeReplace, (const unsigned char*)states, 4);
    Atom type = XInternAtom(display, "_NET_WM_WINDOW_TYPE", 0);
    types[0] = XInternAtom(display, "_NET_WM_WINDOW_TYPE_DOCK", 0);
    ret = XChangeProperty(display, xid, type, XA_ATOM, 32, PropModeReplace, (const unsigned char*)types, 1);
  }
  free(states);
  free(types);
  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1set_1strut
  (JNIEnv *e, jclass c, jlong xid, jint panelHeight, jint x, jint y, jint width, jint height)
{
  Display *display = XOpenDisplay(NULL);
  Atom strut = XInternAtom(display, "_NET_WM_STRUT_PARTIAL", 0);
  Atom *values = (Atom*)malloc(sizeof(Atom) * 12);
  values[0]=(Atom)0;  //left
  values[1]=(Atom)0;  //right
  values[2]=(Atom)0;  //top
  values[3]=(Atom)panelHeight;   //bottom
  values[4]=(Atom)0;  //left_start_y
  values[5]=(Atom)(height-1);  //left_end_y
  values[6]=(Atom)0;  //right_start_y
  values[7]=(Atom)(height-1);  //right_end_y
  values[8]=(Atom)0;  //top_start_x
  values[9]=(Atom)(width-1);  //top_end_x
  values[10]=(Atom)0;  //bottom_start_x
  values[11]=(Atom)(width-1);  //bottom_end_x
  XChangeProperty(display, xid, strut, XA_CARDINAL, 32, PropModeReplace, (const unsigned char*)values, 12);
  free(values);
  XCloseDisplay(display);
}

#define MAX_TRAY_ICONS 64
static XID tray_icons[MAX_TRAY_ICONS];
static int screen_width;
static Display *tray_display;
static Atom tray_opcode;//, tray_data;
static XID tray_window;
static jboolean tray_active;
static int tray_count = 0;
static JavaVM *x11_VM;
static JNIEnv *x11_tray_e;
static jobject x11_listener;
static jmethodID mid_x11_listener_trayIconAdded;
static jmethodID mid_x11_listener_trayIconRemoved;
static jmethodID mid_x11_listener_windowsChanged;
static JNIEnv *x11_window_e;
static jclass cid_javaforce_linux_Linux;
static jmethodID mid_x11_window_add;
static jmethodID mid_x11_window_del;
#define tray_icon_size 24  //fixed
static int tray_height = 24+4;
static int tray_rows = 2;
static int borderSize = 4;
static int tray_pos = 0;
static int tray_pad = 2;
static int tray_width = 24+4;

static JNIEnv* x11_GetEnv() {
  JNIEnv *env;
  if (x11_VM->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
  x11_VM->AttachCurrentThread((void**)&env, NULL);
  return env;
}

static void tray_move_icons() {
  int a, x = tray_pad, y;
  int y1 = 0;
  int y2 = 0;
  if (tray_rows == 1) {
    y1 = (tray_height - tray_icon_size) / 2;
  } else {
    int d = (tray_height - (tray_icon_size*2))/3;
    y1 = d;
    y2 = d + tray_icon_size + d;
  }
  y = y1;
  for(a=0;a<MAX_TRAY_ICONS;a++) {
    if (tray_icons[a] == 0) continue;
    XMoveResizeWindow(tray_display, tray_icons[a], x, y, tray_icon_size, tray_icon_size);
    if (y == y1 && tray_rows > 1) {
      y = y2;
    } else {
      y = y1;
      x += tray_icon_size + tray_pad;
    }
  }
  //reposition/resize tray window
  int cols = (tray_count + (tray_rows > 1 ? 1 : 0)) / tray_rows;
  if (cols == 0) cols = 1;
  int px = tray_pos - (cols * (tray_icon_size+tray_pad)) - tray_pad - borderSize;
  int py = borderSize;
  int sx = (cols * (tray_icon_size+tray_pad)) + tray_pad;
  tray_width = sx;
  int sy = tray_height;  //tray_rows * (tray_icon_size + tray_pad) + tray_pad;
//    JFLog.log("Tray Position:" + px + "," + py + ",size=" + sx + "," + sy);
  XMoveResizeWindow(tray_display, tray_window, px, py, sx, sy);
}

static void tray_add_icon(XID w) {
  if (tray_count == MAX_TRAY_ICONS) return;  //ohoh
  tray_count++;
  int a;
  for(a=0;a<MAX_TRAY_ICONS;a++) {
    if (tray_icons[a] == 0) {
      tray_icons[a] = w;
      break;
    }
  }
  XReparentWindow(tray_display, w, tray_window, 0, 0);
  tray_move_icons();
  XMapWindow(tray_display, w);
  x11_tray_e->CallVoidMethod(x11_listener, mid_x11_listener_trayIconAdded);
  if (x11_tray_e->ExceptionCheck()) x11_tray_e->ExceptionClear();
}

/* Tray opcode messages from System Tray Protocol Specification
   http://standards.freedesktop.org/systemtray-spec/systemtray-spec-0.3.html
*/
#define SYSTEM_TRAY_REQUEST_DOCK   0
#define SYSTEM_TRAY_BEGIN_MESSAGE  1
#define SYSTEM_TRAY_CANCEL_MESSAGE 2

#define SYSTEM_TRAY_STOP 0x100  //more like a wake up
#define SYSTEM_TRAY_REPOSITION 0x101

static void tray_client_message(XClientMessageEvent *ev) {
  if (ev->message_type == tray_opcode) {
    switch (ev->data.l[1]) {
      case SYSTEM_TRAY_REQUEST_DOCK:
        tray_add_icon(ev->data.l[2]);
        break;
      case SYSTEM_TRAY_BEGIN_MESSAGE:
        break;
      case SYSTEM_TRAY_CANCEL_MESSAGE:
        break;
      case SYSTEM_TRAY_REPOSITION:
//          JFLog.log("Tray:ClientMessage = SYSTEM_TRAY_REPOSITION");
        tray_move_icons();
        break;
      case SYSTEM_TRAY_STOP:
        //does nothing, but main while loop will now exit
        break;
    }
  }
}

static void tray_remove_icon(XDestroyWindowEvent *ev) {
  int a;
  for(a=0;a<MAX_TRAY_ICONS;a++) {
    if (tray_icons[a] == 0) continue;
    if (tray_icons[a] == ev->window) {
      tray_icons[a] = 0;
      tray_count--;
      tray_move_icons();
      x11_tray_e->CallVoidMethod(x11_listener, mid_x11_listener_trayIconRemoved, tray_count);
      if (x11_tray_e->ExceptionCheck()) x11_tray_e->ExceptionClear();
      break;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1tray_1main
  (JNIEnv *e, jclass c, jlong pid, jint screenWidth, jint trayPos, jint trayHeight)
{
  x11_tray_e = e;
  for(int a=0;a<MAX_TRAY_ICONS;a++) {
    tray_icons[a] = 0;
  }

  XEvent ev;
  screen_width = screenWidth;
  tray_pos = trayPos;
  tray_height = trayHeight;
  if (tray_height >= tray_icon_size * 2 + tray_pad * 3) {
    tray_rows = 2;
  } else {
    tray_rows = 1;
  }
  tray_display = XOpenDisplay(NULL);
  Atom tray_atom = XInternAtom(tray_display, "_NET_SYSTEM_TRAY_S0", False);
  tray_opcode = XInternAtom(tray_display, "_NET_SYSTEM_TRAY_OPCODE", False);
//  tray_data = XInternAtom(tray_display, "_NET_SYSTEM_TRAY_MESSAGE_DATA", False);

  tray_window = XCreateSimpleWindow(
    tray_display,
    (XID)pid,  //parent id
    trayPos - tray_icon_size - 4 - borderSize, borderSize,  //pos
    tray_icon_size + 4, 52,  //size
    1,  //border_width
    (0xcccccc),  //border clr
    (0xdddddd));  //backgnd clr

  XSetSelectionOwner(tray_display, tray_atom, tray_window, CurrentTime);

  //get DestroyNotify events
  XSelectInput(tray_display, tray_window, SubstructureNotifyMask);

  XMapWindow(tray_display, tray_window);

  tray_active = JNI_TRUE;
  while (tray_active) {
    XNextEvent(tray_display, &ev);
    switch (ev.type) {
      case ClientMessage:
        tray_client_message((XClientMessageEvent*)&ev);
        break;
      case DestroyNotify:
        tray_remove_icon((XDestroyWindowEvent*)&ev);
        break;
    }
  }

  XCloseDisplay(tray_display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1tray_1reposition
  (JNIEnv *e, jclass c, jint screenWidth, jint trayPos, jint trayHeight)
{
  if (screenWidth != -1) screen_width = screenWidth;
  if (trayPos != -1) tray_pos = trayPos;
  if (trayHeight != -1) tray_height = trayHeight;
  if (tray_height >= tray_icon_size * 2 + tray_pad * 3) {
    tray_rows = 2;
  } else {
    tray_rows = 1;
  }
  //X11 is not thread safe so can't call tray_move_icons() from here, send a msg instead
  Display *display = XOpenDisplay(NULL);

  XClientMessageEvent event;

  event.type = ClientMessage;
  event.display = display;
  event.window = tray_window;
  event.message_type = tray_opcode;
  event.format = 32;
  event.data.l[1] = SYSTEM_TRAY_REPOSITION;

  XSendEvent(display, event.window, True, 0, (XEvent*)&event);

  XCloseDisplay(display);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_x11_1tray_1width
  (JNIEnv *e, jclass c)
{
  return tray_width;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1tray_1stop
  (JNIEnv *e, jclass c)
{
  tray_active = JNI_FALSE;
  Display *display = XOpenDisplay(NULL);

  XClientMessageEvent event;

  event.type = ClientMessage;
  event.display = display;
  event.window = tray_window;
  event.message_type = tray_opcode;
  event.format = 32;
  event.data.l[1] = SYSTEM_TRAY_STOP;

  XSendEvent(display, event.window, True, 0, (XEvent*)&event);

  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1set_1listener
  (JNIEnv *e, jclass c, jobject obj)
{
  jclass cls = e->FindClass("javaforce/linux/X11Listener");
  x11_listener = e->NewGlobalRef(obj);
  mid_x11_listener_trayIconAdded = e->GetMethodID(cls, "trayIconAdded", "(I)V");
  mid_x11_listener_trayIconRemoved = e->GetMethodID(cls, "trayIconRemoved", "(I)V");
  mid_x11_listener_windowsChanged = e->GetMethodID(cls, "windowsChanged", "()V");
}

#define MAX_WINDOWS 1024

static jboolean window_list_active = JNI_FALSE;

static XID window_list[MAX_WINDOWS];
static int window_list_size = 0;
static int window_list_event_mask[MAX_WINDOWS];

static void x11_update_window_list(Display *display) {
  XID newList[MAX_WINDOWS];
  int newListSize = 0;

  XID root_window = XDefaultRootWindow(display);

  Atom net_client_list = XInternAtom(display, "_NET_CLIENT_LIST", False);
//    Atom net_client_list_stacking = XInternAtom(display, "_NET_CLIENT_LIST_STACKING", False);
  Atom net_name = XInternAtom(display, "_NET_WM_NAME", False);
  Atom net_pid = XInternAtom(display, "_NET_WM_PID", False);
  Atom net_state = XInternAtom(display, "_NET_WM_STATE", False);
  Atom net_skip_taskbar = XInternAtom(display, "_NET_WM_STATE_SKIP_TASKBAR", False);

  unsigned char *prop;
  unsigned long nItems;
  Atom l1;
  unsigned long l2;
  int i1;
  XGetWindowProperty(display, root_window, net_client_list, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
  int nWindows = nItems;
  XID *list = (XID*)prop;
  for(int a=0;a<nWindows;a++) {
    XID xid = list[a];

    //check state for skip taskbar
    XGetWindowProperty(display, xid, net_state, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
    if (nItems > 0) {
      Atom* atoms = (Atom*)prop;
      jboolean found = JNI_FALSE;
      for(int n=0;n<nItems;n++) {
        if (atoms[n] == net_skip_taskbar) {
          found = JNI_TRUE;
        }
      }
      XFree(prop);
      if (found) continue;
    }

    //get window pid
    XGetWindowProperty(display, xid, net_pid, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
    int pid = -1;
    XID *xids = NULL;
    if (nItems > 0) {
      xids = (XID*)prop;
      pid = xids[0];
    }

    //get title
    XGetWindowProperty(display, xid, net_name, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
    char *title = NULL;
    if (nItems > 0) {
      title = (char*)prop;
    }

    //get name
    char *name = NULL;
    XFetchName(display, xid, &name);

    //get res_name, res_class
    char *res_name = NULL, *res_class = NULL;
    XClassHint hint;
    XGetClassHint(display, xid, &hint);
    res_name = hint.res_name;
    res_class = hint.res_class;

    //add to list
    jstring jtitle = NULL;
    if (title != NULL) jtitle = x11_window_e->NewStringUTF(title);
    jstring jname = NULL;
    if (name != NULL) jname = x11_window_e->NewStringUTF(name);
    jstring jres_name = NULL;
    if (res_name != NULL) jres_name = x11_window_e->NewStringUTF(res_name);
    jstring jres_class = NULL;
    if (res_class != NULL) jres_class = x11_window_e->NewStringUTF(res_class);
    x11_window_e->CallStaticVoidMethod(cid_javaforce_linux_Linux, mid_x11_window_add, xid, pid, jtitle, jname, jres_name, jres_class);
    if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
    if (jtitle != NULL) x11_window_e->DeleteLocalRef(jtitle);
    if (jname != NULL) x11_window_e->DeleteLocalRef(jname);
    if (jres_name != NULL) x11_window_e->DeleteLocalRef(jres_name);
    if (jres_class != NULL) x11_window_e->DeleteLocalRef(jres_class);
    if (xids != NULL) {
      XFree(xids);
    }
    if (title != NULL) {
      XFree(title);
    }
    if (name != NULL) {
      XFree(name);
    }
    if (res_name != NULL) {
      XFree(res_name);
    }
    if (res_class != NULL) {
      XFree(res_class);
    }
    newList[newListSize++] = xid;
  }
  XFree(list);

  //add newList to currentList
  for(int a=0;a<newListSize;a++) {
    XID xid = newList[a];
    jboolean found = JNI_FALSE;
    for(int b=0;b<window_list_size;b++) {
      if (window_list[b] == xid) {
        found = JNI_TRUE;
        break;
      }
    }
    if (!found) {
      if (window_list_size == MAX_WINDOWS) break;  //Ohoh
      window_list[window_list_size] = xid;
      window_list_event_mask[window_list_size] = XSelectInput(display, xid, PropertyChangeMask);
      window_list_size++;
    }
  }
  //remove from currentList if not in newList
  for(int a=0;a<window_list_size;) {
    XID xid = window_list[a];
    jboolean found = JNI_FALSE;
    for(int b=0;b<newListSize;b++) {
      if (newList[b] == xid) {
        found = JNI_TRUE;
        break;
      }
    }
    if (!found) {
      XSelectInput(display, xid, window_list_event_mask[a]);
      for(int z=a+1;z<window_list_size;z++) {
        window_list[z-1] = window_list[z];
        window_list_event_mask[z-1] = window_list_event_mask[z];
      }
      window_list_size--;
      x11_window_e->CallStaticVoidMethod(cid_javaforce_linux_Linux, mid_x11_window_del, xid);
      if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
    } else {
      a++;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1window_1list_1main
  (JNIEnv *e, jclass c)
{
  cid_javaforce_linux_Linux = e->FindClass("javaforce/linux/Linux");
  mid_x11_window_add = e->GetStaticMethodID(cid_javaforce_linux_Linux, "x11_window_add", "(JILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
  mid_x11_window_del = e->GetStaticMethodID(cid_javaforce_linux_Linux, "x11_window_del", "(J)V");

  x11_window_e = e;

  XEvent ev;

  Display *display = XOpenDisplay(NULL);

  XID root_window = XDefaultRootWindow(display);

  Atom net_client_list = XInternAtom(display, "_NET_CLIENT_LIST", False);
  Atom net_client_list_stacking = XInternAtom(display, "_NET_CLIENT_LIST_STACKING", False);
  Atom net_name = XInternAtom(display, "_NET_WM_NAME", False);
  Atom net_pid = XInternAtom(display, "_NET_WM_PID", False);
  Atom net_state = XInternAtom(display, "_NET_WM_STATE", False);

  XSelectInput(display, root_window, PropertyChangeMask);

  window_list_active = JNI_TRUE;
  while (window_list_active) {
    XNextEvent(display, &ev);
    switch (ev.type) {
      case PropertyNotify:
        XPropertyEvent *xpropertyevent = (XPropertyEvent*)&ev;
        Atom atom = xpropertyevent->atom;
        if (
          (atom == net_client_list) ||
          (atom == net_client_list_stacking) ||
          (atom == net_name) ||
          (atom == net_pid) ||
          (atom == net_state)
           )
        {
          x11_update_window_list(display);
          x11_window_e->CallVoidMethod(x11_listener, mid_x11_listener_windowsChanged);
          if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
        }
        break;
    }
  }

  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1window_1list_1stop
  (JNIEnv *e, jclass c)
{
  window_list_active = JNI_FALSE;
  //TODO : send a message to ??? to cause main() loop to abort
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1minimize_1all
  (JNIEnv *e, jclass c)
{
  Display *display = XOpenDisplay(NULL);
  //TODO : need to lock list ???
  for(int a=0;a<window_list_size;a++) {
    XIconifyWindow(display, window_list[a], 0);
  }
  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1raise_1window
  (JNIEnv *e, jclass c, jlong xid)
{
  Display *display = XOpenDisplay(NULL);
  XRaiseWindow(display, (XID)xid);
  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1map_1window
  (JNIEnv *e, jclass c, jlong xid)
{
  Display *display = XOpenDisplay(NULL);
  XMapWindow(display, (XID)xid);
  XCloseDisplay(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1unmap_1window
  (JNIEnv *e, jclass c, jlong xid)
{
  Display *display = XOpenDisplay(NULL);
  XUnmapWindow(display, (XID)xid);
  XCloseDisplay(display);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_x11_1keysym_1to_1keycode
  (JNIEnv *e, jclass c, jchar keysym)
{
  Display *display = XOpenDisplay(NULL);
  int keycode = XKeysymToKeycode(display, keysym);
  XCloseDisplay(display);
  switch (keysym) {
    case '!':
    case '@':
    case '#':
    case '$':
    case '%':
    case '^':
    case '&':
    case '*':
    case '"':
    case ':':
      keycode |= 0x100;
  }
  return keycode;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_x11_1send_1event__IZ
  (JNIEnv *e, jclass c, jint keycode, jboolean down)
{
  Display *display = XOpenDisplay(NULL);

  Window x11id;
  int revert;
  XGetInputFocus(display, &x11id, &revert);

  XKeyEvent event;

  event.type = (down ? KeyPress : KeyRelease);
  event.keycode = keycode & 0xff;
  event.display = display;
  event.window = x11id;
  event.root = XDefaultRootWindow(display);
  event.subwindow = None;
  event.time = CurrentTime;
  event.x = 1;
  event.y = 1;
  event.x_root = 1;
  event.y_root = 1;
  event.same_screen = True;
  if ((keycode & 0x100) == 0x100) event.state = ShiftMask;

  int status = XSendEvent(display, event.window, True, down ? KeyPressMask : KeyReleaseMask, (XEvent*)&event);

  XCloseDisplay(display);

  return status != 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_x11_1send_1event__JIZ
  (JNIEnv *e, jclass c, jlong id, jint keycode, jboolean down)
{
  Display *display = XOpenDisplay(NULL);

  XKeyEvent event;

  event.type = (down ? KeyPress : KeyRelease);
  event.keycode = keycode & 0xff;
  event.display = display;
  event.window = (Window)id;
  event.root = XDefaultRootWindow(display);
  event.subwindow = None;
  event.time = CurrentTime;
  event.x = 1;
  event.y = 1;
  event.x_root = 1;
  event.y_root = 1;
  event.same_screen = True;
  if ((keycode & 0x100) == 0x100) event.state = ShiftMask;

  int status = XSendEvent(display, event.window, True, down ? KeyPressMask : KeyReleaseMask, (XEvent*)&event);

  XCloseDisplay(display);

  return status != 0;
}

//PAM

static const char *pam_user, *pam_pass;
static struct pam_response* pam_responses;

static int pam_callback(int num_msg, const struct pam_message** _pam_messages, struct pam_response** _pam_responses, void* _appdata_ptr)
{
  pam_responses = (struct pam_response*)calloc(num_msg, sizeof(pam_response));  //array of pam_response
  char* tmp;
  for(int a=0;a<num_msg;a++) {
    const struct pam_message *msg = _pam_messages[a];
    tmp = NULL;
    switch (msg->msg_style) {
      case PAM_PROMPT_ECHO_ON:
        tmp = strdup(pam_user);
        break;
      case PAM_PROMPT_ECHO_OFF:
        tmp = strdup(pam_pass);
        break;
    }
    pam_responses[a].resp = tmp;
    pam_responses[a].resp_retcode = 0;
  }
  *_pam_responses = pam_responses;
  return 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_authUser
  (JNIEnv *e, jclass c, jstring user, jstring pass, jstring backend)
{
  const char *cbackend = e->GetStringUTFChars(backend,NULL);
  pam_user = e->GetStringUTFChars(user,NULL);
  pam_pass = e->GetStringUTFChars(pass,NULL);
  pam_handle_t *handle;
  pam_conv conv;
  conv.conv = &pam_callback;
  conv.appdata_ptr = NULL;

  int res = pam_start(cbackend, pam_user, &conv, &handle);
  if (res != 0) {
    e->ReleaseStringUTFChars(backend, cbackend);
    e->ReleaseStringUTFChars(user, pam_user);
    e->ReleaseStringUTFChars(pass, pam_pass);

    printf("pam_start() failed:%d:%d\n", res, errno);
    return JNI_FALSE;
  }
  res = pam_authenticate(handle, PAM_SILENT);
  printf("pam_authenticate():%d:%d\n", res, errno);
  pam_end(handle, 0);
  if (pam_responses != NULL) {
//      free(pam_responses);  //crashes if password was wrong - memory leak for now???
    pam_responses = NULL;
  }

  e->ReleaseStringUTFChars(backend, cbackend);
  e->ReleaseStringUTFChars(user, pam_user);
  e->ReleaseStringUTFChars(pass, pam_pass);

  pam_user = NULL;
  pam_pass = NULL;

  return res == 0;
}

//inotify

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_inotify_1init
  (JNIEnv *e, jclass c)
{
  return inotify_init();
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_inotify_1add_1watch
  (JNIEnv *e, jclass c, jint fd, jstring path, jint mask)
{
  const char *cpath = e->GetStringUTFChars(path,NULL);
  int wd = inotify_add_watch(fd, cpath, mask);
  e->ReleaseStringUTFChars(path, cpath);
  return wd;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_inotify_1rm_1watch
  (JNIEnv *e, jclass c, jint fd, jint wd)
{
  return inotify_rm_watch(fd, wd);
}

JNIEXPORT jbyteArray JNICALL Java_javaforce_jni_LnxNative_inotify_1read
  (JNIEnv *e, jclass c, jint fd)
{
  char inotify_buffer[512];
  int size = read(fd, inotify_buffer, 512);
  if (size == -1) return NULL;
  jbyteArray ba = e->NewByteArray(size);
  e->SetByteArrayRegion(ba, 0, size, (jbyte*)inotify_buffer);
  return ba;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_inotify_1close
  (JNIEnv *e, jclass c, jint fd)
{
  close(fd);
}

//misc

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_setenv
  (JNIEnv *e, jclass c, jstring name, jstring value)
{
  const char *cname = e->GetStringUTFChars(name,NULL);
  const char *cvalue = e->GetStringUTFChars(value,NULL);
  setenv(cname, cvalue, 1);
  e->ReleaseStringUTFChars(name, cname);
  e->ReleaseStringUTFChars(value, cvalue);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_fileGetMode
  (JNIEnv *e, jclass c, jstring name)
{
  struct stat s;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  e->ReleaseStringUTFChars(name, cname);
  return s.st_mode;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fileSetMode
  (JNIEnv *e, jclass c, jstring name, jint mode)
{
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::chmod((const char *)cname, mode);
  e->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fileSetAccessTime
  (JNIEnv *e, jclass c, jstring name, jlong ts)
{
  struct stat s;
  struct utimbuf tb;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  ts /= 1000L;
  tb.actime = ts;
  tb.modtime = s.st_mtime;
  ::utime((const char *)cname, &tb);
  e->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fileSetModifiedTime
  (JNIEnv *e, jclass c, jstring name, jlong ts)
{
  struct stat s;
  struct utimbuf tb;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  ts /= 1000L;
  tb.actime = s.st_atime;
  tb.modtime = ts;
  ::utime((const char *)cname, &tb);
  e->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_fileGetID
  (JNIEnv *e, jclass c, jstring name)
{
  struct stat s;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  e->ReleaseStringUTFChars(name, cname);
  return s.st_ino;
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_LnxNative_getConsoleSize
  (JNIEnv *e, jclass c)
{
  int xy[2];
  struct winsize w;
  ioctl(STDOUT_FILENO, TIOCGWINSZ, &w);
  xy[0] = w.ws_col;
  xy[1] = w.ws_row;
  jintArray ia = e->NewIntArray(2);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)xy);
  return ia;
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_LnxNative_getConsolePos
  (JNIEnv *e, jclass c)
{
  int xy[2];
  //print ESC[6n
  printf("\x1b[6n");
  int x = 1;
  int y = 1;
  char t;
  int val = 0;
  //reply = ESC[row;colR
  while (1) {
    t = fgetc(stdin);
    if (t == '\x1b') continue;
    if (t == '[') continue;
    if (t == 'R') {
      x = val;
      break;
    }
    if (t == ';') {
      y = val;
      val = 0;
    } else {
      val *= 10;
      val += (t - '0');
    }
  }
  xy[0] = x;
  xy[1] = y;
  jintArray ia = e->NewIntArray(2);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)xy);
  return ia;
}

static char console_buffer[8];

static void StringCopy(char *dest, const char *src) {
  while (*src != 0) {
    *(dest++) = (*src++);
  }
  *dest = *src;
}

static struct termios oldt, newt;

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_enableConsoleMode
  (JNIEnv *e, jclass c)
{
  console_buffer[0] = 0;
  initscr();
  raw();
  noecho();
  wtimeout(stdscr, 0);
  wgetch(stdscr);  //first call to wgetch() clears the screen
  wtimeout(stdscr, -1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_disableConsoleMode
  (JNIEnv *e, jclass c)
{
  endwin();
}

JNIEXPORT jchar JNICALL Java_javaforce_jni_LnxNative_readConsole
  (JNIEnv *e, jclass c)
{
  if (console_buffer[0] != 0) {
    char ret = console_buffer[0];
    StringCopy(console_buffer, console_buffer+1);
    return (jchar)ret;
  }
  wtimeout(stdscr, -1);
  char ch = wgetch(stdscr);
  if (ch == 0x1b) {
    //is it Escape key or ANSI code???
    wtimeout(stdscr, 100);
    char ch2 = wgetch(stdscr);  //waits 100ms max
    if (ch2 == ERR) {
      StringCopy(console_buffer, "[1~");  //custom ansi code for esc
    } else {
      if (ch2 == 0x1b) {
        ungetch(ch2);
        StringCopy(console_buffer, "[1~");  //custom ansi code for esc
      } else {
        console_buffer[0] = ch2;
        console_buffer[1] = 0;
      }
    }
    wtimeout(stdscr, -1);
  }
  return (jchar)ch;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_peekConsole
  (JNIEnv *e, jclass c)
{
  if (console_buffer[0] != 0) return JNI_TRUE;
  wtimeout(stdscr, 0);
  char ch = wgetch(stdscr);
  if (ch == 0x1b) {
    console_buffer[0] = 0x1b;
    //is it Escape key or ANSI code???
    wtimeout(stdscr, 100);
    char ch2 = wgetch(stdscr);  //waits 100ms max
    if (ch2 == ERR) {
      StringCopy(console_buffer+1, "[1~");  //custom ansi code for esc
    } else {
      if (ch2 == 0x1b) {
        ungetch(ch2);
        StringCopy(console_buffer+1, "[1~");  //custom ansi code for esc
      } else {
        console_buffer[1] = ch2;
        console_buffer[2] = 0;
      }
    }
    wtimeout(stdscr, -1);
  }
  if (ch == ERR) {
    return JNI_FALSE;
  } else {
    console_buffer[0] = ch;
    console_buffer[1] = 0;
    return JNI_TRUE;
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_writeConsole
  (JNIEnv *e, jclass c, jint ch)
{
  printf("%c", ch);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_writeConsoleArray
  (JNIEnv *e, jclass c, jbyteArray ba, jint off, jint len)
{
  jbyte tmp[128];
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int length = len;
  int pos = off;
  while (length > 0) {
    if (length > 127) {
      memcpy(tmp, baptr+pos, 127);
      tmp[127] = 0;
      length -= 127;
      pos += 127;
    } else {
      memcpy(tmp, baptr+pos, length);
      tmp[length] = 0;
      length = 0;
    }
    printf("%s", tmp);
  }
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
}

#include "../common/library.h"

#include "../common/ffmpeg.cpp"

#ifndef __arm__
#include "../common/videobuffer.cpp"
#endif

#ifndef __FreeBSD__
#include "../common/ni.cpp"
#endif

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

#include "../common/pcap.cpp"
