//camera API

#ifndef __FreeBSD__

struct FFMCamContext {
  int cameraDeviceCount;
  char **cameraDeviceNames;
  int camerafd;
  int api;
  int *px;
  int width, height, bytesperline, imagesize, mmapbuffers_type;
  void *read_buffer;
  struct mmapbuffer mmapbuffers[2];
};

FFMCamContext* createFFMCamContext() {
  FFMCamContext *ctx;
  ctx = (FFMCamContext*)malloc(sizeof(FFMCamContext));
  memset(ctx, 0, sizeof(FFMCamContext));
  return ctx;
}

void deleteFFMCamContext(FFMCamContext *ctx) {
  if (ctx == NULL) return;
  free(ctx);
}

static void resetFFMCameraList(FFMCamContext *ctx) {
  if (ctx->cameraDeviceNames != NULL) {
    for(int a=0;a<ctx->cameraDeviceCount;a++) {
      free(ctx->cameraDeviceNames[a]);
    }
    free(ctx->cameraDeviceNames);
    ctx->cameraDeviceNames = NULL;
  }
  ctx->cameraDeviceCount = 0;
}

jlong cameraInit()
{
  FFMCamContext *ctx = createFFMCamContext();
  if (ctx == NULL) return 0;
  return (jlong)ctx;
}

jboolean cameraUninit(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  deleteFFMCamContext(ctx);
  return JNI_TRUE;
}

JFArray* cameraListDevices(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return NULL;
  resetFFMCameraList(ctx);
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

  JFArray* strs = JFArray::create(ctx->cameraDeviceCount, sizeof(jchar*), ARRAY_TYPE_STRING);
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    char*name = ctx->cameraDeviceNames[a];
    int strlen = strlen8(name);
    char* str = (char*)malloc(strlen+1);
    strcpy8(str, name);
    strs->setString(a, str);
  }
  return strs;
}

JFArray* cameraListModes(jlong ctxptr, jint deviceIdx)
{
  return NULL;
}

jboolean cameraStart(jlong ctxptr, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
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
  return JNI_TRUE;
}

jboolean cameraStop(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
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
  if (ctx->px != NULL) {
    free(ctx->px);
    ctx->px = NULL;
  }
  return JNI_TRUE;
}

//convert RGB24 to RGB32 (it's actually BGR but that's because Java is BE)
static void copyFrameFFM(void* ptr, int length, FFMCamContext *ctx) {
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

static int* getFrame_streamFFM(FFMCamContext *ctx) {
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
    copyFrameFFM(ctx->mmapbuffers[i].start, buffer.bytesused, ctx);
    //requeue
    if (xioctl(ctx->camerafd, VIDIOC_QBUF, &buffer) == -1) {
      printf("LnxCamera:Failed to queue buffer\n");
      return NULL;
    }
    return ctx->px;
  }
  return NULL;
}

static int* getFrame_readFFM(FFMCamContext *ctx) {
  if ((*_v4l2_read)(ctx->camerafd, ctx->read_buffer, ctx->imagesize) <= 0) return NULL;
  copyFrameFFM(ctx->read_buffer, ctx->imagesize, ctx);
  return ctx->px;
}

JFArray* cameraGetFrame(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return NULL;
  int *img;
  switch (ctx->api) {
    case V4L2_CAP_READWRITE:
      img = getFrame_readFFM(ctx);
      break;
    case V4L2_CAP_STREAMING:
      img = getFrame_streamFFM(ctx);
      break;
  }
  if (img == NULL) return NULL;

  int pxsize = ctx->width * ctx->height;
  JFArray *px = JFArray::create(pxsize, 4, ARRAY_TYPE_INT);
  jint *pxptr = px->getBufferInt();
  memcpy(pxptr, img, pxsize * 4);

  return px;
}

jint cameraGetWidth(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return 0;
  return ctx->width;
}

jint cameraGetHeight(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return 0;
  return ctx->height;
}

extern "C" {
  JNIEXPORT jlong (*_cameraInit)() = &cameraInit;
  JNIEXPORT jboolean (*_cameraUninit)(jlong) = &cameraUninit;
  JNIEXPORT JFArray* (*_cameraListDevices)(jlong) = &cameraListDevices;
  JNIEXPORT JFArray* (*_cameraListModes)(jlong, jint) = &cameraListModes;
  JNIEXPORT jboolean (*_cameraStart)(jlong , jint , jint , jint ) = &cameraStart;
  JNIEXPORT jboolean (*_cameraStop)(jlong) = &cameraStop;
  JNIEXPORT JFArray* (*_cameraGetFrame)(jlong) = &cameraGetFrame;
  JNIEXPORT jint (*_cameraGetWidth)(jlong) = &cameraGetWidth;
  JNIEXPORT jint (*_cameraGetHeight)(jlong) = &cameraGetHeight;

  JNIEXPORT jboolean JNICALL CameraAPIinit() {return JNI_TRUE;}
}

#endif  //__FreeBSD__
