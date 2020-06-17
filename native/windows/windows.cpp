#include <windows.h>
#include <ntddtape.h>

#include <objbase.h>
#include <dshow.h>
#include "qedit.h"

#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <cstring>

#include <jni.h>

#include "javaforce_jni_WinNative.h"
#include "javaforce_gl_GL.h"
#include "javaforce_gl_GLWindow.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"
#include "javaforce_controls_ni_DAQmx.h"
#include "javaforce_media_VideoBuffer.h"

HMODULE wgl = NULL;

//open DLLs

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_winInit
  (JNIEnv *e, jclass c)
{
  if (wgl == NULL) {
    wgl = LoadLibrary("opengl32.dll");
    if (wgl == NULL) {
      printf("LoadLibrary(opengl32.dll) failed\n");
      return JNI_FALSE;
    }
  }

  return JNI_TRUE;
}

//OpenGL API

#include "../common/glfw.cpp"

#define GLFW_EXPOSE_NATIVE_WIN32
#define GLFW_EXPOSE_NATIVE_WGL

#include "../glfw/include/GLFW/glfw3native.h"

JNIEXPORT void JNICALL Java_javaforce_gl_GLWindow_nseticon
  (JNIEnv *e, jclass c, jlong id, jstring filename, jint x, jint y)
{
  GLFWContext *ctx = (GLFWContext*)id;
  const char *cstr = e->GetStringUTFChars(filename,NULL);
  HANDLE icon = LoadImage(NULL, cstr, IMAGE_ICON, x, y, LR_LOADFROMFILE);
  e->ReleaseStringUTFChars(filename, cstr);
  HWND hwnd = glfwGetWin32Window(ctx->window);
  SendMessage(hwnd, WM_SETICON, ICON_BIG, (LPARAM)icon);
  SendMessage(hwnd, WM_SETICON, ICON_SMALL, (LPARAM)icon);
}

#include "../common/gl.cpp"

//this func must be called only when a valid OpenGL context is set
JNIEXPORT jboolean JNICALL Java_javaforce_gl_GL_glInit
  (JNIEnv *e, jclass c)
{
  if (funcs[0].func != NULL) return JNI_TRUE;  //already done
  if (wgl == NULL) return JNI_FALSE;
  void *func;
  for(int a=0;a<GL_NO_FUNCS;a++) {
    func = (void*)wglGetProcAddress(funcs[a].name);  //get OpenGL 1.x function
    if (func == NULL) {
      func = (void*)GetProcAddress(wgl, funcs[a].name);  //get OpenGL 2.0+ function
      if (func == NULL) {
        printf("glInit:Error:Can not find function:%s\n", funcs[a].name);
        continue;
      }
    }
    funcs[a].func = func;
  }
  return JNI_TRUE;
}

//Camera API

#ifdef __GNUC__
  //define missing from Mingw headers
  DEFINE_GUID(CLSID_SampleGrabber, 0xc1f400a0, 0x3f08, 0x11d3, 0x9f,0x0b, 0x00,0x60,0x08,0x03,0x9e,0x37);
  DEFINE_GUID(CLSID_NullRenderer , 0xc1f400a4, 0x3f08, 0x11d3, 0x9f,0x0b, 0x00,0x60,0x08,0x03,0x9e,0x37);
#endif

#define MAX_NUM_CAMERAS 32

struct CamContext {
  int cameraDeviceCount;
  jchar **cameraDeviceNames;
  IMoniker **cameraDevices;
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
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    IMoniker *cam = ctx->cameraDevices[a];
    cam->Release();
  }
  ctx->cameraDeviceCount = 0;
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
      //printf("camera=%ls\n", wstr);
      ctx->cameraDeviceNames[ctx->cameraDeviceCount] = wstr;
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

