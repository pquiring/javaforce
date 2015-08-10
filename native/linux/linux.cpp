#include <dlfcn.h>
#include <stdlib.h>
#include <fcntl.h>  //open
#include <termios.h>  //com ports
#include <unistd.h>  //close
#include <linux/videodev2.h>  //V4L2
#include <sys/ioctl.h>  //ioctl
#include <sys/mman.h>  //mmap
#include <signal.h>
#include <errno.h>
#include <string.h>  //memcpy
#define FUSE_USE_VERSION 26
#include <fuse.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <security/pam_appl.h>

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "javaforce_jni_LnxNative.h"
#include "javaforce_gl_GL.h"
#include "javaforce_gl_GLWindow.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"

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

void *fuselib = NULL;

void (*_fuse_main_real)(int argsLength, char **args, void*ops, int opsSize, void*unknown) = NULL;

void *libcdio = NULL;

void* (*cdio_open_linux)(char *dev);
int (*cdio_destroy)(void *ptr);
int (*cdio_get_num_tracks)(void *ptr);
int (*cdio_get_track_lsn)(void *ptr, int track);  //start of track in Logical Sector Number
int (*cdio_get_track_sec_count )(void *ptr, int track);
int (*cdio_read_audio_sectors)(void *ptr, void *buf, int lsn, int blocks);  //2352 bytes each

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_lnxInit
  (JNIEnv *e, jclass c, jstring libgl_so, jstring libv4l2_so, jstring libcdio_so)
{
  if (jawt == NULL) {
    jawt = dlopen("libjawt.so", RTLD_LAZY | RTLD_GLOBAL);
    if (jawt == NULL) {
      printf("dlopen(libjawt.so) failed\n");
      return JNI_FALSE;
    }
    _JAWT_GetAWT = (jboolean (JNICALL *)(JNIEnv *e, JAWT *c))dlsym(jawt, "JAWT_GetAWT");
  }
  if (x11 == NULL) {
    x11 = dlopen("libX11.so", RTLD_LAZY | RTLD_GLOBAL);
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
  if (v4l2 == NULL) {
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
  if (fuselib == NULL) {
    fuselib = dlopen("libfuse.so", RTLD_LAZY | RTLD_GLOBAL);
    if (fuselib != NULL) {
      _fuse_main_real = (void (*)(int argsLength, char **args, void*ops, int opsSize, void*unknown))dlsym(fuselib, "fuse_main_real");
    }
  }
  if (libcdio == NULL && libcdio_so != NULL) {
    const char *clibcdio_so = e->GetStringUTFChars(libcdio_so,NULL);
    libcdio = dlopen(clibcdio_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libcdio_so, clibcdio_so);
    if (libcdio != NULL) {
      cdio_open_linux = (void* (*)(char *dev)) dlsym(libcdio, "cdio_open_linux");
      cdio_destroy = (int (*)(void *ptr)) dlsym(libcdio, "cdio_destroy");
      cdio_get_num_tracks = (int (*)(void *ptr)) dlsym(libcdio, "cdio_get_num_tracks");
      cdio_get_track_lsn = (int (*)(void *ptr, int track)) dlsym(libcdio, "cdio_get_track_lsn");
      cdio_get_track_sec_count = (int (*)(void *ptr, int track)) dlsym(libcdio, "cdio_get_track_sec_count");
      cdio_read_audio_sectors = (int (*)(void *ptr, void *buf, int lsn, int blocks)) dlsym(libcdio, "cdio_read_audio_sectors");
    }
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
  ctx = new CamContext();
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
  delete ctx;
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

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStart
  (JNIEnv *e, jobject c, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  if (deviceIdx >= ctx->cameraDeviceCount) return JNI_FALSE;
  errno = 0;
  char name[16];
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
    //select error
    return -1;
  }
  if (res == 0) {
    //timeout
    return 0;
  }

  if (pty->closed) {
    return -1;
  }

  if (FD_ISSET(pty->master, &error_set)) {
    return -1;
  }
  if (FD_ISSET(pty->master, &read_set)) {
    jbyte *baptr = e->GetByteArrayElements(ba,NULL);
    int readAmt = read(pty->master, baptr, e->GetArrayLength(ba));
    e->ReleaseByteArrayElements(ba, baptr, 0);
    return readAmt;
  }
  printf("LnxPty:select() : unknown reason");
  return -1;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyWrite
  (JNIEnv *e, jclass c, jlong ctx, jbyteArray ba)
{
  Pty *pty = (Pty*)ctx;
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  write(pty->master, baptr, e->GetArrayLength(ba));
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
}

//fuse (multi-threaded)

static JavaVM *fuseVM;
static jobject fuseObj;
//javaforce.jni.lnx.FuseStat
static jmethodID mid_allocate;
static jfieldID fid_folder;
static jfieldID fid_symlink;
static jfieldID fid_mode;
static jfieldID fid_size;
static jfieldID fid_atime;
static jfieldID fid_mtime;
static jfieldID fid_ctime;
//javaforce.jni.lnx.FuseOperations
static jmethodID mid_getattr;
static jmethodID mid_mkdir;
static jmethodID mid_unlink;
static jmethodID mid_rmdir;
static jmethodID mid_symlink;
static jmethodID mid_link;
static jmethodID mid_chmod;
static jmethodID mid_truncate;
static jmethodID mid_open;
static jmethodID mid_read;
static jmethodID mid_write;
static jmethodID mid_statfs;
static jmethodID mid_close;
static jmethodID mid_readdir;
static jmethodID mid_create;

static JNIEnv* fuseGetEnv() {
  JNIEnv *env;
  if (fuseVM->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
  fuseVM->AttachCurrentThread((void**)&env, NULL);
  return env;
}

static int _getattr(const char *path, struct stat *fs) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  jobject jstat = e->CallObjectMethod(NULL, mid_allocate);
  if (e->ExceptionCheck()) e->ExceptionClear();
  int ret = e->CallIntMethod(fuseObj, mid_getattr, jpath, jstat);
  if (e->ExceptionCheck()) e->ExceptionClear();
  if (ret != -1) {
  memset(fs, 0, sizeof(struct stat));
  //  fs->? = e->GetBooleanField(jstat, fid_folder);
  //  fs->? = e->GetBooleanField(jstat, fid_symlink);
    fs->st_mode = e->GetIntField(jstat, fid_mode);
    fs->st_size = e->GetLongField(jstat, fid_size);
    fs->st_atime = e->GetLongField(jstat, fid_atime);
    fs->st_mtime = e->GetLongField(jstat, fid_mtime);
    fs->st_ctime = e->GetLongField(jstat, fid_ctime);
  }
  e->DeleteLocalRef(jstat);
  return ret;
}

static int _mkdir(const char *path, mode_t mode) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_mkdir, jpath, mode);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _unlink(const char *path) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_unlink, jpath);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _rmdir(const char *path) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_rmdir, jpath);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _symlink(const char *target, const char *link) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath1 = e->NewStringUTF(target);
  jstring jpath2 = e->NewStringUTF(link);
  int ret = e->CallIntMethod(fuseObj, mid_symlink, jpath1, jpath2);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _link(const char *target, const char *link) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath1 = e->NewStringUTF(target);
  jstring jpath2 = e->NewStringUTF(link);
  int ret = e->CallIntMethod(fuseObj, mid_link, jpath1, jpath2);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _chmod(const char *path, mode_t mode) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_chmod, jpath, mode);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _truncate(const char *path, off_t size) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_truncate, jpath, size);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _open(const char *path, struct fuse_file_info *ffi) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_open, jpath, ffi->fh, ffi->flags);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _read(const char *path, char *buf, size_t size, off_t off, struct fuse_file_info *ffi) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  jbyteArray ba = e->NewByteArray(size);
  int ret = e->CallIntMethod(fuseObj, mid_read, jpath, ba, off, ffi->fh);
  if (e->ExceptionCheck()) e->ExceptionClear();
  if (ret <= 0) return ret;
  e->GetByteArrayRegion(ba, 0, size, (jbyte*)buf);
  return ret;
}

