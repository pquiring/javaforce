#include <objbase.h>
#include <dshow.h>
#include "qedit.h"

#ifdef __GNUC__
  //define missing from Mingw headers
//  DEFINE_GUID(CLSID_SampleGrabber, 0xc1f400a0, 0x3f08, 0x11d3, 0x9f,0x0b, 0x00,0x60,0x08,0x03,0x9e,0x37);
//  DEFINE_GUID(CLSID_NullRenderer , 0xc1f400a4, 0x3f08, 0x11d3, 0x9f,0x0b, 0x00,0x60,0x08,0x03,0x9e,0x37);
#endif

#define MAX_NUM_CAMERAS 32

struct CamContext {
  int cameraDeviceCount;
  jchar **cameraDeviceNames;  //utf16
  IMoniker **cameraDevices;
  int listModes;
  int cameraModeCount;
  char** cameraModes;
  ICaptureGraphBuilder2 *captureGraphBuilder;
  IGraphBuilder *graphBuilder;
  IMediaControl *mediaControl;
  IBaseFilter *videoInputFilter;
  IAMStreamConfig *streamConfig;
  AM_MEDIA_TYPE *mediaType;
  AM_MEDIA_TYPE rgbMediaType;
  VIDEOINFOHEADER *videoInfo;
  IBaseFilter *sampleGrabberBaseFilter;
  ISampleGrabber *sampleGrabber;
  IBaseFilter *destBaseFilter;  //NULL dest
  GUID PIN;
  int width, height;
  IPin *pin;
  IEnumMediaTypes *enumMediaTypes;
  void* buffer;
  int bufferSize;
  jintArray jpx;
  int compression;
};

CamContext* createCamContext(JNIEnv *e, jobject c) {
  CamContext *ctx;
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  ctx = (CamContext*)e->GetLongField(c, fid_cam_ctx);
  if (ctx != NULL) {
    printf("Camera ctx used twice:%p\n", ctx);
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

static int strlen16(jchar *str) {
  int len = 0;
  while (*str != 0) {
    len++;
    str++;
  }
  return len;
}

static void resetCameraList(CamContext *ctx) {
  printf("resetCameraList\n");
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    IMoniker *cam = ctx->cameraDevices[a];
    cam->Release();
    jchar* name = ctx->cameraDeviceNames[a];
    free(name);
  }
  ctx->cameraDeviceCount = 0;
}

static void printCameraList(CamContext *ctx) {
  printf("printCameraList\n");
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    IMoniker *cam = ctx->cameraDevices[a];
    jchar* name = ctx->cameraDeviceNames[a];
    printf("camera:%p:%ls\n", cam, (wchar_t*)name);
  }
}

static void resetCameraModeList(CamContext *ctx) {
  printf("resetCameraModes\n");
  for(int a=0;a<ctx->cameraModeCount;a++) {
    char* mode = ctx->cameraModes[a];
    free(mode);
  }
  ctx->cameraModeCount = 0;
}

