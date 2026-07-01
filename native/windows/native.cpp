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

jlong peBegin(const char* cstr)
{
  HANDLE handle = BeginUpdateResource(cstr, FALSE);
  if (handle == NULL) {
    printf("peBegin failed:file=%s:error=%d\n", cstr, GetLastError());
  }
  return (jlong)handle;
}

void peAddIcon(jlong handle, jbyte* baptr, int offset, int length)
{
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

void peAddString(jlong handle, jint type, jint idx, jbyte* baptr, int offset, int length)
{
  UpdateResource((HANDLE)handle, (LPCSTR)(jlong)type, (LPCSTR)(jlong)idx, EN_US, baptr + offset, length);
}

void peEnd(jlong handle)
{
  EndUpdateResource((HANDLE)handle, FALSE);
}

extern "C" {
  JNIEXPORT jlong (*_peBegin)(const char*) = &peBegin;
  JNIEXPORT void (*_peAddIcon)(jlong, jbyte*, int, int) = &peAddIcon;
  JNIEXPORT void (*_peAddString)(jlong, int, int, jbyte*, int, int) = &peAddString;
  JNIEXPORT void (*_peEnd)(jlong) = &peEnd;
}

//Windows

jint* getWindowRect(const char* cstr)
{
  HWND hwnd = FindWindow(NULL, cstr);
  RECT winrect;
  jint* rectptr = ffm->newIntArray(4);
  if (hwnd != NULL) {
    if (GetWindowRect(hwnd, &winrect)) {
      rectptr[0] = winrect.left;
      rectptr[1] = winrect.top;
      rectptr[2] = winrect.right - winrect.left;
      rectptr[3] = winrect.bottom - winrect.top;
    }
  }
  return rectptr;
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

int strArrayLength(const char** sa) {
  int len = 0;
  int idx = 0;
  while (sa[idx] != NULL) {
    len++;
    idx++;
  }
  return len;
}

jlong executeSession(const char* cmd, const char** args)
{
  char msg[1024];
  //build args
  int nargs = strArrayLength(args);
  char **cargs = (char **)malloc((nargs+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nargs;a++) {
    const char *cstr = args[a];
    int sl = strlen(cstr);
    cargs[a] = (char*)malloc(sl+1);
    strcpy(cargs[a], cstr);
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

  jboolean res = CreateProcessAsUser(hNewToken, 0, (LPSTR)cmd, NULL, NULL, FALSE, dwCreationFlags, pEnvBlock, NULL, &si, &pi);

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

void simulateCtrlAltDel()
{
  (*_SendSAS)(false);
}

void setInputDesktop()
{
  char msg[1024];
  //change between default and winlogon desktops
  HDESK desk = OpenInputDesktop(0, false, 0x2000000);
  if (desk != NULL) {
    SetThreadDesktop(desk);
  }
}

jint getSessionID()
{
  return WTSGetActiveConsoleSessionId();
}

jboolean setSessionID(jlong token, jint sid)
{
  if (token == 0) return JNI_FALSE;
  HANDLE hToken = (HANDLE)token;
  if (!SetTokenInformation(hToken, TokenSessionId, &sid, sizeof(DWORD))) {
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

void closeSession(jlong token)
{
  if (token == 0) return;
  HANDLE hToken = (HANDLE)token;
  CloseHandle(hToken);
}

extern "C" {
  JNIEXPORT jint* (*_getWindowRect)(const char*) = &getWindowRect;
  JNIEXPORT jlong (*_executeSession)(const char*, const char**) = &executeSession;
  JNIEXPORT void (*_simulateCtrlAltDel)() = &simulateCtrlAltDel;
  JNIEXPORT void (*_setInputDesktop)() = &setInputDesktop;
  JNIEXPORT jint (*_getSessionID)() = &getSessionID;
  JNIEXPORT jboolean (*_setSessionID)(jlong, int) = &setSessionID;
  JNIEXPORT void (*_closeSession)(jlong) = &closeSession;
}

//impersonate user

jboolean impersonateUser(const char* domain, const char* user, const char* passwd)
{
  HANDLE token;
  int ok;

  ok = LogonUser(user, domain, passwd, LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_DEFAULT, &token);
  if (!ok) return JNI_FALSE;
  ok = ImpersonateLoggedOnUser(token);
  if (!ok) {
    CloseHandle(token);
  }
  return ok ? JNI_TRUE : JNI_FALSE;
}

jboolean revertToSelf()
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

jboolean createProcessAsUser(const char* domain, const char* user, const char* passwd, const char* app, const char* cmdline, jint flags)
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

  ok = LogonUserW((LPCWSTR)user, (LPCWSTR)domain, (LPCWSTR)passwd, LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_WINNT50, &hToken);
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
    ok = CreateProcessWithLogonW((LPCWSTR)user, (LPCWSTR)domain, (LPCWSTR)passwd, LOGON_WITH_PROFILE, (LPCWSTR)app, (LPWSTR)cmdline, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessWithLogonW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //0x522
    RevertToSelf();
    ok = CreateProcessWithTokenW(hToken, LOGON_WITH_PROFILE, (LPCWSTR)app, (LPWSTR)cmdline, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessWithTokenW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //0x522
    RevertToSelf();
    ok = CreateProcessAsUserW(hToken, (LPCWSTR)app, (LPWSTR)cmdline, NULL, NULL, false, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessAsUserW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //user profile not fully loaded
    ok = CreateProcessW((LPCWSTR)app, (LPWSTR)cmdline, NULL, NULL, false, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (ok == 0) {
      printf("CreateProcessW Failed:0x%x\n", GetLastError());
    }
  }

  if (flags & FLAG_LIMIT) {
    SaferCloseLevel(hSafer);
  }

  RevertToSelf();

  return ok ? JNI_TRUE : JNI_FALSE;
}

jboolean shellExecute(const char* op, const char* app, const char* args)
{
  bool ok = ShellExecuteW(NULL, (LPCWSTR)op, (LPCWSTR)app, (LPCWSTR)args, NULL, SW_NORMAL);

  return ok;
}

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

jint* getConsoleSize()
{
  CONSOLE_SCREEN_BUFFER_INFO info;
  jint* xy = ffm->newIntArray(2);
  GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
  xy[0] = info.srWindow.Right - info.srWindow.Left + 1;
  xy[1] = info.srWindow.Bottom - info.srWindow.Top + 1;
  return xy;
}

jint* getConsolePos()
{
  CONSOLE_SCREEN_BUFFER_INFO info;
  jint* xy = ffm->newIntArray(2);
  GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
  xy[0] = info.dwCursorPosition.X - info.srWindow.Left + 1;
  xy[1] = info.dwCursorPosition.Y - info.srWindow.Top + 1;
  return xy;
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

void enableConsoleMode()
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

void disableConsoleMode()
{
  SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), input_console_mode);
  SetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), output_console_mode);
}

jchar readConsole()
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

jboolean peekConsole()
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

void writeConsole(jint ch)
{
  printf("%c", ch);
}

void writeConsoleArray(jbyte* baptr, jint off, jint len)
{
  jbyte tmp[128];
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
}

extern "C" {
  JNIEXPORT jboolean (*_impersonateUser)(const char*, const char*, const char*) = &impersonateUser;
  JNIEXPORT jboolean (*_revertToSelf)() = &revertToSelf;
  JNIEXPORT jboolean (*_createProcessAsUser)(const char*, const char*, const char*, const char*, const char*, jint) = &createProcessAsUser;
  JNIEXPORT jboolean (*_shellExecute)(const char*, const char*, const char*) = &shellExecute;
  JNIEXPORT const char* (*_findJDKHome)() = &findJDKHome;
  JNIEXPORT void (*_enableConsoleMode)() = &enableConsoleMode;
  JNIEXPORT void (*_disableConsoleMode)() = &disableConsoleMode;
  JNIEXPORT jint* (*_getConsoleSize)() = &getConsoleSize;
  JNIEXPORT jint* (*_getConsolePos)() = &getConsolePos;
  JNIEXPORT jchar (*_readConsole)() = &readConsole;
  JNIEXPORT jboolean (*_peekConsole)() = &peekConsole;
  JNIEXPORT void (*_writeConsole)(jint) = &writeConsole;
  JNIEXPORT void (*_writeConsoleArray)(jbyte*, int, int) = &writeConsoleArray;
}

//tape drive API

static int tapeLastErrorValue;

jlong tapeOpen(const char* name)
{
  HANDLE handle = CreateFileA(name, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, FILE_FLAG_NO_BUFFERING, NULL);
  if (handle == INVALID_HANDLE_VALUE) {
    tapeLastErrorValue = GetLastError();
    return 0;
  }
  return (jlong)handle;
}

void tapeClose(jlong handle)
{
  CloseHandle((HANDLE)handle);
}

jboolean tapeFormat(jlong handle, jint blocksize)
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
    tapeLastErrorValue = GetLastError();
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
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

jint tapeRead(jlong handle, jbyte* baptr, jint offset, jint length)
{
  if (baptr == NULL) {
    tapeLastErrorValue = -1;
    return 0;
  }
  int read = 0;
  ReadFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&read, NULL);
  tapeLastErrorValue = GetLastError();
  return read;
}

jint tapeWrite(jlong handle, jbyte* baptr, jint offset, jint length)
{
  if (baptr == NULL) {
    tapeLastErrorValue = -1;
    return 0;
  }
  int write = 0;
  WriteFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&write, NULL);
  tapeLastErrorValue = GetLastError();
  return write;
}

jboolean tapeSetpos(jlong handle, jlong pos)
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
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

jlong tapeGetpos(jlong handle)
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
    tapeLastErrorValue = GetLastError();
    return -1;
  }
  return tapePos.Offset.QuadPart;
}

static jlong tape_media_size;
static jint tape_media_blocksize;
static jboolean tape_media_readonly;

jboolean tapeMedia(jlong handle)
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
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  tape_media_size = params.Capacity.QuadPart;
  tape_media_blocksize = params.BlockSize;
  tape_media_readonly = params.WriteProtected;
  return JNI_TRUE;
}

jlong tapeMediaSize()
{
  return tape_media_size;
}

jint tapeMediaBlockSize()
{
  return tape_media_blocksize;
}

jboolean tapeMediaReadOnly()
{
  return tape_media_readonly;
}

static jint tape_drive_def_blocksize;
static jint tape_drive_max_blocksize;
static jint tape_drive_min_blocksize;

jboolean tapeDrive(jlong handle)
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
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  tape_drive_def_blocksize = params.DefaultBlockSize;
  tape_drive_max_blocksize = params.MaximumBlockSize;
  tape_drive_min_blocksize = params.MinimumBlockSize;
  return JNI_TRUE;
}