JNIEXPORT jboolean JNICALL Java_javaforce_media_Camera_cameraStart
  (JNIEnv *e, jobject c, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  CamContext *ctx = getCamContext(e,c);
  HRESULT res;

  if (deviceIdx >= ctx->cameraDeviceCount) return JNI_FALSE;
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
  while (true) {
    res = ctx->enumMediaTypes->Next(1, &ctx->mediaType, NULL);
    if (res != S_OK) {
      printf("Camera:Error:Unable to enumerate compatible video mode\n");
      return JNI_FALSE;
    }
    ctx->videoInfo = (VIDEOINFOHEADER*) ctx->mediaType->pbFormat;
    int enumWidth = ctx->videoInfo->bmiHeader.biWidth;
    int enumHeight = ctx->videoInfo->bmiHeader.biHeight;
    printf("Camera:enumerate:size=%dx%d\n", enumWidth, enumHeight);
    if (enumWidth == desiredWidth && enumHeight == desiredHeight) {
      ctx->width = enumWidth;
      ctx->height = enumHeight;
      ctx->streamConfig->SetFormat(ctx->mediaType);
      break;
    }
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

  res = ctx->mediaControl->Run();
  if (res == S_FALSE) res = S_OK;  //S_FALSE = preparing to run but not ready yet
  if (res != S_OK) {
    printf("IMediaControl.Run() failed\n");
    return JNI_FALSE;
  }

  printf("Camera Size:%dx%d (Compression=%x)\n", ctx->width, ctx->height, ctx->compression);
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
  CamContext *ctx = getCamContext(e,c);
  if (ctx->mediaControl != NULL) {
    ctx->mediaControl->Stop();
  }
  return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL Java_javaforce_media_Camera_cameraGetFrame
  (JNIEnv *e, jobject c)
{
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

//winPE resources

struct ICONENTRY {
  jbyte width, height, clrCnt, reserved;
  jshort planes, bitCnt;
  jint bytesInRes, imageOffset;
};
struct ICONHEADER {
  jshort res, type, count;
//  ICONENTRY entries[1];
};
struct GRPICONENTRY {
  jbyte width;
  jbyte height;
  jbyte colourCount;
  jbyte reserved;
  jbyte planes;
  jbyte bitCount;
  jshort bytesInRes;
  jshort bytesInRes2;
  jshort reserved2;
  jshort id;
};
struct GRPICONHEADER {
  jshort res, type, count;
//  GRPICONENTRY entries[1];
};
struct ICONIMAGE {  //actually a BITMAPINFO struct + xors_ands
  BITMAPINFOHEADER header;
//  RGBQUAD colors[];
//  byte xors_ands[];
};

#define EN_US 0x409  //MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US)

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_peBegin
  (JNIEnv *e, jclass c, jstring file)
{
  const char *cstr = e->GetStringUTFChars(file,NULL);
  HANDLE handle = BeginUpdateResource(cstr, FALSE);
  e->ReleaseStringUTFChars(file, cstr);
  return (jlong)handle;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_peAddIcon
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);

  ICONHEADER i;
  ICONHEADER *ih;
    ICONENTRY *ihE;
  ICONIMAGE **ii;
    int *iiSize;
  GRPICONHEADER *grp;
    GRPICONENTRY *grpE;

  int ptr = 0;
  int size = sizeof(ICONHEADER);
  memcpy(&i, baptr, size);
  size = sizeof(ICONHEADER) + i.count * sizeof(ICONENTRY);
  ih = (ICONHEADER*)malloc(size);
  memcpy(ih, baptr, size);
  ihE = (ICONENTRY*)(((char*)ih) + sizeof(ICONHEADER));
  ii = (ICONIMAGE**)malloc(sizeof(ICONIMAGE*) * i.count);
  iiSize = (int*)malloc(sizeof(int) * i.count);
  for(int a=0;a<i.count;a++) {
    ptr = ihE[a].imageOffset;
    iiSize[a] = ihE[a].bytesInRes;
    ii[a] = (ICONIMAGE*)malloc(iiSize[a]);
    memcpy(ii[a], baptr + ptr, iiSize[a]);
  }
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
  size = sizeof(GRPICONHEADER) + sizeof(GRPICONENTRY) * i.count;
  grp = (GRPICONHEADER*)malloc(size);
  memset(grp, 0, size);
  grpE = (GRPICONENTRY*)(((char*)grp) + sizeof(GRPICONHEADER));
  grp->res = 0;
  grp->type = 1;
  grp->count = i.count;
  for(int a=0;a<i.count;a++) {
    ICONENTRY *ie = &ihE[a];
    GRPICONENTRY *ge = &grpE[a];
    ge->bitCount = 0;
    ge->bytesInRes = ie->bitCnt;
    ge->bytesInRes2 = (short)ie->bytesInRes;
    ge->colourCount = ie->clrCnt;
    ge->height = ie->height;
    ge->id = (short)(a+1);
    ge->planes = (byte)ie->planes;
    ge->reserved = ie->reserved;
    ge->width = ie->width;
    ge->reserved2 = 0;
  }

  UpdateResource((HANDLE)handle, (LPCSTR)RT_GROUP_ICON, (LPCSTR)1, EN_US, grp, sizeof(GRPICONHEADER) + sizeof(GRPICONENTRY) * i.count);

  for(int a=0;a<i.count;a++) {
    UpdateResource((HANDLE)handle, (LPCSTR)RT_ICON, (LPCSTR)(jlong)(a+1), EN_US, ii[a], iiSize[a]);
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_peAddString
  (JNIEnv *e, jclass c, jlong handle, jint type, jint idx, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  UpdateResource((HANDLE)handle, (LPCSTR)(jlong)type, (LPCSTR)(jlong)idx, EN_US, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_peEnd
  (JNIEnv *e, jclass c, jlong handle)
{
  EndUpdateResource((HANDLE)handle, FALSE);
}

//com port API

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_comOpen
  (JNIEnv *e, jclass c, jstring str, jint baud)
{
  const char *cstr = e->GetStringUTFChars(str,NULL);
  HANDLE handle = CreateFileA(cstr, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
  e->ReleaseStringUTFChars(str, cstr);
  if (handle == INVALID_HANDLE_VALUE) return 0;
  DCB dcb;
  memset(&dcb, 0, sizeof(DCB));
  dcb.DCBlength = sizeof(DCB);
  GetCommState(handle, &dcb);
  dcb.BaudRate = baud;
  dcb.fBinary = 1;
  dcb.ByteSize = 8;  //8 data bits
  dcb.StopBits = ONESTOPBIT;  //1 stop bit
  SetCommState(handle, &dcb);
  COMMTIMEOUTS cto;
  memset(&cto, 0, sizeof(COMMTIMEOUTS));
  GetCommTimeouts(handle, &cto);
  cto.ReadIntervalTimeout = 10;
  cto.ReadTotalTimeoutMultiplier = 0;
  cto.ReadTotalTimeoutConstant = 100;
  cto.WriteTotalTimeoutMultiplier = 0;
  cto.WriteTotalTimeoutConstant = 0;
  SetCommTimeouts(handle, &cto);
  return (jlong)handle;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_comClose
  (JNIEnv *e, jclass c, jlong handle)
{
  CloseHandle((HANDLE)handle);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_comRead
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int read;
  ReadFile((HANDLE)handle, baptr, e->GetArrayLength(ba), (LPDWORD)&read, NULL);
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_comWrite
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int write;
  WriteFile((HANDLE)handle, baptr, e->GetArrayLength(ba), (LPDWORD)&write, NULL);
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
  return write;
}

//Windows

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_getWindowRect
  (JNIEnv *e, jclass c, jstring name, jintArray rect)
{
  jint *rectptr = e->GetIntArrayElements(rect,NULL);
  const char *cstr = e->GetStringUTFChars(name,NULL);
  HWND hwnd = FindWindow(NULL, cstr);
  RECT winrect;
  jboolean ok = JNI_FALSE;
  if (hwnd != NULL) {
    if (GetWindowRect(hwnd, &winrect)) {
      rectptr[0] = winrect.left;
      rectptr[1] = winrect.top;
      rectptr[2] = winrect.right - winrect.left;
      rectptr[3] = winrect.bottom - winrect.top;
      ok = JNI_TRUE;
    }
  }
  e->ReleaseStringUTFChars(name, cstr);
  e->ReleaseIntArrayElements(rect, rectptr, JNI_COMMIT);
  return ok;
}

//impersonate user

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_impersonateUser
  (JNIEnv *e, jclass c, jstring domain, jstring user, jstring passwd)
{
  HANDLE token;
  int ok;

  const char *cdomain = e->GetStringUTFChars(domain,NULL);
  const char *cuser = e->GetStringUTFChars(user,NULL);
  const char *cpasswd = e->GetStringUTFChars(passwd,NULL);
  ok = LogonUser(cuser, cdomain, cpasswd, LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_DEFAULT, &token);
  e->ReleaseStringUTFChars(domain, cdomain);
  e->ReleaseStringUTFChars(user, cuser);
  e->ReleaseStringUTFChars(passwd, cpasswd);
  if (!ok) return JNI_FALSE;
  ok = ImpersonateLoggedOnUser(token);
  if (!ok) {
    CloseHandle(token);
  }
  return ok ? JNI_TRUE : JNI_FALSE;
}

//find JDK Home

JNIEXPORT jstring Java_javaforce_jni_WinNative_findJDKHome(JNIEnv *e, jclass c) {
  //try to find JDK in Registry
  HKEY key, subkey;
  int type;
  int size;
  char version[MAX_PATH];
  char path[MAX_PATH];

  if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, "Software\\JavaSoft\\JDK", 0, KEY_READ, &key) != 0) {
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, "Software\\JavaSoft\\Java Development Kit", 0, KEY_READ, &key) != 0) {
      return NULL;
    }
  }

  size = 0;
  if (RegQueryValueEx(key, "CurrentVersion", 0, (LPDWORD)&type, 0, (LPDWORD)&size) != 0 || (type != REG_SZ) || (size > MAX_PATH)) {
    return NULL;
  }

  size = MAX_PATH;
  if (RegQueryValueEx(key, "CurrentVersion", 0, 0, (LPBYTE)version, (LPDWORD)&size) != 0) {
    return NULL;
  }

  if (RegOpenKeyEx(key, version, 0, KEY_READ, &subkey) != 0) {
    return NULL;
  }

  size = 0;
  if (RegQueryValueEx(subkey, "JavaHome", 0, (LPDWORD)&type, 0, (LPDWORD)&size) != 0 || (type != REG_SZ) || (size > MAX_PATH)) {
    return NULL;
  }

  size = MAX_PATH;
  if (RegQueryValueEx(subkey, "JavaHome", 0, 0, (LPBYTE)path, (LPDWORD)&size) != 0) {
    return NULL;
  }

  RegCloseKey(key);
  RegCloseKey(subkey);
  return e->NewStringUTF(path);
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_WinNative_getConsoleSize
  (JNIEnv *e, jclass c)
{
  CONSOLE_SCREEN_BUFFER_INFO info;
  int xy[2];
  GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
  xy[0] = info.srWindow.Right - info.srWindow.Left + 1;
  xy[1] = info.srWindow.Bottom - info.srWindow.Top + 1;
  jintArray ia = e->NewIntArray(2);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)xy);
  return ia;
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_WinNative_getConsolePos
  (JNIEnv *e, jclass c)
{
  CONSOLE_SCREEN_BUFFER_INFO info;
  int xy[2];
  GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
  xy[0] = info.dwCursorPosition.X - info.srWindow.Left + 1;
  xy[1] = info.dwCursorPosition.Y - info.srWindow.Top + 1;
  jintArray ia = e->NewIntArray(2);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)xy);
  return ia;
}

static DWORD input_console_mode;
static DWORD output_console_mode;
static char console_buffer[8];

#ifndef ENABLE_PROCESSED_INPUT
#define ENABLE_PROCESSED_INPUT 0x0001
#endif

#ifndef ENABLE_VIRTUAL_TERMINAL_INPUT
#define ENABLE_VIRTUAL_TERMINAL_INPUT 0x0200
#endif

#ifndef ENABLE_PROCESSED_OUTPUT
#define ENABLE_PROCESSED_OUTPUT 0x0001
#endif

#ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
#define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
#endif

#ifndef DISABLE_NEWLINE_AUTO_RETURN
#define DISABLE_NEWLINE_AUTO_RETURN 0x0008
#endif

#ifndef KEY_EVENT
#define KEY_EVENT 0x0001
#endif

static void StringCopy(char *dest, const char *src) {
  while (*src != 0) {
    *(dest++) = (*src++);
  }
  *dest = *src;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_enableConsoleMode
  (JNIEnv *e, jclass c)
{
  GetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), &input_console_mode);
  GetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), &output_console_mode);

  if (!SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), ENABLE_VIRTUAL_TERMINAL_INPUT)) {
    printf("Error:Unable to set stdin mode\n");
    exit(1);
  }
  if (!SetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), ENABLE_PROCESSED_OUTPUT | ENABLE_VIRTUAL_TERMINAL_PROCESSING | DISABLE_NEWLINE_AUTO_RETURN)) {
    printf("Error:Unable to set stdout mode\n");
    exit(1);
  }
  console_buffer[0] = 0;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_disableConsoleMode
  (JNIEnv *e, jclass c)
{
  SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), input_console_mode);
  SetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), output_console_mode);
}

