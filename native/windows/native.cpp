#include <windows.h>
#include <userenv.h>
#include <sas.h>
#include <wtsapi32.h>
#ifndef _NTDDTAPE_
#include <ntddtape.h>
#endif
#include <winsafer.h>

#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <cstring>

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "../common/string.h"
#include "../common/array.h"
#include "../common/library.h"
#include "../common/register.h"

HMODULE wgl = NULL;

JF_LIB_HANDLE jawt_dll;
jboolean (JNICALL *_JAWT_GetAWT)(JNIEnv *e, JAWT *c) = NULL;

//OpenGL API

#include "../common/ui.cpp"

#define GLFW_EXPOSE_NATIVE_WIN32
#define GLFW_EXPOSE_NATIVE_WGL

#include "../glfw/include/GLFW/glfw3native.h"

void uiWindowSetIcon(GLFWContextFFM* ctx, const char* filename, jint x, jint y)
{
  if (ctx == NULL) return;
  HANDLE icon = LoadImage(NULL, filename, IMAGE_ICON, x, y, LR_LOADFROMFILE);
  HWND hwnd = glfwGetWin32Window(ctx->window);
  SendMessage(hwnd, WM_SETICON, ICON_BIG, (LPARAM)icon);
  SendMessage(hwnd, WM_SETICON, ICON_SMALL, (LPARAM)icon);
}

extern "C" {
  JNIEXPORT void (*_uiWindowSetIcon)(GLFWContextFFM*,const char*,jint,jint) = &uiWindowSetIcon;
}

#include "../common/gl.cpp"

jboolean glPlatformInit()
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

jboolean glGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)wglGetProcAddress(name);  //get OpenGL 1.x function
  if (func == NULL) {
    func = (void*)GetProcAddress(wgl, name);  //get OpenGL 2.0+ function
  }
  if (func != NULL) {
    *funcPtr = func;
    return JNI_TRUE;
  } else {
    printf("OpenGL:Error:Can not find function:%s\n", name);
    return JNI_FALSE;
  }
}

//Camera API (only enable one)

#if 0
//VFW (Win95 era) Camera API
#include "camera-vfw.cpp"  //lost in space and time
#endif

#if 0
//DirectShow (WinXP era) Camera API
#include "camera-directshow.cpp"
#endif

#if 1
//MediaFoundation (WinVista era) Camera API
#include "camera-mediafoundation.cpp"
#endif

#include "pe.cpp"

#include "windows.cpp"

#include "user.cpp"

//find JDK Home

char jdk_path[MAX_PATH];

const char* findJDKHome() {
  //try to find JDK in Registry
  HKEY key, subkey;
  int type;
  int size;
  char version[MAX_PATH];

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
  if (RegQueryValueEx(subkey, "JavaHome", 0, 0, (LPBYTE)jdk_path, (LPDWORD)&size) != 0) {
    return NULL;
  }

  RegCloseKey(key);
  RegCloseKey(subkey);
  return jdk_path;
}

extern "C" {
  JNIEXPORT const char* (*_findJDKHome)() = &findJDKHome;
}

#include "console.cpp"

#include "tape.cpp"

#include "../common/ffmpeg.cpp"

#include "../common/videobuffer.cpp"

#include "../common/opencl.cpp"

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

#include "../common/pcap.cpp"

#include "../speexdsp/speex_dsp.c"

#include "vss.cpp"

#include "comport.cpp"

#include "monitor-folder.cpp"

#include "pipes.cpp"

static jlong getHWND(JNIEnv *e, jobject c) {
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  if (jawt_dll == NULL) return 0;
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
  JAWT_Win32DrawingSurfaceInfo* xdsi = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
  if (xdsi == NULL) {
    printf("JAWT.platformInfo == NULL\n");
    return 0;
  }
  jlong handle = (jlong)xdsi->hwnd;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

/*
  if (jawt_dll != NULL) {
    getFunction(jawt_dll, (void**)&_JAWT_GetAWT, "JAWT_GetAWT");
  }
*/

#include "../common/register.cpp"

JNI_GetCreatedJavaVMs_t get_JNI_GetCreatedJavaVMs() {
  HMODULE dll = GetModuleHandle("jvm.dll");
  if (dll == NULL) {
    printf("GetModuleHandle('jvm.dll') failed\n");
    return NULL;
  }
  return (JNI_GetCreatedJavaVMs_t)GetProcAddress(dll, "JNI_GetCreatedJavaVMs");
}

/** DLL Entry Point. */
BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved)
{
  return TRUE;
}