static int _write(const char *path, const char *buf, size_t size, off_t off, struct fuse_file_info *ffi) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  jbyteArray ba = e->NewByteArray(size);
  e->SetByteArrayRegion(ba, 0, size, (jbyte*)buf);
  int ret = e->CallIntMethod(fuseObj, mid_write, jpath, ba, off, ffi->fh);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _statfs(const char *path, struct statvfs *fs) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int freespace = e->CallIntMethod(fuseObj, mid_statfs, jpath);
  if (e->ExceptionCheck()) e->ExceptionClear();
  e->DeleteLocalRef(jpath);
  memset(fs, 0, sizeof(struct statvfs));
  fs->f_bsize = 512;
  fs->f_frsize = 512;
  fs->f_blocks = 1024 * 1024;
  fs->f_bfree = freespace / 512;
  fs->f_bavail = fs->f_bfree;
  //fs->f_files;
  //fs->f_ffree;
  //fs->f_favail;
  //fs->f_fsid;
  //fs->f_flags;
  fs->f_namemax = 256;
  return 0;
}

static int _close(const char *path, struct fuse_file_info *ffi) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_close, jpath, ffi->fh);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

static int _readdir(const char *path, void* buf, fuse_fill_dir_t filler, off_t off, struct fuse_file_info *ffi) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  jobjectArray sa = (jobjectArray)e->CallObjectMethod(fuseObj, mid_readdir, jpath);
  if (e->ExceptionCheck()) e->ExceptionClear();
  if (sa == NULL) return -1;
  int nstr = e->GetArrayLength(sa);
  for(int a=0;a<nstr;a++) {
    jstring jstr = (jstring)e->GetObjectArrayElement(sa, a);
    const char *cstr = e->GetStringUTFChars(jstr, NULL);
    (*filler)(buf, cstr, NULL, 0);
    e->ReleaseStringUTFChars(jstr, cstr);
  }
  return 0;
}