JNIEXPORT jchar JNICALL Java_javaforce_jni_WinNative_readConsole
  (JNIEnv *e, jclass c)
{
  INPUT_RECORD input;
  DWORD read;
  if (console_buffer[0] != 0) {
    char ret = console_buffer[0];
    StringCopy(console_buffer, console_buffer+1);
    return (jchar)ret;
  }
  ReadConsoleInput(GetStdHandle(STD_INPUT_HANDLE), &input, 1, &read);
  if (input.EventType != KEY_EVENT) return 0;
  if (!input.Event.KeyEvent.bKeyDown) return 0;
  if (input.Event.KeyEvent.uChar.AsciiChar != 0) {
    char ch = input.Event.KeyEvent.uChar.AsciiChar;
    if (ch == 0x1b) {
      //is it Escape key or ANSI code???
      for(int a=0;a<10;a++) {
        read = 0;
        PeekConsoleInput(GetStdHandle(STD_INPUT_HANDLE), &input, 1, &read);
        if (read == 1) {
          char ch2 = input.Event.KeyEvent.uChar.AsciiChar;
          if (input.EventType == KEY_EVENT && input.Event.KeyEvent.bKeyDown && ch2 != 0) {
            if (ch2 != 0x1b) {
              //must be an ANSI code
              return (jchar)ch;
            } else {
              //multiple esc chars - prev must be esc key
              break;
            }
          } else {
            //ignore non-ascii events
            ReadConsoleInput(GetStdHandle(STD_INPUT_HANDLE), &input, 1, &read);
            continue;
          }
        }
        Sleep(10);
      }
      //it must be Escape key
      StringCopy(console_buffer, "[1~");  //custom code
    }
    if (ch == 13) ch = 10;  //linux style
    return (jchar)ch;
  }
  bool shift = input.Event.KeyEvent.dwControlKeyState & SHIFT_PRESSED;
  bool ctrl = input.Event.KeyEvent.dwControlKeyState & (LEFT_CTRL_PRESSED | RIGHT_CTRL_PRESSED);
  bool alt = input.Event.KeyEvent.dwControlKeyState & (LEFT_ALT_PRESSED | RIGHT_ALT_PRESSED);
  char code = 0;
  if (shift && ctrl && alt) {
    code = '8';
  } else if (ctrl && alt) {
    code = '7';
  } else if (ctrl && shift) {
    code = '6';
  } else if (ctrl) {
    code = '5';
  } else if (alt && shift) {
    code = '4';
  } else if (alt) {
    code = '3';
  } else if (shift) {
    code = '2';
  }
  switch (input.Event.KeyEvent.wVirtualKeyCode) {
    case VK_ESCAPE: StringCopy(console_buffer, "\x1b[1~"); break;  //custom
    case VK_INSERT: StringCopy(console_buffer, "\x1b[2~"); break;
    case VK_DELETE: StringCopy(console_buffer, "\x1b[3~"); break;
    case VK_UP: StringCopy(console_buffer, "\x1b[1;0A"); break;
    case VK_DOWN: StringCopy(console_buffer, "\x1b[1;0B"); break;
    case VK_RIGHT: StringCopy(console_buffer, "\x1b[1;0C"); break;
    case VK_LEFT: StringCopy(console_buffer, "\x1b[1;0D"); break;
    case VK_HOME: StringCopy(console_buffer, "\x1b[1;0H"); break;
    case VK_END: StringCopy(console_buffer, "\x1b[1;0F"); break;
  }
  if (console_buffer[0] != 0) {
    if (code > 0 && console_buffer[3] != '~') {
      console_buffer[4] = code;
    }
    char ret = console_buffer[0];
    StringCopy(console_buffer, console_buffer+1);
    return (jchar)ret;
  }
  return 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_peekConsole
  (JNIEnv *e, jclass c)
{
  DWORD count;
  GetNumberOfConsoleInputEvents(GetStdHandle(STD_INPUT_HANDLE), &count);
  return count != 0;
}

//tape drive API

static int tapeLastError;

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_tapeOpen
  (JNIEnv *e, jclass c, jstring name)
{
  const char *cstr = e->GetStringUTFChars(name,NULL);
  HANDLE handle = CreateFileA(cstr, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, FILE_FLAG_NO_BUFFERING, NULL);
  e->ReleaseStringUTFChars(name, cstr);
  if (handle == INVALID_HANDLE_VALUE) {
    tapeLastError = GetLastError();
    return 0;
  }
  return (jlong)handle;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_tapeClose
  (JNIEnv *e, jclass c, jlong handle)
{
  CloseHandle((HANDLE)handle);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeRead
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba, jint offset, jint length)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int read = 0;
  ReadFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&read, NULL);
  tapeLastError = GetLastError();
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeWrite
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba, jint offset, jint length)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int write = 0;
  WriteFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&write, NULL);
  tapeLastError = GetLastError();
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
  return write;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_tapeSetpos(JNIEnv *e, jclass c, jlong handle, jlong pos)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_SET_POSITION tapePos;
  tapePos.Method = pos == 0 ? TAPE_REWIND : TAPE_LOGICAL_BLOCK;
  tapePos.Partition = 0;
  tapePos.Offset.QuadPart = pos;
  tapePos.Immediate = FALSE;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_SET_POSITION,
    &tapePos,
    sizeof(tapePos),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastError = GetLastError();
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_tapeGetpos(JNIEnv *e, jclass c, jlong handle)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_GET_POSITION tapePos;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_POSITION,
    nullptr,
    0,
    &tapePos,
    sizeof(tapePos),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastError = GetLastError();
    return -1;
  }
  return tapePos.Offset.QuadPart;
}

