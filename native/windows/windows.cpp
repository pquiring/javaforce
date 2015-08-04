#include <windows.h>

#include <objbase.h>
#include <dshow.h>
#include <qedit.h>

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "javaforce_jni_WinNative.h"
#include "javaforce_gl_GL.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

HMODULE jawt = NULL;
jboolean (JNICALL *_JAWT_GetAWT)(JNIEnv *e, JAWT *c) = NULL;

HMODULE wgl = NULL;

//open DLLs

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_winInit
  (JNIEnv *e, jclass c, jstring jawtPath)
{
  if (jawt == NULL) {
    const char *cjawtPath = e->GetStringUTFChars(jawtPath, NULL);
    jawt = LoadLibrary(cjawtPath);
    e->ReleaseStringUTFChars(jawtPath, cjawtPath);
    if (jawt == NULL) {
      printf("LoadLibrary(jawt.dll) failed\n");
      return JNI_FALSE;
    }
    const char *name;
    if (sizeof(void*) == 8)
      name = "JAWT_GetAWT";
    else
      name = "_JAWT_GetAWT@8";  //32bit oddity (forget to clean exports?)
    _JAWT_GetAWT = (jboolean (JNICALL *)(JNIEnv *e, JAWT *c))GetProcAddress(jawt, name);
  }
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

static HWND getHandle(JNIEnv *e, jobject c) {
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
  JAWT_Win32DrawingSurfaceInfo* wdsi = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
  if (wdsi == NULL) {
    printf("JAWT HWND == NULL\n");
    return NULL;
  }
  HWND handle = wdsi->hwnd;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

struct GLContext {
  HWND hwnd;
  HDC hdc;
  HGLRC ctx;
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

JNIEXPORT jboolean JNICALL Java_javaforce_gl_GL_glCreate
  (JNIEnv *e, jobject c, jobject canvas, jlong sharedCtx)
{
  GLContext *ctx = createGLContext(e,c);
  GLContext *ctx_shared = (GLContext*)sharedCtx;
  ctx->hwnd = getHandle(e,canvas);
  if (ctx->hwnd == NULL) {
    printf("glCreate:hwnd == NULL\n");
    ctx->Delete();
    return JNI_FALSE;
  }

  //now create gl context for handle

  ctx->hdc = GetDC(ctx->hwnd);
  PIXELFORMATDESCRIPTOR pfd;
  pfd.nSize = sizeof(pfd);
  pfd.nVersion = 1;
  pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
  pfd.iPixelType = PFD_TYPE_RGBA;
  pfd.cColorBits = 24;
  pfd.cDepthBits = 16;
  pfd.cStencilBits = 8;
  pfd.iLayerType = PFD_MAIN_PLANE;
  int pixelFormat = ChoosePixelFormat(ctx->hdc, &pfd);
  SetPixelFormat(ctx->hdc, pixelFormat, &pfd);
  if (ctx_shared == NULL) {
    ctx->ctx = wglCreateContext(ctx->hdc);
  } else {
    ctx->ctx = ctx_shared->ctx;
    ctx->shared = 1;
  }
  wglMakeCurrent(ctx->hdc, ctx->ctx);

  Java_javaforce_gl_GL_glInit(e,c);

  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glSetContext
  (JNIEnv *e, jobject c)
{
  GLContext *ctx = getGLContext(e,c);
  wglMakeCurrent(ctx->hdc, ctx->ctx);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glSwap
  (JNIEnv *e, jobject c)
{
  GLContext *ctx = getGLContext(e,c);
  SwapBuffers(ctx->hdc);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GL_glDelete
  (JNIEnv *e, jobject c)
{
  GLContext *ctx = getGLContext(e,c);
  wglMakeCurrent(NULL, NULL);
  if (ctx->shared == 0) {
    wglDeleteContext(ctx->ctx);
  }
  ctx->Delete();
}

#include "../common/gl.cpp"

//this func must be called only when a valid OpenGL context is set
JNIEXPORT void JNICALL Java_javaforce_gl_GL_glInit
  (JNIEnv *e, jobject c)
{
  if (funcs[0].func != NULL) return;  //already done
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
  jintArray jpx;

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

CamContext* createCamContext(JNIEnv *e, jobject c) {
  CamContext *ctx;
  jclass cls_camera = e->FindClass("javaforce/media/Camera");
  jfieldID fid_cam_ctx = e->GetFieldID(cls_camera, "ctx", "J");
  ctx = (CamContext*)e->GetLongField(c, fid_cam_ctx);
  if (ctx != NULL) {
    printf("Camera ctx used twice:%p\n", ctx);
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
      printf("camera=%ls\n", wstr);
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

/*
  //TODO : try to find a different size
  if ((desiredWidth == -1) || (desiredHeight == -1)) {
    desiredWidth = width;
    desiredHeight = height;
  }

  if (desiredWidth != ctx->width || desiredHeight != ctx->height) {
    //enumerate formats
    boolean ok = JNI_FALSE;
    while (true) {
      res = enumMediaTypes->Next(1, &enumMediaType, NULL);
      if (res != 0) break;
      videoInfo = mediaType.pbFormat;
      int enumWidth = videoInfo->bmiHeader.biWidth;
      int enumHeight = videoInfo->bmiHeader.biHeight;
      if (enumWidth == desiredWidth && enumHeight == desiredHeight) {
        width = enumWidth;
        height = enumHeight;
        //TODO : streamConfig->setFormat(enumMediaType);
      }
    }
  }
*/

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
  ctx->sampleGrabber->SetMediaType(&ctx->rgbMediaType);

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

  HRESULT res = ctx->mediaControl->Run();
  if (res == S_FALSE) res = S_OK;  //S_FALSE = preparing to run but not ready yet
  if (res != S_OK) {
    printf("IMediaControl.Run() failed\n");
    return JNI_FALSE;
  }

  printf("Camera Size:%dx%d\n", ctx->width, ctx->height);
  ctx->buffer = malloc(ctx->width * ctx->height * 4);

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
  int size = ctx->width * ctx->height * 4;

  if (ctx->sampleGrabber->GetCurrentBuffer((LONG*)&size, (LONG*)ctx->buffer) != S_OK) return NULL;

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
    UpdateResource((HANDLE)handle, (LPCSTR)RT_ICON, (LPCSTR)(a+1), EN_US, ii[a], iiSize[a]);
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_peAddString
  (JNIEnv *e, jclass c, jlong handle, jint type, jint idx, jbyteArray ba)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  UpdateResource((HANDLE)handle, (LPCSTR)type, (LPCSTR)idx, EN_US, baptr, e->GetArrayLength(ba));
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

#include "../common/ffmpeg.cpp"
