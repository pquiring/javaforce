#include <windows.h>
#include <commctrl.h>

#include <stdio.h>

//downloads and unzips JRE to %APPDATA%\java

WNDCLASS wc;
HWND wnd, lbl, pb;
HINSTANCE inst;
char path[MAX_PATH];
char zip[MAX_PATH];
char exe[MAX_PATH];
char url[MAX_PATH];
char cmd[MAX_PATH];
char status[MAX_PATH];
MSG msg;
volatile int active;
int failed = FALSE;
STARTUPINFO sInfo;
PROCESS_INFORMATION pInfo;

LRESULT CALLBACK WindowProc(
  _In_ HWND   hwnd,
  _In_ UINT   uMsg,
  _In_ WPARAM wParam,
  _In_ LPARAM lParam)
{
  switch (uMsg) {
    case WM_CLOSE:
      active = FALSE;
      failed = TRUE;
      break;
  }
  return DefWindowProc(hwnd, uMsg, wParam, lParam);
}

static void SetStatus(const char *msg) {
  SetWindowText(lbl, msg);
  PostMessage(wnd, WM_PAINT, 0, 0);
}

static void SetProgress(int value) {
  PostMessage(pb, PBM_SETPOS, value, 0);
  PostMessage(wnd, WM_PAINT, 0, 0);
}

static void WakeThread() {
  PostMessage(wnd, WM_PAINT, 0, 0);
}

class Callback : public IBindStatusCallback
{
  public:
    STDMETHOD(OnStartBinding)(
      DWORD dwReserved,
      IBinding __RPC_FAR *pib)
    { return E_NOTIMPL; }

    STDMETHOD(GetPriority)(
      LONG __RPC_FAR *pnPriority)
    { return E_NOTIMPL; }

    STDMETHOD(OnLowResource)(
      DWORD reserved)
    { return E_NOTIMPL; }

    STDMETHOD(OnStopBinding)(
      HRESULT hresult,
      LPCWSTR szError)
    { return E_NOTIMPL; }

    STDMETHOD(GetBindInfo)(
      DWORD __RPC_FAR *grfBINDF,
      BINDINFO __RPC_FAR *pbindinfo)
    { return E_NOTIMPL; }

    STDMETHOD(OnDataAvailable)(
      DWORD grfBSCF,
      DWORD dwSize,
      FORMATETC __RPC_FAR *pformatetc,
      STGMEDIUM __RPC_FAR *pstgmed)
    { return E_NOTIMPL; }

    STDMETHOD(OnObjectAvailable)(
      REFIID riid,
      IUnknown __RPC_FAR *punk)
    { return E_NOTIMPL; }

    STDMETHOD_(ULONG,AddRef)() { return 0; }

    STDMETHOD_(ULONG,Release)() { return 0; }

    STDMETHOD(QueryInterface)(
      REFIID riid,
      void __RPC_FAR *__RPC_FAR *ppvObject)
    { return E_NOTIMPL; }

    STDMETHOD(OnProgress)(
      ULONG ulProgress,
      ULONG ulProgressMax,
      ULONG ulStatusCode,
      LPCWSTR szStatusText)
    {
      if (failed) return E_ABORT;
      float top = ulProgress;
      float bottom = ulProgressMax;
      float percent = top / bottom * 100.0f;
      SetProgress(percent);
//      sprintf(status, "%d %d %d %s", ulProgress, ulProgressMax, ulStatusCode, szStatusText);
//      printf("%s\n", status);
//      SetStatus(status);
      if (ulStatusCode == BINDSTATUS_ENDDOWNLOADDATA) {
        active = FALSE;
        WakeThread();
      }
      return 0;
    }
};

Callback callback;
int thread_id;
HANDLE thread_handle;

static void DoDownload(char *dest) {
  URLDownloadToFile(NULL, url, dest, 0, &callback);  //contrary to docs this does not return until done (sync)
  active = FALSE;
  WakeThread();
}