static jlong tape_media_size;
static jboolean tape_media_readonly;

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_tapeMedia(JNIEnv *e, jclass c, jlong handle)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_GET_MEDIA_PARAMETERS params;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_MEDIA_PARAMS,
    nullptr,
    0,
    &params,
    sizeof(params),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastError = GetLastError();
    return JNI_FALSE;
  }
  tape_media_size = params.Capacity.QuadPart;
  tape_media_readonly = params.WriteProtected;
  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_tapeMediaSize(JNIEnv *e, jclass c)
{
  return tape_media_size;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_tapeMediaReadOnly(JNIEnv *e, jclass c)
{
  return tape_media_readonly;
}

static jint tape_drive_def_blocksize;
static jint tape_drive_max_blocksize;
static jint tape_drive_min_blocksize;

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_tapeDrive(JNIEnv *e, jclass c, jlong handle)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_GET_DRIVE_PARAMETERS params;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_DRIVE_PARAMS,
    nullptr,
    0,
    &params,
    sizeof(params),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastError = GetLastError();
    return JNI_FALSE;
  }
  tape_drive_def_blocksize = params.DefaultBlockSize;
  tape_drive_max_blocksize = params.MaximumBlockSize;
  tape_drive_min_blocksize = params.MinimumBlockSize;
  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeDriveMinBlockSize(JNIEnv *e, jclass c)
{
  return tape_drive_min_blocksize;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeDriveMaxBlockSize(JNIEnv *e, jclass c)
{
  return tape_drive_max_blocksize;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeDriveDefaultBlockSize(JNIEnv *e, jclass c)
{
  return tape_drive_def_blocksize;
}

//tape changer

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_changerOpen(JNIEnv *e, jclass c, jstring name)
{
  const char *cstr = e->GetStringUTFChars(name, NULL);
  HANDLE handle = CreateFileA(cstr, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
  e->ReleaseStringUTFChars(name, cstr);
  if (handle == INVALID_HANDLE_VALUE) {
    tapeLastError = GetLastError();
    return 0;
  }
  return (jlong)handle;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_changerClose(JNIEnv *e, jclass c, jlong handle)
{
  CloseHandle((HANDLE)handle);
}

static int list_count;
static char* list_elements[32*4];

static int listType(HANDLE dev, _ELEMENT_TYPE type, const char* name) {
  CHANGER_READ_ELEMENT_STATUS request;
  DWORD bytesReturn;
  CHANGER_ELEMENT_STATUS_EX status;
  for(int idx=0;idx<32;idx++) {
    request.ElementList.Element.ElementType = type;
    request.ElementList.Element.ElementAddress = idx;
    request.ElementList.NumberOfElements = 1;
    request.VolumeTagInfo = TRUE;
    BOOL ret = DeviceIoControl(
      dev,
      IOCTL_CHANGER_GET_ELEMENT_STATUS,
      &request,
      sizeof(request),
      &status,
      sizeof(status),
      &bytesReturn,
      nullptr
    );
    if (ret == FALSE) break;
    if (status.ExceptionCode == ERROR_SLOT_NOT_PRESENT) break;
    BOOL hasTape = (status.Flags & ELEMENT_STATUS_FULL) != 0;
    if (hasTape) {
      //trim barcode
      char *barcode = (char*)status.PrimaryVolumeID;
      for(int a=0;a<MAX_VOLUME_ID_SIZE;a++) {
        if (barcode[a] == ' ') barcode[a] = 0;
      }
    }
    list_elements[list_count] = (char*)malloc(128);
    std::sprintf(list_elements[list_count], "%s%d:%s", name, idx+1, (hasTape ? (const char*)status.PrimaryVolumeID : "<empty>"));
    list_count++;
  }
  return 0;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_WinNative_changerList(JNIEnv *e, jclass c, jlong handle)
{
  HANDLE dev = (HANDLE)handle;

  list_count = 0;

  listType(dev, ELEMENT_TYPE::ChangerDrive, "drive");
  listType(dev, ELEMENT_TYPE::ChangerTransport, "transport");
  listType(dev, ELEMENT_TYPE::ChangerSlot, "slot");
  listType(dev, ELEMENT_TYPE::ChangerIEPort, "port");

  jobjectArray ret = (jobjectArray)e->NewObjectArray(list_count,e->FindClass("java/lang/String"),e->NewStringUTF(""));

  for(int i=0;i<list_count;i++) {
    e->SetObjectArrayElement(ret,i,e->NewStringUTF(list_elements[i]));
    free(list_elements[i]);
  }

  return ret;
}

static BOOL startsWith(const char* str, const char* with) {
  while (*str && *with) {
    if (*str != *with) return FALSE;
    str++;
    with++;
  }
  if (*with == 0) return TRUE;
  return FALSE;
}

static BOOL isValidElement(const char* loc) {
  int len = -1;
  if (startsWith(loc, "drive")) len = 5;
  else if (startsWith(loc, "slot")) len = 4;
  else if (startsWith(loc, "port")) len = 4;
  else if (startsWith(loc, "transport")) len = 9;
  if (len == -1) return FALSE;
  loc += len;
  if (*loc < '1' || *loc > '9') return FALSE;
  loc++;
  while (*loc) {
    if (*loc < '0' || *loc > '9') return FALSE;
    loc++;
  }
  return TRUE;
}

static ELEMENT_TYPE getElementType(const char* loc) {
  if (startsWith(loc, "drive")) return ELEMENT_TYPE::ChangerDrive;
  if (startsWith(loc, "slot")) return ELEMENT_TYPE::ChangerSlot;
  if (startsWith(loc, "port")) return ELEMENT_TYPE::ChangerIEPort;
  if (startsWith(loc, "transport")) return ELEMENT_TYPE::ChangerTransport;
  return ELEMENT_TYPE::AllElements;
}

static int getElementAddress(const char* loc) {
  int len = -1;
  if (startsWith(loc, "drive")) len = 5;
  else if (startsWith(loc, "slot")) len = 4;
  else if (startsWith(loc, "port")) len = 4;
  else if (startsWith(loc, "transport")) len = 9;
  loc += len;
  int value = std::atoi(loc);
  if (value < 1 || value > 32) {
    printf("Error:location invalid");
    return -1;
  }
  return value - 1;  //return zero based
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_changerMove(JNIEnv *e, jclass c, jlong handle, jstring jsrc, jstring jtransport, jstring jdst)
{
  HANDLE dev = (HANDLE)handle;

  const char *src = e->GetStringUTFChars(jsrc,NULL);
  const char *transport = nullptr;
  const char *dst = e->GetStringUTFChars(jdst,NULL);

  if (jtransport != nullptr) transport = e->GetStringUTFChars(jtransport,NULL);

  if (!isValidElement(src)) {
    printf("Error:src invalid\n");
    return JNI_FALSE;
  }
  if (getElementType(src) == ELEMENT_TYPE::ChangerTransport) {
    printf("Error:src can not be transport\n");
    return JNI_FALSE;
  }

  if (transport != nullptr) {
    if (!isValidElement(transport)) {
      printf("Error:transport invalid\n");
      return JNI_FALSE;
    }
    if (getElementType(transport) != ELEMENT_TYPE::ChangerTransport) {
      printf("Error:transport must be transport");
      return JNI_FALSE;
    }
  }

  if (!isValidElement(dst)) {
    printf("Error:dst invalid\n");
    return JNI_FALSE;
  }
  if (getElementType(dst) == ELEMENT_TYPE::ChangerTransport) {
    printf("Error:dst can not be transport\n");
    return JNI_FALSE;
  }

  CHANGER_MOVE_MEDIUM request;
  DWORD bytesReturn;

  if (transport == nullptr) {
    request.Transport.ElementType = ELEMENT_TYPE::ChangerTransport;
    request.Transport.ElementAddress = 0;
  } else {
    request.Transport.ElementType = getElementType(transport);
    request.Transport.ElementAddress = getElementAddress(transport);
  }

  request.Source.ElementType = getElementType(src);
  request.Source.ElementAddress = getElementAddress(src);

  request.Destination.ElementType = getElementType(dst);
  request.Destination.ElementAddress = getElementAddress(dst);

  request.Flip = FALSE;

  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_CHANGER_MOVE_MEDIUM,
    &request,
    sizeof(request),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  e->ReleaseStringUTFChars(jsrc, src);
  if (transport != nullptr) e->ReleaseStringUTFChars(jtransport, transport);
  e->ReleaseStringUTFChars(jdst, dst);
  if (ret != TRUE) {
    tapeLastError = GetLastError();
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT int JNICALL Java_javaforce_jni_WinNative_tapeLastError
  (JNIEnv *e, jclass c, jlong handle)
{
  return tapeLastError;
}

//test

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_add
  (JNIEnv *e, jclass c, jint x, jint y)
{
  return x+y;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_hold
  (JNIEnv *e, jclass c, jintArray a, jint ms)
{
  jboolean isCopy;
  jint *aptr = (jint*)e->GetPrimitiveArrayCritical(a, &isCopy);

  ::Sleep(ms);

  e->ReleasePrimitiveArrayCritical(a, aptr, JNI_COMMIT);
}


#include "../common/library.h"

#include "../common/ffmpeg.cpp"

#include "../common/videobuffer.cpp"

#include "../common/ni.cpp"
