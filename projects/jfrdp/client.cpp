#include <windows.h>
#include "rdpmingw.h"

#include "client_WDS.h"

//this one does not define it as "extern"
#define DEFINE_GUID_(name,l,w1,w2,b1,b2,b3,b4,b5,b6,b7,b8) const GUID name = { l, w1, w2, { b1, b2, b3, b4, b5, b6, b7, b8 } }

DEFINE_GUID_(CLSID_RDPViewer,0x32be5ed2,0x5c86,0x480f,0xa9,0x14,0x0f,0xf8,0x88,0x5a,0x1b,0x3f);
DEFINE_GUID_(IID_IRDPSRAPIViewer,0xc6bfcd38,0x8ce9,0x404d,0x8a,0xe8,0xf3,0x1d,0x00,0xc6,0x5c,0xb5);
DEFINE_GUID_(CLSID_RDPSession,0x9B78F0E6,0x3E05,0x4A5B,0xB2,0xE8,0xE7,0x43,0xA8,0x95,0x6B,0x65);
DEFINE_GUID_(DIID__IRDPSessionEvents,0x98a97042,0x6698,0x40e9,0x8e,0xfd,0xb3,0x20,0x09,0x90,0x00,0x4b);
DEFINE_GUID_(IID_IRDPSRAPISharingSession,0xeeb20886,0xe470,0x4cf6,0x84,0x2b,0x27,0x39,0xc0,0xec,0x5c,0xfb);
DEFINE_GUID_(IID_IRDPSRAPIAttendee,0xec0671b3,0x1b78,0x4b80,0xa4,0x64,0x91,0x32,0x24,0x75,0x43,0xe3);
DEFINE_GUID_(IID_IRDPSRAPIAttendeeManager,0xba3a37e8,0x33da,0x4749,0x8d,0xa0,0x07,0xfa,0x34,0xda,0x79,0x44);
DEFINE_GUID_(IID_IRDPSRAPISessionProperties,0x339b24f2,0x9bc0,0x4f16,0x9a,0xac,0xf1,0x65,0x43,0x3d,0x13,0xd4);
DEFINE_GUID_(CLSID_RDPSRAPIApplicationFilter,0xe35ace89,0xc7e8,0x427e,0xa4,0xf9,0xb9,0xda,0x07,0x28,0x26,0xbd);
DEFINE_GUID_(CLSID_RDPSRAPIInvitationManager,0x53d9c9db,0x75ab,0x4271,0x94,0x8a,0x4c,0x4e,0xb3,0x6a,0x8f,0x2b);

class MyClass;  //forward decl

struct RDP {
  WNDCLASS wc;
  HWND wnd;
  HINSTANCE inst;
  volatile int active;
  IUnknown *rdpUnknown;
  IRDPSRAPIViewer *rdpViewer;
  IOleObject *rdpOO;
  IOleInPlaceObject *rdpOIPO;
  RECT rect;
  MyClass *cls;
} *rdp;

void SetSize() {
  if (rdp == NULL) return;
  if (rdp->rdpOIPO == NULL) return;
  RECT rect;
  GetClientRect(rdp->wnd, &rect);
  rdp->rdpOIPO->SetObjectRects(&rect, &rect);
}

LRESULT CALLBACK WindowProc(
  _In_ HWND   hwnd,
  _In_ UINT   uMsg,
  _In_ WPARAM wParam,
  _In_ LPARAM lParam)
{
  switch (uMsg) {
    case WM_CLOSE:
      rdp->active = FALSE;
      break;
    case WM_SIZE:
      SetSize();
      break;
  }
  return DefWindowProc(hwnd, uMsg, wParam, lParam);
}

