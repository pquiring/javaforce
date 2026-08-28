//camera API (FFM)

struct FFMCamContext;  //forward decl

@interface JFDelegate : NSObject
{
  @public FFMCamContext *ctx;
}
- (void)captureOutput:(AVCaptureOutput *)captureOutput
  didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
  fromConnection:(AVCaptureConnection *)connection;
@end

struct FFMCamContext {
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
  static FFMCamContext* New() {
    FFMCamContext *ctx = (FFMCamContext*)malloc(sizeof(FFMCamContext));
    memset(ctx, 0, sizeof(FFMCamContext));
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

FFMCamContext* createFFMCamContext(JNIEnv *e, jobject c) {
  FFMCamContext *ctx;
  ctx = FFMCamContext::New();
  return ctx;
}

void deleteFFMCamContext(JNIEnv *e, jobject c, FFMCamContext *ctx) {
  if (ctx == NULL) return;
  ctx->Delete();
}

jlong cameraInit()
{
  FFMCamContext *ctx = createFFMCamContext(e,c);
  if (ctx == NULL) return 0;
  return (jlong)ctx;
}

jboolean cameraUninit(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  deleteFFMCamContext(e,c,ctx);
  return JNI_TRUE;
}

JFArray* cameraListDevices(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return NULL;
  ctx->devices = [[AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo]
    arrayByAddingObjectsFromArray:[AVCaptureDevice devicesWithMediaType:AVMediaTypeMuxed]];
  int cnt = [ctx->devices count];

  JFArray* strs = JFArray::create(ctx->cameraDeviceCount, sizeof(jchar*), ARRAY_TYPE_STRING);
  for(int a=0;a<cnt;a++) {
    AVCaptureDevice *device = [ctx->devices objectAtIndex:a];
    char* name = [[device localizedName] UTF8String]);
    char* str = (char*)malloc(strlen(name) + 1);
    strcpy(str, name);
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

jboolean cameraStop(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
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

JFArray* cameraGetFrame(jlong ctxptr)
{
  FFMCamContext *ctx = (FFMCamContext*)ctxptr;
  if (ctx == NULL) return NULL;
//  if (ctx->frames == NULL) return NULL;
  if (!ctx->haveFrame) return NULL;

  int size = ctx->width * ctx->height;
  int idx = ctx->frameIdx;
  idx--;
  if (idx == -1) idx = FRAME_BUFFER_SIZE-1;
  void *px = ctx->frames[idx];
  if (px == NULL) return NULL;

  JFArray *jfpx = JFArray::create(size, 4, ARRAY_TYPE_INT);
  jint *pxptr = jfpx->getBufferInt();
  memcpy(pxptr, px, size * 4);

  return jfpx;
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