static int _create(const char *path, mode_t mode, struct fuse_file_info *ffi) {
  JNIEnv *e = fuseGetEnv();
  jstring jpath = e->NewStringUTF(path);
  int ret = e->CallIntMethod(fuseObj, mid_create, jpath, mode, ffi->fh);
  if (e->ExceptionCheck()) e->ExceptionClear();
  return ret;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fuse
  (JNIEnv *e, jclass c, jobjectArray args, jobject jops)
{
  if (fuselib == NULL || _fuse_main_real == NULL) {
    printf("Fuse not available\n");
    return;
  }

  fuseObj = jops;
  e->GetJavaVM(&fuseVM);

  struct fuse_operations ops;

  jclass cls;

  cls = e->FindClass("javaforce/jni/lnx/FuseStat");
  mid_allocate = e->GetMethodID(cls, "allocate", "()Ljavaforce/jni/lnx/FuseStat;");
  fid_folder = e->GetFieldID(cls, "folder" , "Z");
  fid_symlink = e->GetFieldID(cls, "symlink" , "Z");
  fid_mode = e->GetFieldID(cls, "mode" , "I");
  fid_size = e->GetFieldID(cls, "size" , "J");
  fid_atime = e->GetFieldID(cls, "atime" , "J");
  fid_mtime = e->GetFieldID(cls, "mtime" , "J");
  fid_ctime = e->GetFieldID(cls, "ctime" , "J");

  cls = e->FindClass("javaforce/jni/lnx/FuseOperations");
  mid_getattr = e->GetMethodID(cls, "getattr", "(Ljava/lang/String;Ljavaforce/jni/lnx/FuseStat;)I");
  mid_mkdir = e->GetMethodID(cls, "mkdir", "(Ljava/lang/String;I)I");
  mid_unlink = e->GetMethodID(cls, "unlink", "(Ljava/lang/String;)I");
  mid_rmdir = e->GetMethodID(cls, "rmdir", "(Ljava/lang/String;)I");
  mid_symlink = e->GetMethodID(cls, "symlink", "(Ljava/lang/String;Ljava/lang/String;)I");
  mid_link = e->GetMethodID(cls, "link", "(Ljava/lang/String;Ljava/lang/String;)I");
  mid_chmod = e->GetMethodID(cls, "chmod", "(Ljava/lang/String;I)I");
  mid_truncate = e->GetMethodID(cls, "truncate", "(Ljava/lang/String;J)I");
  mid_open = e->GetMethodID(cls, "open", "(Ljava/lang/String;II)I");
  mid_read = e->GetMethodID(cls, "read", "(Ljava/lang/String;[BJI)I");
  mid_write = e->GetMethodID(cls, "write", "(Ljava/lang/String;[BJI)I");
  mid_statfs = e->GetMethodID(cls, "statfs", "(Ljava/lang/String;)I");
  mid_close = e->GetMethodID(cls, "close", "(Ljava/lang/String;I)I");
  mid_readdir = e->GetMethodID(cls, "readdir", "(Ljava/lang/String;)[Ljava/lang/String;");
  mid_create = e->GetMethodID(cls, "create", "(Ljava/lang/String;II)I");

  ops.getattr = &_getattr;
  ops.mkdir = &_mkdir;
  ops.unlink = &_unlink;
  ops.rmdir = &_rmdir;
  ops.symlink = &_symlink;
  ops.link = &_link;
  ops.chmod = &_chmod;
  ops.truncate = &_truncate;
  ops.open = &_open;
  ops.read = &_read;
  ops.write = &_write;
  ops.statfs = &_statfs;
  ops.release = &_close;
  ops.readdir = &_readdir;
  ops.create = &_create;

  int nargs = e->GetArrayLength(args);
  char **cargs = (char **)malloc(sizeof(char*) * nargs);
  for(int a=0;a<nargs;a++) {
    jstring jstr = (jstring)e->GetObjectArrayElement(args, a);
    cargs[a] = (char*)e->GetStringUTFChars(jstr, NULL);
  }

  (*_fuse_main_real)(nargs, cargs, &ops, sizeof(ops), NULL);
}

//libcdio (optional)

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_cdio_1open_1linux
  (JNIEnv *e, jclass c, jstring jdev)
{
  char *cdev = (char*)e->GetStringUTFChars(jdev, NULL);
  void *ptr = (*cdio_open_linux)(cdev);
  e->ReleaseStringUTFChars(jdev, cdev);
  return (jlong)ptr;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_cdio_1destroy
  (JNIEnv *e, jclass c, jlong ptr)
{
  (*cdio_destroy)((void*)ptr);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_cdio_1get_1num_1tracks
  (JNIEnv *e, jclass c, jlong ptr)
{
  return (*cdio_get_num_tracks)((void*)ptr);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_cdio_1get_1track_1lsn
  (JNIEnv *e, jclass c, jlong ptr, jint track)
{
  return (*cdio_get_track_lsn)((void*)ptr, track);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_cdio_1get_1track_1sec_1count
  (JNIEnv *e, jclass c, jlong ptr, jint track)
{
  return (*cdio_get_track_sec_count)((void*)ptr, track);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_cdio_1read_1audio_1sectors
  (JNIEnv *e, jclass c, jlong ptr, jbyteArray ba, jint lsn, jint sectors)
{
  void *buf = e->GetByteArrayElements(ba, NULL);
  int ret = (*cdio_read_audio_sectors)((void*)ptr, buf, lsn, sectors);
  e->ReleaseByteArrayElements(ba, (jbyte*)buf, 0);
  return ret;
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
  x11_listener = obj;
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
    x11_window_e->CallVoidMethod(x11_listener, mid_x11_window_add, xid, pid, jtitle, jname, jres_name, jres_class);
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
      x11_window_e->CallVoidMethod(x11_listener, mid_x11_window_del, xid);
      if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
    } else {
      a++;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_x11_1window_1list_1main
  (JNIEnv *e, jclass c)
{
  jclass cls = e->FindClass("javaforce/linux/Linux");
  mid_x11_window_add = e->GetMethodID(cls, "x11_window_add", "(JILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
  mid_x11_window_del = e->GetMethodID(cls, "x11_window_del", "(J)V");

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
  (JNIEnv *e, jclass c, jstring user, jstring pass)
{
  pam_user = e->GetStringUTFChars(user,NULL);
  pam_pass = e->GetStringUTFChars(pass,NULL);
  pam_handle_t *handle;
  pam_conv conv;
  conv.conv = &pam_callback;
  conv.appdata_ptr = NULL;

  int res = pam_start("passwd", pam_user, &conv, &handle);
  if (res != 0) return JNI_FALSE;
  res = pam_authenticate(handle, PAM_SILENT);
  pam_end(handle, 0);
  if (pam_responses != NULL) {
//      free(pam_responses);  //crashes if password was wrong - memory leak for now???
    pam_responses = NULL;
  }

  e->ReleaseStringUTFChars(user, pam_user);
  e->ReleaseStringUTFChars(pass, pam_pass);

  pam_user = NULL;
  pam_pass = NULL;

  return res == 0;
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

#include "../common/ffmpeg.cpp"