class MyClass : public IDispatch, public IOleClientSite, public IOleInPlaceSite, public IOleContainer, public IOleInPlaceFrame {
  public:
  //IUnknown
    STDMETHOD(QueryInterface)(
      REFIID riid,
      void __RPC_FAR *__RPC_FAR *ppvObject)
    {
      if (ppvObject == NULL) {
        return E_POINTER;
      }

      if (riid == IID_IUnknown) {
        *ppvObject = (IDispatch*)this;  //IUnknown is ambiguous
        return S_OK;
      }

      if (riid == IID_IDispatch) {
        *ppvObject = (IDispatch*)this;
        return S_OK;
      }

      if (riid == IID_IOleClientSite)
      {
        *ppvObject = (IOleClientSite*)this;
        return S_OK;
      }

      if (riid == IID_IOleInPlaceSite) {
        *ppvObject = (IOleInPlaceSite*)this;
        return S_OK;
      }

      OLECHAR *str;
      StringFromCLSID(riid, &str);
      wprintf(L"Unknown Interface Queried : %s\n", str);
      CoTaskMemFree(str);

      return E_NOINTERFACE;
    }
    STDMETHOD_(ULONG,AddRef)() { return 0; }
    STDMETHOD_(ULONG,Release)() { return 0; }
  //IDispatch
    STDMETHOD(GetTypeInfo)(
      UINT      iTInfo,
      LCID      lcid,
      ITypeInfo **ppTInfo)
    { return E_NOTIMPL; }

    STDMETHOD(GetTypeInfoCount)(
      UINT *pctinfo)
    { return E_NOTIMPL; }

    STDMETHOD(GetIDsOfNames)(
      REFIID   riid,
      LPOLESTR *rgszNames,
      UINT     cNames,
      LCID     lcid,
      DISPID   *rgDispId)
    { return E_NOTIMPL; }

    STDMETHOD(Invoke)(
      DISPID     dispIdMember,
      REFIID     riid,
      LCID       lcid,
      WORD       wFlags,
      DISPPARAMS *pDispParams,
      VARIANT    *pVarResult,
      EXCEPINFO  *pExcepInfo,
      UINT       *puArgErr)
    {
      return S_OK;
    }
  //IOleClientSite
    STDMETHOD(SaveObject)() {return S_OK;}
    STDMETHOD(GetMoniker)(DWORD dwAssign,DWORD dwWhichMoniker,IMoniker **ppmk) {return S_OK;}
    STDMETHOD(GetContainer)(IOleContainer **ppContainer) {
      printf("GetContainer\n");
      *ppContainer = rdp->cls;
      return S_OK;
    }
    STDMETHOD(ShowObject)() {return S_OK;}
    STDMETHOD(OnShowWindow)(BOOL fShow) {return S_OK;}
    STDMETHOD(RequestNewObjectLayout)() {return S_OK;}
  //IOleWindow
    STDMETHOD(GetWindow)(HWND *phwnd) {
      printf("GetWindow\n");
      *phwnd = rdp->wnd;
      return S_OK;
    }
    STDMETHOD(ContextSensitiveHelp)(BOOL fEnterMode) {return S_OK;}
  //IOleInPlaceSite
    STDMETHOD(CanInPlaceActivate)() {return S_OK;}
    STDMETHOD(OnInPlaceActivate)() {return S_OK;}
    STDMETHOD(OnUIActivate)() {return S_OK;}
    STDMETHOD(GetWindowContext)(
      IOleInPlaceFrame      **ppFrame,
      IOleInPlaceUIWindow   **ppDoc,
      LPRECT                lprcPosRect,
      LPRECT                lprcClipRect,
      LPOLEINPLACEFRAMEINFO lpFrameInfo)
    {
      printf("GetWindowContext\n");
      *ppFrame = rdp->cls;
      *ppDoc = rdp->cls;

/*
      lprcPosRect->left = rdp->rect->left;
      lprcPosRect->top = rdp->rect->top;
      lprcPosRect->right = rdp->rect->right;
      lprcPosRect->bottom = rdp->rect->bottom;

      lprcClipRect->left = rdp->rect->left;
      lprcClipRect->top = rdp->rect->top;
      lprcClipRect->right = rdp->rect->right;
      lprcClipRect->bottom = rdp->rect->bottom;
*/

      lpFrameInfo->fMDIApp = FALSE;
      lpFrameInfo->hwndFrame = rdp->wnd;
      lpFrameInfo->haccel = NULL;
      lpFrameInfo->cAccelEntries = 0;

      return S_OK;
    }
    STDMETHOD(Scroll)(SIZE scrollExtant) {return S_OK;}
    STDMETHOD(OnUIDeactivate)(BOOL fUndoable) {return S_OK;}
    STDMETHOD(OnInPlaceDeactivate)() {return S_OK;}
    STDMETHOD(DiscardUndoState)() {return S_OK;}
    STDMETHOD(OnPosRectChange)(LPCRECT lprcPosRect) {return S_OK;}
    STDMETHOD(DeactivateAndUndo)() {return S_OK;}
  //IParseDisplayName
    STDMETHOD(ParseDisplayName)(
      IBindCtx *pbc,
      LPOLESTR pszDisplayName,
      ULONG    *pchEaten,
      IMoniker **ppmkOut)
      {return S_OK;}
  //IOleContainer
    STDMETHOD(EnumObjects)(DWORD grfFlags, IEnumUnknown **ppenum) {return S_OK;}
    STDMETHOD(LockContainer)(BOOL fLock) {return S_OK;}
  //IOleInPlaceUIWindow
    STDMETHOD(GetBorder)(LPRECT lprectBorder) {return S_OK;}
    STDMETHOD(RequestBorderSpace)(LPCBORDERWIDTHS pborderwidths) {return S_OK;}
    STDMETHOD(SetBorderSpace)(LPCBORDERWIDTHS pborderwidths) {return S_OK;}
    STDMETHOD(SetActiveObject)(IOleInPlaceActiveObject *pActiveObject,LPCOLESTR pszObjName) {return S_OK;}
  //IOleInPlaceFrame
    STDMETHOD(InsertMenus)(HMENU hmenuShared, LPOLEMENUGROUPWIDTHS lpMenuWidths) {return S_OK;}
    STDMETHOD(SetMenu)(HMENU hmenuShared, HOLEMENU holemenu, HWND hwndActiveObject) {return S_OK;}
    STDMETHOD(RemoveMenus)(HMENU hmenuShared) {return S_OK;}
    STDMETHOD(SetStatusText)(LPCOLESTR pszStatusText) {return S_OK;}
    STDMETHOD(EnableModeless)(BOOL fEnable) {return S_OK;}
    STDMETHOD(TranslateAccelerator)(LPMSG lpmsg,WORD  wID) {return S_OK;}
};