static void cameraReleaseAll(JNIEnv *e, CamContext *ctx) {
  if (ctx->captureGraphBuilder != NULL) {
    ctx->captureGraphBuilder->Release();
    ctx->captureGraphBuilder = NULL;
  }
  if (ctx->graphBuilder != NULL) {
    ctx->graphBuilder->Release();
    ctx->graphBuilder = NULL;
  }
  if (ctx->mediaControl != NULL) {
    ctx->mediaControl->Release();
    ctx->mediaControl = NULL;
  }
  if (ctx->videoInputFilter != NULL) {
    ctx->videoInputFilter->Release();
    ctx->videoInputFilter = NULL;
  }
  if (ctx->streamConfig != NULL) {
    ctx->streamConfig->Release();
    ctx->streamConfig = NULL;
  }
  if (ctx->buffer != NULL) {
    free(ctx->buffer);
    ctx->buffer = NULL;
  }
  if (ctx->jpx != NULL) {
    e->DeleteGlobalRef(ctx->jpx);
    ctx->jpx = NULL;
  }
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraInit
  (JNIEnv *e, jobject c)
{
  printf("CameraInit\n");
  CamContext *ctx = createCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;

  ctx->cameraDeviceNames = (jchar**)malloc(sizeof(jchar*) * MAX_NUM_CAMERAS);
  memset(ctx->cameraDeviceNames, 0, sizeof(jchar*) * MAX_NUM_CAMERAS);

  ctx->cameraDevices = (IMoniker**)malloc(sizeof(IMoniker*) * MAX_NUM_CAMERAS);
  memset(ctx->cameraDevices, 0, sizeof(IMoniker*) * MAX_NUM_CAMERAS);

  ctx->cameraDeviceCount = 0;

  ctx->cameraModes = (char**)malloc(sizeof(char*) * MAX_NUM_CAMERAS);
  memset(ctx->cameraModes, 0, sizeof(char*) * MAX_NUM_CAMERAS);

  ctx->cameraModeCount = 0;

  return CoInitializeEx(NULL, COINIT_MULTITHREADED) == S_OK;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraUninit
  (JNIEnv *e, jobject c)
{
  printf("CameraUninit\n");
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return JNI_FALSE;
  if (ctx->cameraDeviceNames != NULL) {
    free(ctx->cameraDeviceNames);
    ctx->cameraDeviceNames = NULL;
  }
  if (ctx->cameraDevices != NULL) {
    free(ctx->cameraDevices);
    ctx->cameraDevices = NULL;
  }
  cameraReleaseAll(e,ctx);
  deleteCamContext(e,c,ctx);
  CoUninitialize();
  return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_media_Camera_cameraListDevices
  (JNIEnv *e, jobject c)
{
  printf("CameraListDevices\n");
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return NULL;
  resetCameraList(ctx);

  ICreateDevEnum *createDevEnum;
  IEnumMoniker *enumMoniker;
  IMoniker *moniker;
  IPropertyBag *propBag;

  if (CoCreateInstance(CLSID_SystemDeviceEnum, NULL, CLSCTX_INPROC_SERVER, IID_ICreateDevEnum, (void**)&createDevEnum) != S_OK) {
    printf("CoCreateInstance() failed\n");
    return NULL;
  }

  if (createDevEnum->CreateClassEnumerator(CLSID_VideoInputDeviceCategory, &enumMoniker, 0) != S_OK) {
    printf("ICreateDevEnum.CreateClassEnumerator() failed\n");
    return NULL;
  }

  VARIANT var;
  VariantInit(&var);

  while(ctx->cameraDeviceCount < MAX_NUM_CAMERAS) {
    moniker = NULL;
    enumMoniker->Next(1, &moniker, NULL);
    if (moniker == NULL) break;  //done

    if (moniker->BindToStorage(NULL, NULL, IID_IPropertyBag, (void**)&propBag) != S_OK) {
      moniker->Release();
      moniker = NULL;
      continue;  //this one failed, try the next one
    }

    HRESULT res;
    res = propBag->Read(L"Description", &var, NULL);
    if (res != S_OK) {
      res = propBag->Read(L"FriendlyName", &var, NULL);
    }
    if (res == S_OK) {
      jchar *wstr = (jchar*)var.byref;
      int wstrlen = strlen16(wstr) * 2;
      //printf("camera=%ls\n", wstr);
      //printf("moniker=%p\n", moniker);
      ctx->cameraDeviceNames[ctx->cameraDeviceCount] = (jchar*)malloc(wstrlen);
      memcpy(ctx->cameraDeviceNames[ctx->cameraDeviceCount], wstr, wstrlen + 2);
      ctx->cameraDevices[ctx->cameraDeviceCount] = moniker;
      ctx->cameraDeviceCount++;
    } else {
      moniker->Release();
      moniker = NULL;
    }

    VariantClear(&var);
    propBag->Release();
    propBag = NULL;
  };

  jclass strcls = e->FindClass("java/lang/String");
  jobjectArray strs = e->NewObjectArray(ctx->cameraDeviceCount, strcls, NULL);
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    e->SetObjectArrayElement(strs, a, e->NewString(ctx->cameraDeviceNames[a], strlen16(ctx->cameraDeviceNames[a])));
  }

  createDevEnum->Release();
  enumMoniker->Release();

  return strs;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_media_Camera_cameraListModes
  (JNIEnv *e, jobject c, jint deviceIdx)
{
  printf("CameraListModes\n");
  CamContext *ctx = getCamContext(e,c);
  if (ctx == NULL) return NULL;
  resetCameraModeList(ctx);

  if (deviceIdx < 0 || deviceIdx >= ctx->cameraDeviceCount) return NULL;

  ctx->listModes = JNI_TRUE;
  Java_javaforce_media_Camera_cameraStart(e,c,deviceIdx,-1,-1);
  Java_javaforce_media_Camera_cameraStop(e,c);
  ctx->listModes = JNI_FALSE;

  jclass strcls = e->FindClass("java/lang/String");
  jobjectArray strs = e->NewObjectArray(ctx->cameraModeCount, strcls, NULL);
  for(int a=0;a<ctx->cameraModeCount;a++) {
    e->SetObjectArrayElement(strs, a, e->NewStringUTF(ctx->cameraModes[a]));
  }

  return strs;
}

static void FreeMediaType(AM_MEDIA_TYPE *mt)
{
  if (mt->cbFormat != 0)
  {
    CoTaskMemFree((PVOID)mt->pbFormat);
    mt->cbFormat = 0;
    mt->pbFormat = NULL;
  }
  if (mt->pUnk != NULL)
  {
    // pUnk should not be used.
    mt->pUnk->Release();
    mt->pUnk = NULL;
  }
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStart
  (JNIEnv *e, jobject c, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  printf("CameraStart:deviceIdx=%d\n", deviceIdx);
  CamContext *ctx = getCamContext(e,c);
  HRESULT res;

  if (deviceIdx < 0 || deviceIdx >= ctx->cameraDeviceCount) return JNI_FALSE;

  if (CoCreateInstance(CLSID_CaptureGraphBuilder2, NULL, CLSCTX_INPROC_SERVER, IID_ICaptureGraphBuilder2 , (void**)&ctx->captureGraphBuilder) != S_OK) {
    printf("CoCreateInstance() failed\n");
    return JNI_FALSE;
  }

  if (CoCreateInstance(CLSID_FilterGraph, NULL, CLSCTX_INPROC_SERVER, IID_IGraphBuilder, (void**)&ctx->graphBuilder)) {
    printf("CoCreateInstance() failed\n");
    return JNI_FALSE;
  }

  if (ctx->captureGraphBuilder->SetFiltergraph(ctx->graphBuilder) != S_OK) {
    printf("ICaptureGraphBuilder2.SetFiltergraph() failed\n");
    return JNI_FALSE;
  }

  if (ctx->graphBuilder->QueryInterface(IID_IMediaControl, (void**)&ctx->mediaControl) != S_OK) {
    printf("IGraphBuilder.QueryInterface() failed\n");
    return JNI_FALSE;
  }

  IMoniker *moniker = ctx->cameraDevices[deviceIdx];
  if (moniker == NULL) {
    printf("IMoniker == NULL");
    return JNI_FALSE;
  }

  if (moniker->BindToObject(NULL, NULL, IID_IBaseFilter, (void**)&ctx->videoInputFilter) != S_OK) {
    printf("IMoniker.BindToObject() failed\n");
    return JNI_FALSE;
  }

  ctx->graphBuilder->AddFilter(ctx->videoInputFilter, (LPCWSTR)ctx->cameraDeviceNames[deviceIdx]);

  if (ctx->captureGraphBuilder->FindInterface(&PIN_CATEGORY_PREVIEW, &MEDIATYPE_Video, ctx->videoInputFilter, IID_IAMStreamConfig, (void**)&ctx->streamConfig) != S_OK) {
    printf("Warning:Couldn't find preview pin using SmartTee\n");
    if (ctx->captureGraphBuilder->FindInterface(&PIN_CATEGORY_CAPTURE, &MEDIATYPE_Video, ctx->videoInputFilter, IID_IAMStreamConfig, (void**)&ctx->streamConfig) != S_OK) {
      printf("ICaptureGraphBuilder2.FindInterface() failed\n");
      return JNI_FALSE;
    }
    ctx->PIN = PIN_CATEGORY_CAPTURE;
  } else {
    ctx->PIN = PIN_CATEGORY_PREVIEW;
  }

  //TODO : routeCrossbar() : needed???

  if (ctx->captureGraphBuilder->FindInterface(&ctx->PIN, &MEDIATYPE_Video, ctx->videoInputFilter, IID_IPin, (void**)&ctx->pin) != S_OK) {
    printf("ICaptureGraphBuilder2.FindInterface() failed\n");
    return JNI_FALSE;
  }

  if (ctx->pin->EnumMediaTypes(&ctx->enumMediaTypes) != S_OK) {
    printf("IPin.EnumMediaTypes() failed\n");
    return JNI_FALSE;
  }

/*
  //enumerate pins (not needed)
  res = ctx->videoInputFilter->EnumPins(ref);
  if (res != 0) throw new Exception("IBaseFilter.EnumPins() failed");
*/

  //GetFormat() returns the FIRST format supported and may not be the ACTIVE format
  if (ctx->streamConfig->GetFormat(&ctx->mediaType) != S_OK) {
    printf("IAMStreamConfig.GetFormat() failed\n");
    return JNI_FALSE;
  }

  if (ctx->mediaType->pbFormat == NULL) {
    printf("IAMStreamConfig.GetFormat() returned NULL pbFormat\n");
    return JNI_FALSE;
  }

  ctx->videoInfo = (VIDEOINFOHEADER*) ctx->mediaType->pbFormat;
  ctx->width = ctx->videoInfo->bmiHeader.biWidth;
  ctx->height = ctx->videoInfo->bmiHeader.biHeight;
  ctx->compression = ctx->videoInfo->bmiHeader.biCompression;

  if ((desiredWidth == -1) || (desiredHeight == -1)) {
    desiredWidth = ctx->width;
    desiredHeight = ctx->height;
  }

  //enumerate formats
  resetCameraModeList(ctx);
  while (true) {
    res = ctx->enumMediaTypes->Next(1, &ctx->mediaType, NULL);
    if (res != S_OK) {
      if (ctx->listModes) {
        break;
      }
      printf("Camera:Error:Unable to enumerate compatible video mode\n");
      return JNI_FALSE;
    }
    ctx->videoInfo = (VIDEOINFOHEADER*) ctx->mediaType->pbFormat;
    int enumWidth = ctx->videoInfo->bmiHeader.biWidth;
    int enumHeight = ctx->videoInfo->bmiHeader.biHeight;
    int bitCount = ctx->videoInfo->bmiHeader.biBitCount;
    int compression = ctx->videoInfo->bmiHeader.biCompression;
    printf("Camera:enumerate:size=%dx%d (%dbpp) (%x)\n", enumWidth, enumHeight, bitCount, compression);
    if (enumWidth == 0 || enumHeight == 0) {
      FreeMediaType(ctx->mediaType);
      continue;
    }
    if (bitCount == 0) {
      FreeMediaType(ctx->mediaType);
      continue;
    }
    if (ctx->listModes) {
      ctx->cameraModes[ctx->cameraModeCount] = (char*)malloc(16);
			sprintf(ctx->cameraModes[ctx->cameraModeCount], "%dx%d", enumWidth, enumHeight);
      ctx->cameraModeCount++;
      FreeMediaType(ctx->mediaType);
      continue;
    }
    if (enumWidth == desiredWidth && enumHeight == desiredHeight) {
      ctx->width = enumWidth;
      ctx->height = enumHeight;
      ctx->streamConfig->SetFormat(ctx->mediaType);
      FreeMediaType(ctx->mediaType);
      break;
    }
    FreeMediaType(ctx->mediaType);
  }

  ctx->streamConfig->Release();
  ctx->streamConfig = NULL;

  if (CoCreateInstance(CLSID_SampleGrabber, NULL, CLSCTX_INPROC_SERVER ,IID_IBaseFilter, (void**)&ctx->sampleGrabberBaseFilter) != S_OK) {
    printf("CoCreateInstance() failed\n");
    return JNI_FALSE;
  }

  if (ctx->graphBuilder->AddFilter(ctx->sampleGrabberBaseFilter,L"Sample Grabber") != S_OK) {
    printf("IGraphBuilder.AddFilter() failed\n");
    return JNI_FALSE;
  }

  if (ctx->sampleGrabberBaseFilter->QueryInterface(IID_ISampleGrabber, (void**)&ctx->sampleGrabber) != S_OK) {
    printf("IBaseFilter.QueryInterface() failed\n");
    return JNI_FALSE;
  }

  ctx->sampleGrabber->SetOneShot(FALSE);
  ctx->sampleGrabber->SetBufferSamples(TRUE);

  memset(&ctx->rgbMediaType, 0, sizeof(AM_MEDIA_TYPE));
  ctx->rgbMediaType.majortype = MEDIATYPE_Video;
  ctx->rgbMediaType.subtype = MEDIASUBTYPE_RGB32;
  ctx->rgbMediaType.formattype = FORMAT_VideoInfo;

  res = ctx->sampleGrabber->SetMediaType(&ctx->rgbMediaType);
  if (res != S_OK) {
    printf("ISampleGrabber.SetMediaType() failed : Result=0x%x\n", res);
    return JNI_FALSE;
  }

  if (CoCreateInstance(CLSID_NullRenderer, NULL, CLSCTX_INPROC_SERVER,IID_IBaseFilter, (void**)&ctx->destBaseFilter) != S_OK) {
    printf("CoCreateInstance() failed\n");
    return JNI_FALSE;
  }

  if (ctx->graphBuilder->AddFilter(ctx->destBaseFilter, L"NullRenderer") != S_OK) {
    printf("IGraphBuilder.AddFilter() failed\n");
    return JNI_FALSE;
  }

  if (ctx->captureGraphBuilder->RenderStream(&ctx->PIN, &MEDIATYPE_Video, ctx->videoInputFilter, ctx->sampleGrabberBaseFilter, ctx->destBaseFilter) != S_OK) {
    printf("ICaptureGraphBuilder2.RenderStream() failed\n");
    return JNI_FALSE;
  }

  if (!ctx->listModes) {
    res = ctx->mediaControl->Run();
    if (res == S_FALSE) res = S_OK;  //S_FALSE = preparing to run but not ready yet
    if (res != S_OK) {
      printf("IMediaControl.Run() failed\n");
      return JNI_FALSE;
    }
    printf("Camera Size:%dx%d (Compression=%x)\n", ctx->width, ctx->height, ctx->compression);
  }

  ctx->bufferSize = ctx->width * ctx->height * 4;
  ctx->buffer = malloc(ctx->bufferSize);

  ctx->videoInputFilter->Release();
  ctx->videoInputFilter = NULL;
  ctx->sampleGrabberBaseFilter->Release();
  ctx->sampleGrabberBaseFilter = NULL;
  ctx->destBaseFilter->Release();
  ctx->destBaseFilter = NULL;

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStop
  (JNIEnv *e, jobject c)
{
  printf("CameraStop\n");
  CamContext *ctx = getCamContext(e,c);
  if (ctx->mediaControl != NULL) {
    ctx->mediaControl->Stop();
  }
  return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_Camera_cameraGetFrame
  (JNIEnv *e, jobject c)
{
//  printf("CameraGetFrame\n");
  CamContext *ctx = getCamContext(e,c);
  if (ctx->sampleGrabber == NULL) return JNI_FALSE;
  int size = ctx->bufferSize;
  int status;

  status = ctx->sampleGrabber->GetCurrentBuffer((LONG*)&size, (LONG*)ctx->buffer);
  if (status == E_OUTOFMEMORY) {
    ctx->sampleGrabber->GetCurrentBuffer((LONG*)&size, (LONG*)NULL);
    printf("Camera:SampleGrabber->GetCurrentBuffer() (Buffer too small) (Requested Size=%d)\n", size);
    free(ctx->buffer);
    ctx->bufferSize = size;
    ctx->buffer = malloc(size);
    return NULL;
  }
  if (status != S_OK) {
    ctx->sampleGrabber->GetCurrentBuffer((LONG*)&size, (LONG*)NULL);
    printf("Camera:SampleGrabber->GetCurrentBuffer() = 0x%x (Requested Size=%d)\n", status, size);
    return NULL;
  }

  if (ctx->jpx == NULL) {
    ctx->jpx = e->NewIntArray(ctx->width * ctx->height);
    ctx->jpx = (jintArray)e->NewGlobalRef(ctx->jpx);  //ensure it is not gc'ed
  }

  //copy pixels, flip image, set opaque alpha channel
  jint *jpxptr = e->GetIntArrayElements(ctx->jpx,NULL);
  int jpxsize = e->GetArrayLength(ctx->jpx);

  jint *dst = jpxptr;
  jint *src = (jint*)ctx->buffer;
  src += ctx->width * (ctx->height-1);
  int w2 = ctx->width * 2;
  for(int y=0;y<ctx->height;y++) {
    for(int x=0;x<ctx->width;x++) {
      *(dst++) = *(src++) | 0xff000000;
    }
    src -= w2;
  }

  e->ReleaseIntArrayElements(ctx->jpx, jpxptr, 0);

  return ctx->jpx;
}

JNIEXPORT jint JNICALL Java_javaforce_media_Camera_cameraGetWidth
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  return ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_media_Camera_cameraGetHeight
  (JNIEnv *e, jobject c)
{
  CamContext *ctx = getCamContext(e,c);
  return ctx->height;
}
