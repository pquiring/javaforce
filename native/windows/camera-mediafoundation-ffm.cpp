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

struct FFMCameraContext {
  int cameraDeviceCount;
  IMFActivate** devices;
  IMFSourceReader* reader;
  jboolean listModes;
  int cameraModeCount;
  char** cameraModes;
  int width, height;
};

FFMCameraContext* createFFMCameraContext() {
  FFMCameraContext* ctx;
  ctx = new FFMCameraContext();
  memset(ctx, 0, sizeof(FFMCameraContext));
  return ctx;
}

void deleteFFMCameraContext(FFMCameraContext* ctx) {
  if (ctx == NULL) return;
  delete ctx;
}

static void ffmResetCameraList(FFMCameraContext* ctx) {
  printf("MF:ffmResetCameraList\n");
  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    IMFActivate *cam = ctx->devices[a];
    cam->Release();
  }
  ctx->cameraDeviceCount = 0;
}

static void ffmResetCameraModeList(FFMCameraContext* ctx) {
  printf("MF:resetCameraModes\n");
  for(int a=0;a<ctx->cameraModeCount;a++) {
    char* mode = ctx->cameraModes[a];
    free(mode);
  }
  ctx->cameraModeCount = 0;
}

static void cameraReleaseAll(FFMCameraContext* ctx) {
}

jlong cameraInit()
{
  printf("MF:CameraInit\n");
  FFMCameraContext* ctx = createFFMCameraContext();
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

jboolean cameraUninit(jlong ctxptr)
{
  printf("MF:CameraUninit\n");
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;
  cameraReleaseAll(ctx);
  deleteFFMCameraContext(ctx);
  CoUninitialize();
  return JNI_TRUE;
}

JFArray* cameraListDevices(jlong ctxptr)
{
  printf("MF:CameraListDevices\n");
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
  if (ctx == NULL) return NULL;
  ffmResetCameraList(ctx);

  IMFAttributes* attr;

  if (MFCreateAttributes(&attr, 1) != S_OK) return NULL;

  if (attr->SetGUID(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE, MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID) != S_OK) return NULL;

  if (MFEnumDeviceSources(attr, &ctx->devices, (UINT32*)&ctx->cameraDeviceCount) != S_OK) return NULL;

  attr->Release();

  JFArray* strs = JFArray::create(ctx->cameraDeviceCount, sizeof(jchar*), ARRAY_TYPE_STRING);

  for(int a=0;a<ctx->cameraDeviceCount;a++) {
    UINT32 length;
    LPWSTR name;
    LPWSTR symlink;
    ctx->devices[a]->GetAllocatedString(MF_DEVSOURCE_ATTRIBUTE_FRIENDLY_NAME, &name, &length);
    ctx->devices[a]->GetAllocatedString(MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_SYMBOLIC_LINK, &symlink, &length);

    int strlen = strlen16((jchar*)name);
    char* str = (char*)malloc(strlen+1);
    strcpy8_16(str, (jchar*)name);
    strs->setString(a, str);

    CoTaskMemFree(name);
    CoTaskMemFree(symlink);
  }

  return strs;
}

jboolean cameraStart(jlong ctxptr, jint deviceIdx, jint desiredWidth, jint desiredHeight);
jboolean cameraStop(jlong ctxptr);

JFArray* cameraListModes(jlong ctxptr, jint deviceIdx)
{
  printf("MF:CameraListModes\n");
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
  if (ctx == NULL) return NULL;
  ffmResetCameraModeList(ctx);

  if (deviceIdx < 0 || deviceIdx >= ctx->cameraDeviceCount) return NULL;

  ctx->listModes = JNI_TRUE;
  cameraStart(ctxptr,deviceIdx,-1,-1);
  cameraStop(ctxptr);
  ctx->listModes = JNI_FALSE;

  JFArray* strs = JFArray::create(ctx->cameraModeCount, sizeof(jchar*), ARRAY_TYPE_STRING);

  for(int a=0;a<ctx->cameraModeCount;a++) {
    char* mode = ctx->cameraModes[a];
    int len8 = strlen(mode);
    char* str = (char*)malloc(len8+1);
    strcpy8(str, mode);
    strs->setString(a, str);
  }

  return strs;
}


jboolean cameraStart(jlong ctxptr, jint deviceIdx, jint desiredWidth, jint desiredHeight)
{
  printf("MF:CameraStart:deviceIdx=%d\n", deviceIdx);
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
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

jboolean cameraStop(jlong ctxptr)
{
  printf("MF:CameraStop\n");
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
  if (ctx == NULL) return JNI_FALSE;

  if (ctx->reader != NULL) {
    ctx->reader->Release();
    ctx->reader = NULL;
  }

  return JNI_TRUE;
}

JFArray* cameraGetFrame(jlong ctxptr)
{
//  printf("MF:CameraGetFrame\n");
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
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

  int pxsize = ctx->width * ctx->height;
  JFArray *px = JFArray::create(pxsize, 4, ARRAY_TYPE_INT);

  //copy pixels, flip image, set opaque alpha channel
  jint *pxptr = px->getBufferInt();

  jint *dst = pxptr;
  jint *src = (jint*)data;
  src += ctx->width * (ctx->height-1);
  int w2 = ctx->width * 2;
  for(int y=0;y<ctx->height;y++) {
    for(int x=0;x<ctx->width;x++) {
      *(dst++) = *(src++) | 0xff000000;
    }
    src -= w2;
  }

  buffer->Unlock();

  buffer->Release();

  sample->Release();

  return px;
}

jint cameraGetWidth(jlong ctxptr)
{
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
  return ctx->width;
}

jint cameraGetHeight(jlong ctxptr)
{
  FFMCameraContext* ctx = (FFMCameraContext*)ctxptr;
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