BSTR bstr_alloc(const char *s8) {
  int sl = strlen(s8) + 1;
  OLECHAR *s16 = (OLECHAR*)malloc(sl * 2);
  for(int a=0;a<sl;a++) {
    s16[a] = s8[a];
  }
  BSTR ret = SysAllocString(s16);
  free(s16);
  return ret;
}

void bstr_free(BSTR bstr) {
  SysFreeString(bstr);
}

JNIEXPORT jboolean JNICALL Java_client_WDS_startClient
  (JNIEnv *e, jclass cls, jstring xml, jstring user, jstring pass)
{
  rdp = new RDP();
  memset(rdp, 0, sizeof(RDP));

  rdp->inst = GetModuleHandle(NULL);

  memset(&rdp->wc, 0, sizeof(WNDCLASS));
  rdp->wc.lpfnWndProc = &WindowProc;
  rdp->wc.hInstance = rdp->inst;
  rdp->wc.lpszClassName = "jfRDPClient";
  rdp->wc.hbrBackground = (HBRUSH)COLOR_WINDOW;
  RegisterClass(&rdp->wc);

  //calc full size
  int sx = GetSystemMetrics(SM_CXSCREEN);
  int sy = GetSystemMetrics(SM_CYSCREEN);
  int x = 800;
  int y = 600;
  y += GetSystemMetrics(SM_CYCAPTION);
  x += GetSystemMetrics(SM_CXFIXEDFRAME);
  y += GetSystemMetrics(SM_CYFIXEDFRAME);

  int px = (sx - x) / 2;
  int py = (sy - y) / 2;

  rdp->wnd = CreateWindow("jfRDPClient", "jfRDP", WS_TILEDWINDOW | WS_VISIBLE,
    px, py, x, y, NULL, NULL, rdp->inst, NULL);

  rdp->cls = new MyClass();

  int res = CoCreateInstance(CLSID_RDPViewer
    , NULL
    , CLSCTX_INPROC_SERVER | CLSCTX_INPROC_HANDLER
    , IID_IUnknown
    , (void**)&rdp->rdpUnknown);
  if (res != 0) {
    printf("CoCreateInstance failed!\n");
    return JNI_FALSE;
  }

  //get AX interfaces
  res = rdp->rdpUnknown->QueryInterface(IID_IRDPSRAPIViewer, (void**)&rdp->rdpViewer);
  if (res != 0) {
    printf("RDPClient does not support IID_IRDPSRAPIViewer\n");
    return JNI_FALSE;
  }

  res = rdp->rdpUnknown->QueryInterface(IID_IOleObject, (void**)&rdp->rdpOO);
  if (res != 0) {
    printf("RDPClient does not support IOleObject\n");
    return JNI_FALSE;
  }

  res = rdp->rdpUnknown->QueryInterface(IID_IOleInPlaceObject, (void**)&rdp->rdpOIPO);
  if (res != 0) {
    printf("QueryInterface(IID_IOleInPlaceObject) failed\n");
    return JNI_FALSE;
  }

  //embed object
  res = rdp->rdpOO->SetClientSite(rdp->cls);
  if (res != 0) {
    printf("IOleObject.SetClientSite() failed\n");
    return JNI_FALSE;
  }

  res = OleSetContainedObject(rdp->rdpOO, TRUE);
  if (res != 0) {
    printf("Ole.OleSetContainedObject() failed\n");
    return JNI_FALSE;
  }

  rdp->rect.right = 800;
  rdp->rect.bottom = 600;
  res = rdp->rdpOO->DoVerb(OLEIVERB_SHOW, NULL
    , rdp->cls, -1, rdp->wnd, &rdp->rect);
  if (res != 0) {
    printf("IOleObject.DoVerb() failed : %08x\n", res);
    return JNI_FALSE;
  }

  const char *cxml = e->GetStringUTFChars(xml, NULL);
  printf("xml=%s\n", cxml);
  BSTR bstr_xml = bstr_alloc(cxml);
  e->ReleaseStringUTFChars(xml, cxml);
  const char *cuser = e->GetStringUTFChars(user, NULL);
  BSTR bstr_user = bstr_alloc(cuser);
  e->ReleaseStringUTFChars(user, cuser);
  const char *cpass = e->GetStringUTFChars(pass, NULL);
  BSTR bstr_pass = bstr_alloc(cpass);
  e->ReleaseStringUTFChars(pass, cpass);
  res = rdp->rdpViewer->Connect(bstr_xml, bstr_user, bstr_pass);
  if (res != 0) {
    printf("CreateInvitation() Failed\n");
    return 0;
  }
  bstr_free(bstr_xml);
  bstr_free(bstr_user);
  bstr_free(bstr_pass);

  rdp->rdpViewer->put_SmartSizing(TRUE);

  SetSize();

  rdp->active = TRUE;
  MSG msg;
  while (rdp->active) {
    GetMessage(&msg, NULL, 0, 0);
    TranslateMessage(&msg);
    DispatchMessage(&msg);
  }

  printf("Cleanup\n");

  rdp->rdpViewer->Disconnect();

  DestroyWindow(rdp->wnd);

  if (rdp->rdpOO != NULL) {
    rdp->rdpOO->Close(OLECLOSE_SAVEIFDIRTY);
    //release after rdpViewer
  }
  if (rdp->rdpViewer != NULL) {
    rdp->rdpViewer->Release();
    rdp->rdpViewer = NULL;
  }
  if (rdp->rdpOO != NULL) {
    rdp->rdpOO->Release();
    rdp->rdpOO = NULL;
  }
  if (rdp->rdpUnknown != NULL) {
    rdp->rdpUnknown->Release();
    rdp->rdpUnknown = NULL;
  }

  return JNI_TRUE;
}