extern "C" {

#define SCALE 16

int DownloadJRE() {
  inst = GetModuleHandle(NULL);

  memset(&wc, 0, sizeof(WNDCLASS));
  wc.lpfnWndProc = &WindowProc;
  wc.hInstance = inst;
  wc.lpszClassName = "jfDownload";
  wc.hbrBackground = (HBRUSH)COLOR_WINDOW;
  RegisterClass(&wc);

  //calc full size
  int sx = GetSystemMetrics(SM_CXSCREEN);
  int sy = GetSystemMetrics(SM_CYSCREEN);
  int x = 200 + SCALE * 2;
  int y = SCALE*5;
  y += GetSystemMetrics(SM_CYCAPTION);
  x += GetSystemMetrics(SM_CXFIXEDFRAME);
  y += GetSystemMetrics(SM_CYFIXEDFRAME);

  int px = (sx - x) / 2;
  int py = (sy - y) / 2;

  wnd = CreateWindow("jfDownload", "Downloading...", WS_BORDER | WS_CAPTION | WS_SYSMENU | WS_VISIBLE,
    px, py, x, y, NULL, NULL, inst, NULL);

  lbl = CreateWindow("STATIC", "Downloading Java...", WS_CHILD | WS_VISIBLE,
    SCALE, SCALE, 200, SCALE, wnd, NULL, inst, NULL);
  //SetWindowText to update text

  pb = CreateWindow(PROGRESS_CLASS, "", WS_CHILD | WS_VISIBLE,
    SCALE, SCALE*3, 200, SCALE, wnd, NULL, inst, NULL);

  //get path
  GetEnvironmentVariable("APPDATA", path, MAX_PATH);
  strcat(path, "\\java");
  CreateDirectory(path, NULL);

  //get zip
  strcpy(zip, path);
  strcat(zip, "\\jre.7z");

  //get src
  sprintf(url, "http://javaforce.sourceforge.net/jre/win%d.7z", sizeof(void*) * 8);

  active = TRUE;
  thread_handle = CreateThread(NULL, 64 * 1024, (LPTHREAD_START_ROUTINE)&DoDownload, zip, 0, (LPDWORD)&thread_id);

  while (active) {
    GetMessage(&msg, NULL, 0, 0);
    TranslateMessage(&msg);
    DispatchMessage(&msg);
  }

  if (failed) {
    DestroyWindow(wnd);
    return 0;
  }

  //get exe
  strcpy(exe, path);
  strcat(exe, "\\7z.exe");

  //get src
  sprintf(url, "http://javaforce.sourceforge.net/jre/7z.exe");

  active = TRUE;
  thread_handle = CreateThread(NULL, 64 * 1024, (LPTHREAD_START_ROUTINE)&DoDownload, exe, 0, (LPDWORD)&thread_id);

  while (active) {
    GetMessage(&msg, NULL, 0, 0);
    TranslateMessage(&msg);
    DispatchMessage(&msg);
  }

  if (failed) {
    DestroyWindow(wnd);
    return 0;
  }

  //try to open zip file until it works
  do {
    HANDLE test = CreateFile(zip, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
    if (test != INVALID_HANDLE_VALUE) {
      CloseHandle(test);
      break;
    }
    Sleep(10);
    if (PeekMessage(&msg, NULL, 0, 0, TRUE)) {
      TranslateMessage(&msg);
      DispatchMessage(&msg);
    }
  } while (!failed);

  memset(&sInfo, 0, sizeof(STARTUPINFO));
  sInfo.cb = sizeof(STARTUPINFO);

  memset(&pInfo, 0, sizeof(PROCESS_INFORMATION));

  SetStatus("Extracting Java...");

  CreateProcess(exe, (LPSTR)"7z.exe x jre.7z", NULL, NULL, FALSE, NORMAL_PRIORITY_CLASS, NULL, path, &sInfo, &pInfo);

  WaitForSingleObject( pInfo.hProcess, INFINITE );

  CloseHandle(pInfo.hProcess);
  CloseHandle(pInfo.hThread);

  DestroyWindow(wnd);

  return 1;
}

} //extern "C"
