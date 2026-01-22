#include <mfapi.h>
#include <mfobjects.h>
#include <mfidl.h>
#include <mfreadwrite.h>

/*

typedef struct _GUID { // size is 16
  DWORD Data1;
  WORD  Data2;
  WORD  Data3;
  BYTE  Data4[8];
} GUID;

*/

#define MAX_NUM_CAMERAS 32

struct CamContext {
  int cameraDeviceCount;
  IMFActivate** devices;
  IMFSourceReader* reader;
  jboolean listModes;
  int cameraModeCount;
  char** cameraModes;
  int width, height;
  jintArray jpx;
};

CamContext* createCamContext(JNIEnv *e, jobject c) {
  CamContext* ctx;
  ctx = new CamContext();
  memset(ctx, 0, sizeof(CamContext));
  return ctx;
}

void deleteCamContext(JNIEnv *e, jobject c, CamContext* ctx) {
  if (ctx == NULL) return;
  delete ctx;
}

static int strlen16(jchar *str) {
  int len = 0;
  while (*str != 0) {
    len++;
    str++;
  }
  return len;
}

static void resetCameraList(CamContext* ctx) {
  printf("MF:resetCameraList\n");
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    IMFActivate *cam = ctx->devices[a];
    cam->Release();
  }
  ctx->cameraDeviceCount = 0;
}

static void resetCameraModeList(CamContext* ctx) {
  printf("MF:resetCameraModes\n");
  for(int a=0;a<ctx->cameraModeCount;a++) {
    char* mode = ctx->cameraModes[a];
    free(mode);
  }
  ctx->cameraModeCount = 0;
}

