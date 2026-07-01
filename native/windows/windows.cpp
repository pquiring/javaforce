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
  return true;
}

BOOL CALLBACK EnumDesktopProc(_In_ LPTSTR lpszDesktop,_In_ LPARAM lParam) {
  char msg[1024];
  sprintf(msg, "enum.desktop=%s\n", lpszDesktop);
  return true;
}

BOOL CALLBACK EnumWindowsProc(_In_ HWND hwnd,_In_ LPARAM lParam) {
  char msg[1024];
  sprintf(msg, "enum.window=%p\n", hwnd);
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
  if (lib_sas == NULL) {
    lib_sas = loadLibrary("sas.dll");
    if (lib_sas == NULL) {
      printf("Warning:Unable to open sas.dll");
      return;
    } else {
      getFunction(lib_sas, (void**)&_SendSAS, "SendSAS");
    }
  }

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
