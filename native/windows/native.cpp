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

#include "javaforce_jni_DAQmxJNI.h"
#include "javaforce_jni_WinNative.h"
#include "javaforce_jni_GLJNI.h"
#include "javaforce_jni_CameraJNI.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"
#include "javaforce_media_VideoBuffer.h"
#include "javaforce_jni_UIJNI.h"
#include "javaforce_jni_PCapJNI.h"
#include "javaforce_jni_CLJNI.h"

#include "../common/library.h"
#include "../common/register.h"

static char* log_log = NULL;
static int log_size = 0;

static void log_append(const char* msg) {
  if (log_log == NULL) {
    log_size = 4096;
    log_log = (char*)GlobalAlloc(GMEM_FIXED, log_size);
    log_log[0] = 0;
  }
  int loglen = strlen(log_log);
  int msglen = strlen(msg);
  if (loglen + msglen + 1 > log_size) {
    log_size += 4096;
    log_log = (char*)GlobalReAlloc(log_log, log_size, GMEM_MOVEABLE);
  }
  strcat(log_log, msg);
}

static void log_reset() {
  if (log_log == NULL) return;
  log_log[0] = 0;
}

HMODULE wgl = NULL;

//OpenGL API

#include "../common/glfw.cpp"

#define GLFW_EXPOSE_NATIVE_WIN32
#define GLFW_EXPOSE_NATIVE_WGL

#include "../glfw/include/GLFW/glfw3native.h"

JNIEXPORT void JNICALL Java_javaforce_jni_UIJNI_seticon
  (JNIEnv *e, jobject c, jlong id, jstring filename, jint x, jint y)
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
#include "native-camera-vfw.cpp"  //lost in space and time
#endif

#if 0
//DirectShow (WinXP era) Camera API
#include "native-camera-directshow.cpp"
#endif

#if 1
//MediaFoundation (WinVista era) Camera API
#include "native-camera-mediafoundation.cpp"
#endif

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
  if (handle == NULL) {
    printf("peBegin failed:file=%s:error=%d\n", cstr, GetLastError());
  }
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
  dcb.fParity = 0;
  dcb.ByteSize = 8;  //8 data bits
  dcb.StopBits = ONESTOPBIT;  //1 stop bit
  dcb.Parity = 0;  //no parity
  SetCommState(handle, &dcb);
  COMMTIMEOUTS cto;
  memset(&cto, 0, sizeof(COMMTIMEOUTS));
  GetCommTimeouts(handle, &cto);
  cto.ReadIntervalTimeout = MAXDWORD;
  cto.ReadTotalTimeoutMultiplier = 0;
  cto.ReadTotalTimeoutConstant = 0;
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
  e->ReleaseIntArrayElements(rect, rectptr, 0);
  return ok;
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_WinNative_getLog
  (JNIEnv *e, jclass c)
{
  if (log_log == NULL) return NULL;
  jstring log = e->NewStringUTF(log_log);
  log_reset();
  return log;
}

BOOL CALLBACK EnumWindowStationProc(_In_ LPTSTR lpszWindowStation,_In_ LPARAM lParam) {
  char msg[1024];
  sprintf(msg, "enum.station=%s\n", lpszWindowStation);
  log_append(msg);
  return true;
}

BOOL CALLBACK EnumDesktopProc(_In_ LPTSTR lpszDesktop,_In_ LPARAM lParam) {
  char msg[1024];
  sprintf(msg, "enum.desktop=%s\n", lpszDesktop);
  log_append(msg);
  return true;
}