jint tapeDriveMinBlockSize()
{
  return tape_drive_min_blocksize;
}

jint tapeDriveMaxBlockSize()
{
  return tape_drive_max_blocksize;
}

jint tapeDriveDefaultBlockSize()
{
  return tape_drive_def_blocksize;
}

jint tape_LastError()
{
  return tapeLastErrorValue;
}

extern "C" {
  JNIEXPORT jlong (*_tapeOpen)(const char*) = &tapeOpen;
  JNIEXPORT void (*_tapeClose)(jlong) = &tapeClose;
  JNIEXPORT jboolean (*_tapeFormat)(jlong, jint) = &tapeFormat;
  JNIEXPORT jint (*_tapeRead)(jlong,jbyte*,jint,jint) = &tapeRead;
  JNIEXPORT jint (*_tapeWrite)(jlong,jbyte*,jint,jint) = &tapeWrite;
  JNIEXPORT jboolean (*_tapeSetpos)(jlong,jlong) = &tapeSetpos;
  JNIEXPORT jlong (*_tapeGetpos)(jlong) = &tapeGetpos;
  JNIEXPORT jboolean (*_tapeMedia)(jlong) = &tapeMedia;
  JNIEXPORT jlong (*_tapeMediaSize)() = &tapeMediaSize;
  JNIEXPORT jint (*_tapeMediaBlockSize)() = &tapeMediaBlockSize;
  JNIEXPORT jboolean (*_tapeMediaReadOnly)() = &tapeMediaReadOnly;
  JNIEXPORT jboolean (*_tapeDrive)(jlong) = &tapeDrive;
  JNIEXPORT jint (*_tapeDriveMinBlockSize)() = &tapeDriveMinBlockSize;
  JNIEXPORT jint (*_tapeDriveMaxBlockSize)() = &tapeDriveMaxBlockSize;
  JNIEXPORT jint (*_tapeDriveDefaultBlockSize)() = &tapeDriveDefaultBlockSize;
  JNIEXPORT jint (*_tape_LastError)() = &tape_LastError;
}