static void cameraReleaseAll(JNIEnv *e, CamContext* ctx) {
  if (ctx->jpx != NULL) {
    e->DeleteGlobalRef(ctx->jpx);
    ctx->jpx = NULL;
  }
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_CameraJNI_cameraInit
  (JNIEnv *e, jobject c)
{
  printf("MF:CameraInit\n");
  CamContext* ctx = createCamContext(e,c);
  if (ctx == NULL) return 0;

  ctx->devices = (IMFActivate**)NULL;

  ctx->cameraDeviceCount = 0;

  ctx->cameraModes = (char**)malloc(sizeof(char*) * MAX_NUM_CAMERAS);
  memset(ctx->cameraModes, 0, sizeof(char*) * MAX_NUM_CAMERAS);

  ctx->cameraModeCount = 0;

  BOOL ok = CoInitializeEx(NULL, COINIT_MULTITHREADED) == S_OK;
  if (!ok) return NULL;

  ok = MFStartup(MF_VERSION, MFSTARTUP_NOSOCKET) == S_OK;
  if (!ok) return NULL;

  return (jlong)ctx;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CameraJNI_cameraUninit
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  printf("MF:CameraUninit\n");
  CamContext* ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  cameraReleaseAll(e,ctx);
  deleteCamContext(e,c,ctx);
  CoUninitialize();
  return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_CameraJNI_cameraListDevices
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  printf("MF:CameraListDevices\n");
  CamContext* ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return NULL;
  resetCameraList(ctx);

  IMFAttributes* attr;

  if (MFCreateAttributes(&attr, 1) != S_OK) return NULL;

  if (attr->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE, MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID) != S_OK) return NULL;

  if (MFEnumDeviceSources(attr, &ctx->devices, (UINT32*)&ctx->cameraDeviceCount) != S_OK) return NULL;

  attr->Release();

  jclass strcls = e->FindClass("java/lang/String");
  jobjectArray strs = e->NewObjectArray(ctx->cameraDeviceCount, strcls, NULL);
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    UINT32 length;
    LPWSTR name;
    LPWSTR symlink;
    ctx->devices[a]->GetAllocatedString(MF_DEVSOURCE_ATTRIBUTE_FRIENDLY_NAME, &name, &length);
    ctx->devices[a]->GetAllocatedString(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_SYMBOLIC_LINK, &symlink, &length);

    e->SetObjectArrayElement(strs, a, e->NewString((jchar*)name, strlen16((jchar*)name)));

    CoTaskMemFree(name);
    CoTaskMemFree(symlink);
  }

  return strs;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_CameraJNI_cameraListModes
  (JNIEnv *e, jobject c, jlong ctxptr, jint deviceIdx)
{
  printf("MF:CameraListModes\n");
  CamContext* ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return NULL;
  resetCameraModeList(ctx);

  if (deviceIdx < 0 || deviceIdx >= ctx->cameraDeviceCount) return NULL;

  ctx->listModes = JNI_TRUE;
  Java_javaforce_jni_CameraJNI_cameraStart(e,c,ctxptr,deviceIdx,-1,-1);
  Java_javaforce_jni_CameraJNI_cameraStop(e,c,ctxptr);
  ctx->listModes = JNI_FALSE;

  jclass strcls = e->FindClass("java/lang/String");
  jobjectArray strs = e->NewObjectArray(ctx->cameraModeCount, strcls, NULL);
  for(int a=0;a<ctx->cameraModeCount;a++) {
    e->SetObjectArrayElement(strs, a, e->NewStringUTF(ctx->cameraModes[a]));
  }

  return strs;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CameraJNI_cameraStart
  (JNIEnv *e, jobject c, jlong ctxptr, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  printf("MF:CameraStart:deviceIdx=%d\n", deviceIdx);
  CamContext* ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  HRESULT res;

  if (deviceIdx < 0 || deviceIdx >= ctx->cameraDeviceCount) return JNI_FALSE;

  IMFActivate *device = ctx->devices[deviceIdx];
  IMFAttributes* pAttr;
  IMFMediaSource *source;
  IMFPresentationDescriptor *presDesc;
  IMFStreamDescriptor *streamDesc;
  IMFMediaTypeHandler *typeHandler;
  IMFMediaType* type;
  GUID guid;
  UINT64 size;
  DWORD streamCount;
  DWORD mediaCount;
  BOOL selected;
//  OLECHAR* guidString;

  if (device->ActivateObject(IID_PPV_ARGS(&source)) != S_OK) {
    return JNI_FALSE;
  }

  if (MFCreateAttributes(&pAttr, 1) != S_OK) {
    return JNI_FALSE;
  }

  //enable video processing to allow reader to convert camera native format to RGB
  if (pAttr->SetUINT32(MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, TRUE) != S_OK) {
    return JNI_FALSE;
  }

  if (MFCreateSourceReaderFromMediaSource(source, pAttr, &ctx->reader) != S_OK) {
    return JNI_FALSE;
  }

  pAttr->Release();

  if (source->CreatePresentationDescriptor(&presDesc) != S_OK) {
    return JNI_FALSE;
  }

  source->Release();

  if (presDesc->GetStreamDescriptorCount(&streamCount) != S_OK) {
    return JNI_FALSE;
  }

  printf("MF streamCount=%d\n", streamCount);

  if (ctx->listModes) {
    ctx->cameraModeCount = 0;
  }

  for(int idx=0;idx<streamCount;idx++) {
    if (presDesc->GetStreamDescriptorByIndex(idx, &selected, &streamDesc) != S_OK) {
      continue;
    }

    if (streamDesc->GetMediaTypeHandler(&typeHandler) != S_OK) {
      continue;
    }

    if (typeHandler->GetMediaTypeCount(&mediaCount) != S_OK) {
      continue;
    }

    printf("MF mediaCount=%d\n", mediaCount);

    for(int idx2=0;idx2<mediaCount;idx2++) {
      if (typeHandler->GetMediaTypeByIndex(idx2, &type) != S_OK) {
        continue;
      }

      //get media type info

      if (type->GetGUID(MF_MT_SUBTYPE, &guid) != S_OK) {
        type->Release();
        continue;
      }

      if (type->GetUINT64(MF_MT_FRAME_SIZE, &size) != S_OK) {
        type->Release();
        continue;
      }

      int width = size >> 32;
      int height = size & 0xffffffff;

      if (ctx->listModes) {
        ctx->cameraModes[ctx->cameraModeCount] = (char*)malloc(16);
  			sprintf(ctx->cameraModes[ctx->cameraModeCount], "%dx%d", width, height);
        ctx->cameraModeCount++;
        type->Release();
        continue;
      }

//      StringFromCLSID(guid, &guidString);  //not needed, Data1 contains for FOURCC code
      printf("MF type=0x%08x size=%dx%d\n", guid.Data1, width, height);
//      CoTaskMemFree(guidString);
      if (width == desiredWidth && height == desiredHeight) {
        printf("MF:Using desired format:%d,%d\n", desiredWidth, desiredHeight);
        if (ctx->reader->SetCurrentMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, NULL, type) != S_OK) {
          printf("MF:SetCurrentMediaType failed!\n");
          return JNI_FALSE;
        }
        type->Release();
        break;
      }
      type->Release();
    }
    streamDesc->Release();
  }

  presDesc->Release();

  if (ctx->listModes) {
    return JNI_TRUE;
  }

  if (ctx->reader->GetCurrentMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, &type) != S_OK) {
    printf("MF:GetCurrentMediaType failed!\n");
    return JNI_FALSE;
  }

  if (type->GetUINT64(MF_MT_FRAME_SIZE, &size) != S_OK) {
    printf("MF:GetUINT64:MF_MT_FRAME_SIZE failed!\n");
    return JNI_FALSE;
  }

  ctx->width = size >> 32;
  ctx->height = size & 0xffffffff;

  //change media type of RGB

  if (MFCreateMediaType(&type) != S_OK) {
    return JNI_FALSE;
  }

  if (type->SetGUID(MF_MT_MAJOR_TYPE, MFMediaType_Video) != S_OK) {
    return JNI_FALSE;
  }

  if (type->SetGUID(MF_MT_SUBTYPE, MFVideoFormat_RGB32) != S_OK) {
    return JNI_FALSE;
  }

  if (ctx->reader->SetCurrentMediaType(MF_SOURCE_READER_FIRST_VIDEO_STREAM, NULL, type) != S_OK) {
    printf("MF:SetCurrentMediaType(RGB) failed!\n");
    return JNI_FALSE;
  }

  type->Release();

  printf("MF:Start okay\n");

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_CameraJNI_cameraStop
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  printf("MF:CameraStop\n");
  CamContext* ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;

  if (ctx->reader != NULL) {
    ctx->reader->Release();
    ctx->reader = NULL;
  }

  return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_CameraJNI_cameraGetFrame
  (JNIEnv *e, jobject c, jlong ctxptr)
{
//  printf("MF:CameraGetFrame\n");
  CamContext* ctx = (CamContext*)ctxptr;
  if (ctx == NULL) return NULL;

  IMFSample* sample;
  IMFMediaBuffer* buffer;
  DWORD stream;
  DWORD flags;
  LONGLONG timestamp;
  BYTE* data;
  DWORD size;

  for (;;) {
    if (ctx->reader->ReadSample(MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0, &stream, &flags, &timestamp, &sample) != S_OK) {
      return NULL;
    }
    if (flags & MF_SOURCE_READERF_STREAMTICK) {
      continue;
    }
    break;
  }

  if (sample->ConvertToContiguousBuffer(&buffer) != S_OK) {
    return NULL;
  }

  if (buffer->Lock(&data, NULL, &size) != S_OK) {
    return NULL;
  }

  if (ctx->jpx == NULL) {
    ctx->jpx = e->NewIntArray(ctx->width * ctx->height);
    ctx->jpx = (jintArray)e->NewGlobalRef(ctx->jpx);  //ensure it is not gc'ed
  }

  //copy pixels, flip image, set opaque alpha channel
  jint *jpxptr = e->GetIntArrayElements(ctx->jpx, NULL);
  int jpxsize = e->GetArrayLength(ctx->jpx);

  jint *dst = jpxptr;
  jint *src = (jint*)data;
  src += ctx->width * (ctx->height-1);
  int w2 = ctx->width * 2;
  for(int y=0;y<ctx->height;y++) {
    for(int x=0;x<ctx->width;x++) {
      *(dst++) = *(src++) | 0xff000000;
    }
    src -= w2;
  }

  e->ReleaseIntArrayElements(ctx->jpx, jpxptr, 0);

  buffer->Unlock();

  buffer->Release();

  sample->Release();

  return ctx->jpx;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_CameraJNI_cameraGetWidth
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext* ctx = (CamContext*)ctxptr;
  return ctx->width;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_CameraJNI_cameraGetHeight
  (JNIEnv *e, jobject c, jlong ctxptr)
{
  CamContext* ctx = (CamContext*)ctxptr;
  return ctx->height;
}