BOOL CALLBACK EnumWindowsProc(_In_ HWND hwnd,_In_ LPARAM lParam) {
  char msg[1024];
  sprintf(msg, "enum.window=%p\n", hwnd);
  log_append(msg);
  return true;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_executeSession
  (JNIEnv *e, jclass c, jstring cmd, jobjectArray args)
{
  char msg[1024];
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

  DWORD sid = WTSGetActiveConsoleSessionId();

  STARTUPINFO si;
  memset(&si, 0, sizeof(STARTUPINFO));
  si.cb = sizeof(STARTUPINFO);
  si.lpDesktop = "winsta0\\default";

  PROCESS_INFORMATION pi;

  HANDLE hToken = NULL;
  HANDLE hNewToken = NULL;
  void* pEnvBlock = NULL;

  DWORD dwCreationFlags = NORMAL_PRIORITY_CLASS;

  if (!OpenProcessToken(GetCurrentProcess(), TOKEN_ALL_ACCESS, &hToken)) {
    return JNI_FALSE;
  }

  if (!DuplicateTokenEx(hToken, MAXIMUM_ALLOWED, NULL, SecurityAnonymous, TokenPrimary, &hNewToken)){
    return JNI_FALSE;
  }

  if (!SetTokenInformation(hNewToken, TokenSessionId, &sid, sizeof(DWORD))) {
    return JNI_FALSE;
  }

  if (!CreateEnvironmentBlock(&pEnvBlock, hToken, TRUE)) {
    return JNI_FALSE;
  }

  dwCreationFlags |= CREATE_UNICODE_ENVIRONMENT;

  const char *ccmd = e->GetStringUTFChars(cmd, NULL);

  jboolean res = CreateProcessAsUser(hNewToken, 0, (LPSTR)ccmd, NULL, NULL, FALSE, dwCreationFlags, pEnvBlock, NULL, &si, &pi);

  e->ReleaseStringUTFChars(cmd, ccmd);

  if (hToken) CloseHandle(hToken);
  if (pEnvBlock) DestroyEnvironmentBlock(pEnvBlock);

  if (res == false) {
    if (hNewToken) CloseHandle(hNewToken);
    return 0;
  }

  return (jlong)hNewToken;
}

JF_LIB_HANDLE lib_sas;
void (*_SendSAS)(bool asUser);

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_simulateCtrlAltDel
  (JNIEnv *e, jclass c)
{
  (*_SendSAS)(false);
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_setInputDesktop
  (JNIEnv *e, jclass c)
{
  char msg[1024];
  //change between default and winlogon desktops
  HDESK desk = OpenInputDesktop(0, false, 0x2000000);
  if (desk != NULL) {
    SetThreadDesktop(desk);
  }
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_getSessionID
  (JNIEnv *e, jclass c)
{
  return WTSGetActiveConsoleSessionId();
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_setSessionID
  (JNIEnv *e, jclass c, jlong token, jint sid)
{
  if (token == 0) return JNI_FALSE;
  HANDLE hToken = (HANDLE)token;
  if (!SetTokenInformation(hToken, TokenSessionId, &sid, sizeof(DWORD))) {
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_closeSession
  (JNIEnv *e, jclass c, jlong token)
{
  if (token == 0) return;
  HANDLE hToken = (HANDLE)token;
  CloseHandle(hToken);
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_revertToSelf
  (JNIEnv *e, jclass c)
{
  return RevertToSelf();
}

static bool EnablePrivilege(LPCSTR privName)
{
  HANDLE hToken;
  TOKEN_PRIVILEGES tp;
  LUID luid;

  if (!OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken))
  {
    printf("OpenProcessToken failed. Error: 0x%x\n", GetLastError());
    return false;
  }

  if (!LookupPrivilegeValueA(NULL, privName, &luid))
  {
    printf("LookupPrivilegeValue failed. Error: 0x%x\n", GetLastError());
    CloseHandle(hToken);
    return false;
  }

  tp.PrivilegeCount = 1;
  tp.Privileges[0].Luid = luid;
  tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

  if (!AdjustTokenPrivileges(hToken, FALSE, &tp, sizeof(TOKEN_PRIVILEGES), NULL, NULL))
  {
    printf("AdjustTokenPrivileges failed. Error: 0x%x\n",GetLastError());
    CloseHandle(hToken);
    return false;
  }

  if (GetLastError() == ERROR_NOT_ALL_ASSIGNED)
  {
    printf("Privilege %s not assigned to process\n", privName);
    CloseHandle(hToken);
    return false;
  }

  CloseHandle(hToken);
  return true;
}

#define FLAG_LIMIT 1
#define FLAG_ELEVATE 2

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_createProcessAsUser
  (JNIEnv *e, jclass c, jstring domain, jstring user, jstring passwd, jstring app, jstring cmdline, jint flags)
{
  int ok;

  //Error 0x57 (87) : ERROR_INVALID_PARAMETER
  //Error 0x522 (1314) : ERROR_PRIVILEGE_NOT_HELD
  //Error 0x52E (1326) : ERROR_LOGON_FAILURE
  //Error 0x542 (1346) : ERROR_BAD_IMPERSONATION_LEVEL
  //Error 0x569 (1385) : ERROR_LOGON_TYPE_NOT_GRANTED

  PROCESS_INFORMATION pi;
  STARTUPINFOW si;
  memset(&si, 0, sizeof(STARTUPINFOW));
  si.cb = sizeof(STARTUPINFOW);
  //si.lpDesktop = L"winsta0\\default";

  HANDLE hToken;
  HANDLE hNewToken;
  HANDLE hRestricted;
  SAFER_LEVEL_HANDLE hSafer;
  LPVOID pEnv;
  DWORD dup = TOKEN_ASSIGN_PRIMARY | TOKEN_DUPLICATE | TOKEN_QUERY | TOKEN_ADJUST_DEFAULT | TOKEN_ADJUST_SESSIONID;

  TOKEN_ELEVATION_TYPE tet;
  TOKEN_LINKED_TOKEN tlt;
  DWORD needed = 0;

  if (false) {
    EnablePrivilege(SE_ASSIGNPRIMARYTOKEN_NAME);
    EnablePrivilege(SE_INCREASE_QUOTA_NAME);
    EnablePrivilege(SE_IMPERSONATE_NAME);
  }

  const jchar *cdomain = e->GetStringChars(domain,NULL);
  const jchar *cuser = e->GetStringChars(user,NULL);
  const jchar *cpasswd = e->GetStringChars(passwd,NULL);
  const jchar *capp = e->GetStringChars(app,NULL);
  const jchar *ccmdline = e->GetStringChars(cmdline,NULL);

  ok = LogonUserW((LPCWSTR)cuser, (LPCWSTR)cdomain, (LPCWSTR)cpasswd, LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_WINNT50, &hToken);
  if (!ok) {
    printf("LogonUserW Failed:0x%x\n", GetLastError());
  }

  ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
  if (!ok) {
    printf("DuplicateTokenEx(1) Failed:0x%x\n", GetLastError());
  } else {
    hToken = hNewToken;
  }

  if (false) {
    int level = SECURITY_MANDATORY_LOW_RID;
    ok = SetTokenInformation(hNewToken, TokenIntegrityLevel, &level, sizeof(DWORD));
    if (ok != 0) {
      printf("SetTokenInformation Failed:0x%x\n", GetLastError());
    }

    if (!AdjustTokenPrivileges(hNewToken, TRUE, NULL, 0, NULL, NULL))
    {
      printf("AdjustTokenPrivileges Failed:0x%x\n",GetLastError());
    }
  }

  if (flags & FLAG_LIMIT) {
    printf("Limiting access...\n");
    if (!SaferCreateLevel(SAFER_SCOPEID_USER, SAFER_LEVELID_NORMALUSER, SAFER_LEVEL_OPEN, &hSafer, NULL)) {
      printf("SaferCreateLevel Failed:0x%x\n",GetLastError());
    }

    if (!SaferComputeTokenFromLevel(hSafer, hToken, &hRestricted, 0, NULL)) {
      printf("SaferComputeTokenFromLevel Failed:0x%x\n",GetLastError());
    } else {
      hToken = hRestricted;
    }

    ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
    if (!ok) {
      printf("DuplicateTokenEx(2) Failed:0x%x\n", GetLastError());
    } else {
      hToken = hNewToken;
    }
  }


  if (flags & FLAG_ELEVATE) {
    printf("Elevating access...\n");
    if (!GetTokenInformation(hToken, TokenElevationType, (LPVOID)&tet, sizeof(tet), &needed)) {
      printf("GetTokenInformation(TokenElevationType) Failed:0x%x\n", GetLastError());
    }

    if (!GetTokenInformation(hToken, TokenLinkedToken, (LPVOID)&tlt, sizeof(tlt), &needed)) {
      printf("GetTokenInformation(TokenLinkedToken) Failed:0x%x\n", GetLastError());
    } else {
      hToken = tlt.LinkedToken;
    }

    if (false) {
      ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
      if (!ok) {
        printf("DuplicateTokenEx(3) Failed:0x%x\n", GetLastError());
      } else {
        hToken = hNewToken;
      }
    }
  }

  if (true) {
    ok = ImpersonateLoggedOnUser(hToken);
    if (!ok) {
      printf("ImpersonateLoggedOnUser Failed:0x%x\n", GetLastError());
    }
  }

  if (true) {
    ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
    if (!ok) {
      printf("DuplicateTokenEx(1) Failed:0x%x\n", GetLastError());
    } else {
      hToken = hNewToken;
    }
  }

  ok = CreateEnvironmentBlock(&pEnv, hToken, true);
  if (!ok) {
    printf("CreateEnvironmentBlock Failed:0x%x\n", GetLastError());
  }

  if (false) {
    ok = CreateRestrictedToken(hToken, LUA_TOKEN, 0, NULL, 0, NULL, 0, NULL, &hRestricted);
    if (!ok) {
      printf("CreateRestrictedToken Failed:0x%x\n", GetLastError());
    } else {
      hToken = hRestricted;
    }
  }

  if (true) {
    RevertToSelf();
    ok = CreateProcessWithLogonW((LPCWSTR)cuser, (LPCWSTR)cdomain, (LPCWSTR)cpasswd, LOGON_WITH_PROFILE, (LPCWSTR)capp, (LPWSTR)ccmdline, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessWithLogonW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //0x522
    RevertToSelf();
    ok = CreateProcessWithTokenW(hToken, LOGON_WITH_PROFILE, (LPCWSTR)capp, (LPWSTR)ccmdline, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessWithTokenW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //0x522
    RevertToSelf();
    ok = CreateProcessAsUserW(hToken, (LPCWSTR)capp, (LPWSTR)ccmdline, NULL, NULL, false, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessAsUserW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //user profile not fully loaded
    ok = CreateProcessW((LPCWSTR)capp, (LPWSTR)ccmdline, NULL, NULL, false, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (ok == 0) {
      printf("CreateProcessW Failed:0x%x\n", GetLastError());
    }
  }

  if (flags & FLAG_LIMIT) {
    SaferCloseLevel(hSafer);
  }

  RevertToSelf();

  e->ReleaseStringChars(app, capp);
  e->ReleaseStringChars(cmdline, ccmdline);
  e->ReleaseStringChars(domain, cdomain);
  e->ReleaseStringChars(user, cuser);
  e->ReleaseStringChars(passwd, cpasswd);

  return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_shellExecute
  (JNIEnv *e, jclass c, jstring op, jstring app, jstring args)
{
  const jchar *cop = e->GetStringChars(op, NULL);
  const jchar *capp = e->GetStringChars(app, NULL);
  const jchar *cargs = e->GetStringChars(args, NULL);

  bool ok = ShellExecuteW(NULL, (LPCWSTR)cop, (LPCWSTR)capp, (LPCWSTR)cargs, NULL, SW_NORMAL);

  e->ReleaseStringChars(op, cop);
  e->ReleaseStringChars(app, capp);
  e->ReleaseStringChars(args, cargs);

  return ok;
}

//find JDK Home

JNIEXPORT jstring JNICALL Java_javaforce_jni_WinNative_findJDKHome(JNIEnv *e, jclass c) {
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

/**
  UTF8 Format:
    1st bytes:
      0xxxxxxx = 7bit (1 byte)
      110xxxxx = 5bit (2 byte) 11bit total (import supported) (not exported)
      1110xxxx = 4bit (3 byte) 16bit total (import/export supported)
      11110xxx = 3bit (4 byte) 21bit total (import truncated to 16bit) (not exported)
    2nd,3rd,4th bytes only:
      10xxxxxx = 6bit
*/

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_writeConsole
  (JNIEnv *e, jclass c, jint ch)
{
  printf("%c", ch);
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_writeConsoleArray
  (JNIEnv *e, jclass c, jbyteArray ba, jint off, jint len)
{
  jbyte tmp[128];
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int length = len;
  int pos = off;
  while (length > 0) {
    if (length > 127) {
      std::memcpy(tmp, baptr+pos, 127);
      tmp[127] = 0;
      length -= 127;
      pos += 127;
    } else {
      std::memcpy(tmp, baptr+pos, length);
      tmp[length] = 0;
      length = 0;
    }
    printf("%s", tmp);
  }
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_tapeFormat
  (JNIEnv *e, jclass c, jlong handle, jint blocksize)
{
  HANDLE dev = (HANDLE)handle;
  DWORD bytesReturn;
  BOOL ret;

/*  //not supported
  TAPE_PREPARE tapePrepare;
  tapePrepare.Operation = TAPE_FORMAT;
  tapePrepare.Immediate = FALSE;
  ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_PREPARE,
    &tapePrepare,
    sizeof(tapePrepare),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("TAPE_PREPARE Failed\r\n");
    tapeLastError = GetLastError();
    //return JNI_FALSE;  //ignore error - not supported on all drives
  }
*/

  TAPE_SET_MEDIA_PARAMETERS tapeSetMediaParams;
  tapeSetMediaParams.BlockSize = blocksize;
  ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_SET_MEDIA_PARAMS,
    &tapeSetMediaParams,
    sizeof(tapeSetMediaParams),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("TAPE_SET_MEDIA_PARAMETERS Failed\r\n");
    tapeLastError = GetLastError();
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeRead
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba, jint offset, jint length)
{
  jboolean isCopy;
  jbyte *baptr = (jbyte*)e->GetPrimitiveArrayCritical(ba, &isCopy);
  if (baptr == NULL) {
    tapeLastError = -1;
    return 0;
  }
  int read = 0;
  ReadFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&read, NULL);
  tapeLastError = GetLastError();
  e->ReleasePrimitiveArrayCritical(ba, (jbyte*)baptr, JNI_ABORT);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeWrite
  (JNIEnv *e, jclass c, jlong handle, jbyteArray ba, jint offset, jint length)
{
  jboolean isCopy;
  jbyte *baptr = (jbyte*)e->GetPrimitiveArrayCritical(ba, &isCopy);
  if (baptr == NULL) {
    tapeLastError = -1;
    return 0;
  }
  int write = 0;
  WriteFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&write, NULL);
  tapeLastError = GetLastError();
  e->ReleasePrimitiveArrayCritical(ba, (jbyte*)baptr, JNI_ABORT);
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
static jint tape_media_blocksize;
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
  tape_media_blocksize = params.BlockSize;
  tape_media_readonly = params.WriteProtected;
  return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_tapeMediaSize(JNIEnv *e, jclass c)
{
  return tape_media_size;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeMediaBlockSize(JNIEnv *e, jclass c)
{
  return tape_media_blocksize;
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
    BOOL noLabel = (status.ExceptionCode & ERROR_LABEL_UNREADABLE) != 0;
    const char* label = NULL;
    if (hasTape) {
      //trim barcode
      char *barcode = (char*)status.PrimaryVolumeID;
      for(int a=0;a<MAX_VOLUME_ID_SIZE;a++) {
        if (barcode[a] == ' ') barcode[a] = 0;
      }
      if (barcode[0] != 0) {
        label = barcode;
      } else {
        label = "<null>";
      }
    } else {
      label = "<empty>";
    }
    if (noLabel) {
      label = "<no label>";
    }
    list_elements[list_count] = (char*)malloc(128);
    std::sprintf(list_elements[list_count], "%s%d:%s", name, idx+1, label);
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

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_tapeLastError
  (JNIEnv *e, jclass c)
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

  e->ReleasePrimitiveArrayCritical(a, aptr, 0);
}


#include "../common/ffmpeg.cpp"

#include "../common/videobuffer.cpp"

#include "../common/opencl.cpp"

#include "../common/ni.cpp"

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

#include "../common/pcap.cpp"

#include "../speexdsp/speex_dsp.c"

#include "vss.cpp"

static JNINativeMethod javaforce_media_Camera[] = {
  {"cameraInit", "()J", (void *)&Java_javaforce_jni_CameraJNI_cameraInit},
  {"cameraUninit", "(J)Z", (void *)&Java_javaforce_jni_CameraJNI_cameraUninit},
  {"cameraListDevices", "(J)[Ljava/lang/String;", (void *)&Java_javaforce_jni_CameraJNI_cameraListDevices},
  {"cameraListModes", "(JI)[Ljava/lang/String;", (void *)&Java_javaforce_jni_CameraJNI_cameraListModes},
  {"cameraStart", "(JIII)Z", (void *)&Java_javaforce_jni_CameraJNI_cameraStart},
  {"cameraStop", "(J)Z", (void *)&Java_javaforce_jni_CameraJNI_cameraStop},
  {"cameraGetFrame", "(J)[I", (void *)&Java_javaforce_jni_CameraJNI_cameraGetFrame},
  {"cameraGetWidth", "(J)I", (void *)&Java_javaforce_jni_CameraJNI_cameraGetWidth},
  {"cameraGetHeight", "(J)I", (void *)&Java_javaforce_jni_CameraJNI_cameraGetHeight},
};

extern "C" void camera_register(JNIEnv *env);

void camera_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/CameraJNI");
  registerNatives(env, cls, javaforce_media_Camera, sizeof(javaforce_media_Camera)/sizeof(JNINativeMethod));
}

//Windows native methods
static JNINativeMethod javaforce_jni_WinNative[] = {
  {"comOpen", "(Ljava/lang/String;I)J", (void *)&Java_javaforce_jni_WinNative_comOpen},
  {"comClose", "(J)V", (void *)&Java_javaforce_jni_WinNative_comClose},
  {"comRead", "(J[B)I", (void *)&Java_javaforce_jni_WinNative_comRead},
  {"comWrite", "(J[B)I", (void *)&Java_javaforce_jni_WinNative_comWrite},

  {"getWindowRect", "(Ljava/lang/String;[I)Z", (void *)&Java_javaforce_jni_WinNative_getWindowRect},
  {"getLog", "()Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_getLog},
  {"executeSession", "(Ljava/lang/String;[Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_executeSession},
  {"simulateCtrlAltDel", "()V", (void *)&Java_javaforce_jni_WinNative_simulateCtrlAltDel},
  {"setInputDesktop", "()V", (void *)&Java_javaforce_jni_WinNative_setInputDesktop},
  {"getSessionID", "()I", (void *)&Java_javaforce_jni_WinNative_getSessionID},
  {"setSessionID", "(JI)Z", (void *)&Java_javaforce_jni_WinNative_setSessionID},
  {"closeSession", "(J)V", (void *)&Java_javaforce_jni_WinNative_closeSession},

  {"peBegin", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_peBegin},
  {"peAddIcon", "(J[B)V", (void *)&Java_javaforce_jni_WinNative_peAddIcon},
  {"peAddString", "(JII[B)V", (void *)&Java_javaforce_jni_WinNative_peAddString},
  {"peEnd", "(J)V", (void *)&Java_javaforce_jni_WinNative_peEnd},
  {"impersonateUser", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_impersonateUser},
  {"revertToSelf", "()Z", (void *)&Java_javaforce_jni_WinNative_revertToSelf},
  {"createProcessAsUser", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Z", (void *)&Java_javaforce_jni_WinNative_createProcessAsUser},
  {"shellExecute", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_shellExecute},
  {"findJDKHome", "()Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_findJDKHome},
  {"enableConsoleMode", "()V", (void *)&Java_javaforce_jni_WinNative_enableConsoleMode},
  {"disableConsoleMode", "()V", (void *)&Java_javaforce_jni_WinNative_disableConsoleMode},
  {"getConsoleSize", "()[I", (void *)&Java_javaforce_jni_WinNative_getConsoleSize},
  {"getConsolePos", "()[I", (void *)&Java_javaforce_jni_WinNative_getConsolePos},
  {"readConsole", "()C", (void *)&Java_javaforce_jni_WinNative_readConsole},
  {"peekConsole", "()Z", (void *)&Java_javaforce_jni_WinNative_peekConsole},
  {"writeConsole", "(I)V", (void *)&Java_javaforce_jni_WinNative_writeConsole},
  {"writeConsoleArray", "([BII)V", (void *)&Java_javaforce_jni_WinNative_writeConsoleArray},
  {"tapeOpen", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_tapeOpen},
  {"tapeClose", "(J)V", (void *)&Java_javaforce_jni_WinNative_tapeClose},
  {"tapeFormat", "(JI)Z", (void *)&Java_javaforce_jni_WinNative_tapeFormat},
  {"tapeRead", "(J[BII)I", (void *)&Java_javaforce_jni_WinNative_tapeRead},
  {"tapeWrite", "(J[BII)I", (void *)&Java_javaforce_jni_WinNative_tapeWrite},
  {"tapeSetpos", "(JJ)Z", (void *)&Java_javaforce_jni_WinNative_tapeSetpos},
  {"tapeGetpos", "(J)J", (void *)&Java_javaforce_jni_WinNative_tapeGetpos},
  {"tapeMedia", "(J)Z", (void *)&Java_javaforce_jni_WinNative_tapeMedia},
  {"tapeMediaSize", "()J", (void *)&Java_javaforce_jni_WinNative_tapeMediaSize},
  {"tapeMediaBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeMediaBlockSize},
  {"tapeMediaReadOnly", "()Z", (void *)&Java_javaforce_jni_WinNative_tapeMediaReadOnly},
  {"tapeDrive", "(J)Z", (void *)&Java_javaforce_jni_WinNative_tapeDrive},
  {"tapeDriveMinBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeDriveMinBlockSize},
  {"tapeDriveMaxBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeDriveMaxBlockSize},
  {"tapeDriveDefaultBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeDriveDefaultBlockSize},
  {"tapeLastError", "()I", (void *)&Java_javaforce_jni_WinNative_tapeLastError},
  {"changerOpen", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_changerOpen},
  {"changerClose", "(J)V", (void *)&Java_javaforce_jni_WinNative_changerClose},
  {"changerList", "(J)[Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_changerList},
  {"changerMove", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_changerMove},
//vss
  {"vssInit", "()Z", (void *)&Java_javaforce_jni_WinNative_vssInit},
  {"vssListVols", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_vssListVols},
  {"vssListShadows", "()[[Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_vssListShadows},
  {"vssCreateShadow", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssCreateShadow},
  {"vssDeleteShadow", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssDeleteShadow},
  {"vssDeleteShadowAll", "()Z", (void *)&Java_javaforce_jni_WinNative_vssDeleteShadowAll},
  {"vssMountShadow", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssMountShadow},
  {"vssUnmountShadow", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssUnmountShadow},
//test
  {"add", "(II)I", (void *)&Java_javaforce_jni_WinNative_add},
  {"hold", "([II)V", (void *)&Java_javaforce_jni_WinNative_hold},
};

extern "C" void winnative_register(JNIEnv *env);

void winnative_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/WinNative");
  registerNatives(env, cls, javaforce_jni_WinNative, sizeof(javaforce_jni_WinNative)/sizeof(JNINativeMethod));

  lib_sas = loadLibrary("sas.dll");
  if (lib_sas == NULL) {
    printf("Warning:Unable to open sas.dll");
  } else {
    getFunction(lib_sas, (void**)&_SendSAS, "SendSAS");
  }

  speex_dsp_register(env);
}

#include "../common/register.cpp"

/** DLL Entry Point. */
BOOL APIENTRY DllMain( HANDLE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved)
{
  return TRUE;
}