//tape changer

jlong changerOpen(const char* name)
{
  HANDLE handle = CreateFileA(name, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
  if (handle == INVALID_HANDLE_VALUE) {
    tapeLastErrorValue = GetLastError();
    return 0;
  }
  return (jlong)handle;
}

void changerClose(jlong handle)
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

jstringArray changerList(jlong handle)
{
  HANDLE dev = (HANDLE)handle;

  list_count = 0;

  listType(dev, ELEMENT_TYPE::ChangerDrive, "drive");
  listType(dev, ELEMENT_TYPE::ChangerTransport, "transport");
  listType(dev, ELEMENT_TYPE::ChangerSlot, "slot");
  listType(dev, ELEMENT_TYPE::ChangerIEPort, "port");

  jstringArray ret = ffm->newStringArray(list_count);

  for(int i=0;i<list_count;i++) {
    ffm->setString(i,list_elements[i]);
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

jboolean changerMove(jlong handle, const char* src, const char* transport, const char* dst)
{
  HANDLE dev = (HANDLE)handle;

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
  if (ret != TRUE) {
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jlong (*_changerOpen)(const char*) = &changerOpen;
  JNIEXPORT void (*_changerClose)(jlong) = &changerClose;
  JNIEXPORT jstringArray (*_changerList)(jlong) = &changerList;
  JNIEXPORT jboolean (*_changerMove)(jlong,const char*,const char*,const char*) = &changerMove;
}

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
  lib_sas = loadLibrary("sas.dll");
  if (lib_sas == NULL) {
    printf("Warning:Unable to open sas.dll");
  } else {
    getFunction(lib_sas, (void**)&_SendSAS, "SendSAS");
  }

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
