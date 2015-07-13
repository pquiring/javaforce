#include <windows.h>
#include "rdpmingw.h"

#include "server_WDS.h"

//this one does not define it as "extern"
#define DEFINE_GUID_(name,l,w1,w2,b1,b2,b3,b4,b5,b6,b7,b8) const GUID name = { l, w1, w2, { b1, b2, b3, b4, b5, b6, b7, b8 } }

DEFINE_GUID_(CLSID_RDPSession,0x9B78F0E6,0x3E05,0x4A5B,0xB2,0xE8,0xE7,0x43,0xA8,0x95,0x6B,0x65);
DEFINE_GUID_(DIID__IRDPSessionEvents,0x98a97042,0x6698,0x40e9,0x8e,0xfd,0xb3,0x20,0x09,0x90,0x00,0x4b);
DEFINE_GUID_(IID_IRDPSRAPISharingSession,0xeeb20886,0xe470,0x4cf6,0x84,0x2b,0x27,0x39,0xc0,0xec,0x5c,0xfb);
DEFINE_GUID_(IID_IRDPSRAPIAttendee,0xec0671b3,0x1b78,0x4b80,0xa4,0x64,0x91,0x32,0x24,0x75,0x43,0xe3);
DEFINE_GUID_(IID_IRDPSRAPIAttendeeManager,0xba3a37e8,0x33da,0x4749,0x8d,0xa0,0x07,0xfa,0x34,0xda,0x79,0x44);
DEFINE_GUID_(IID_IRDPSRAPISessionProperties,0x339b24f2,0x9bc0,0x4f16,0x9a,0xac,0xf1,0x65,0x43,0x3d,0x13,0xd4);
DEFINE_GUID_(CLSID_RDPSRAPIApplicationFilter,0xe35ace89,0xc7e8,0x427e,0xa4,0xf9,0xb9,0xda,0x07,0x28,0x26,0xbd);
DEFINE_GUID_(CLSID_RDPSRAPIInvitationManager,0x53d9c9db,0x75ab,0x4271,0x94,0x8a,0x4c,0x4e,0xb3,0x6a,0x8f,0x2b);

class MyClass : public IDispatch {
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
        *ppvObject = this;
        return S_OK;
      }

      if (riid == IID_IDispatch) {
        *ppvObject = this;
        return S_OK;
      }

      if (riid == DIID__IRDPSessionEvents) {
        *ppvObject = this;
        return S_OK;
      }

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
////
      int res;
      VARIANT vr;

      switch (dispIdMember)
      {
        case DISPID_RDPSRAPI_EVENT_ON_ATTENDEE_CONNECTED:
          {
            CTRL_LEVEL level;
            IDispatch *pDispatch;
            IRDPSRAPIAttendee *pAttendee;

            printf("onAttendeeConnected\n");

            vr.vt = VT_DISPATCH;
            vr.byref = NULL;

            res = DispGetParam(pDispParams, 0, VT_DISPATCH, &vr, puArgErr);

            if (res != 0)
            {
              printf("DispGetParam failed\n");
              return res;
            }

            pDispatch = (IDispatch*)(vr.ppdispVal);

            res = pDispatch->QueryInterface(IID_IRDPSRAPIAttendee, (void**)&pAttendee);

            if (res != 0)
            {
              printf("IDispatch::QueryInterface(IRDPSRAPIAttendee) failed\n");
              return res;
            }

            //level = CTRL_LEVEL_VIEW;
            level = CTRL_LEVEL_INTERACTIVE;

            res = pAttendee->put_ControlLevel(level);

            if (res != 0)
            {
              printf("IRDPSRAPIAttendee::put_ControlLevel() failed\n");
              return res;
            }

            pAttendee->Release();

//            if (listener != NULL) {listener.onAttendeeConnect();}
          }
          break;

        case DISPID_RDPSRAPI_EVENT_ON_ATTENDEE_DISCONNECTED:
          printf("onAttendeeDisconnect\n");
          //TODO : stop rdp
//          if (listener != NULL) {listener.onAttendeeDisconnect();}
          break;

        case DISPID_RDPSRAPI_EVENT_ON_ATTENDEE_UPDATE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_ERROR:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIEWER_CONNECTED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIEWER_DISCONNECTED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIEWER_AUTHENTICATED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIEWER_CONNECTFAILED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_CTRLLEVEL_CHANGE_REQUEST:
          {
            CTRL_LEVEL level;
            IDispatch *pDispatch;
            IRDPSRAPIAttendee *pAttendee;

            printf("onAttendeeControlLevelChangeRequest\n");

            vr.vt = VT_INT;
            vr.byref = NULL;

            res = DispGetParam(pDispParams, 1, VT_INT, &vr, puArgErr);

            if (res != 0)
            {
              printf("DispGetParam(1, VT_INT) failed\n");
              return res;
            }

            level = (CTRL_LEVEL)vr.intVal;

            vr.vt = VT_DISPATCH;
            vr.byref = NULL;

            res = DispGetParam(pDispParams, 0, VT_DISPATCH, &vr, puArgErr);

            if (res != 0)
            {
              printf("DispGetParam(0, VT_DISPATCH) failed\n");
              return res;
            }

            pDispatch = (IDispatch*)(vr.ppdispVal);

            res = pDispatch->QueryInterface(IID_IRDPSRAPIAttendee, (void**)&pAttendee);

            if (res != 0)
            {
              printf("IDispatch::QueryInterface(IRDPSRAPIAttendee) failed\n");
              return res;
            }

            res = pAttendee->put_ControlLevel(level);

            if (res != 0)
            {
              printf("IRDPSRAPIAttendee::put_ControlLevel() failed\n");
              return res;
            }

            pAttendee->Release();
          }
          break;

        case DISPID_RDPSRAPI_EVENT_ON_GRAPHICS_STREAM_PAUSED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_GRAPHICS_STREAM_RESUMED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIRTUAL_CHANNEL_JOIN:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIRTUAL_CHANNEL_LEAVE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIRTUAL_CHANNEL_DATARECEIVED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_VIRTUAL_CHANNEL_SENDCOMPLETED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_APPLICATION_OPEN:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_APPLICATION_CLOSE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_APPLICATION_UPDATE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_WINDOW_OPEN:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_WINDOW_CLOSE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_WINDOW_UPDATE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_APPFILTER_UPDATE:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_SHARED_RECT_CHANGED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_FOCUSRELEASED:
          break;

        case DISPID_RDPSRAPI_EVENT_ON_SHARED_DESKTOP_SETTINGS_CHANGED:
          break;

//            case DISPID_RDPAPI_EVENT_ON_BOUNDING_RECT_CHANGED:
//              break;
      }

      return S_OK;
    }
////
};

struct RDP {
  IUnknown *rdpUnknown;
  IRDPSRAPISharingSession *rdpSession;
  IRDPSRAPISessionProperties *rdpProps;
  IRDPSRAPIInvitationManager *rdpIM;
  IRDPSRAPIInvitation *rdpI;
  IConnectionPointContainer *rdpCPC;
  IConnectionPoint *rdpCP;
  int token, left, top, right, bottom, depth;
  MyClass *cls;
  char *cs;
  volatile int active;
  int threadId;
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

int wstrlen(OLECHAR *s16) {
  int len = 0;
  while (*s16 != 0) {
    s16++;
    len++;
  }
  return len;
}

void wstrcpy(char *dest, OLECHAR *src) {
  int sl = wstrlen(src) + 1;
  for(int a=0;a<sl;a++) {
    dest[a] = src[a];
  }
}

JNIEXPORT jlong JNICALL Java_server_WDS_startServer
  (JNIEnv *e, jclass cls, jstring user, jstring group, jstring pass, jint numAttend, jint port)
{
  RDP *rdp = new RDP();
  memset(rdp, 0, sizeof(RDP));

  int res = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
  if (res != S_OK) return 0;

  res = CoCreateInstance(CLSID_RDPSession
    , NULL, CLSCTX_ALL
    , IID_IUnknown, (void**)&rdp->rdpUnknown);
  if (res != 0) {
    printf("CoCreateInstance failed\n");
    return 0;
  }

  res = rdp->rdpUnknown->QueryInterface(IID_IRDPSRAPISharingSession, (void**)&rdp->rdpSession);
  if (res != 0) {
    printf("QueryInterface(IConnectionPointContainer) failed\n");
    return 0;
  }

  res = rdp->rdpSession->QueryInterface(IID_IConnectionPointContainer, (void**)&rdp->rdpCPC);
  if (res != 0) {
    printf("QueryInterface(IConnectionPointContainer) failed\n");
    return 0;
  }

  res = rdp->rdpCPC->FindConnectionPoint(DIID__IRDPSessionEvents, &rdp->rdpCP);
  if (res != 0) {
    printf("FindConnectionPoint failed\n");
    return 0;
  }

  rdp->cls = new MyClass();

  res = rdp->rdpCP->Advise(rdp->cls, (DWORD*)&rdp->token);
  if (res != 0) {
    printf("IConnectionPoint.Advise() failed\n");
    return 0;
  }

  res = rdp->rdpSession->get_Properties(&rdp->rdpProps);
  if (res != 0) {
    printf("RDPSession.get_Properties() failed\n");
    return 0;
  }

  res = rdp->rdpSession->GetDesktopSharedRect((long int *)&rdp->left, (long int *)&rdp->top, (long int *)&rdp->right, (long int *)&rdp->bottom);
  if (res != 0) {
    printf("GetDesktopSharedRect failed\n");
    return 0;
  }

////

  BSTR bstr = SysAllocString(L"PortId");
  VARIANT var;
  var.vt = VT_I4;
  var.intVal = port;

  res = rdp->rdpProps->put_Property(bstr, var);

  SysFreeString(bstr);

  if (res != 0)
  {
    printf( "IRDPSRAPISessionProperties::put_Property(PortId) failure: 0x%08X\n", res);
    return 0;
  }

  bstr = SysAllocString(L"DrvConAttach");
  var.vt = VT_BOOL;
  var.boolVal = VARIANT_TRUE;

  res = rdp->rdpProps->put_Property(bstr, var);

  SysFreeString(bstr);

  if (res != 0)
  {
    printf( "IRDPSRAPISessionProperties::put_Property(DrvConAttach) failure: 0x%08X\n", res);
    return 0;
  }

  bstr = SysAllocString(L"PortProtocol");
  var.vt = VT_I4;

  //var.intVal = 0; // AF_UNSPEC
  var.intVal = 2; // AF_INET
  //var.intVal = 23; // AF_INET6

  res = rdp->rdpProps->put_Property(bstr, var);

  SysFreeString(bstr);

  if (res != 0)
  {
    printf( "IRDPSRAPISessionProperties::put_Property(PortProtocol) failure: 0x%08X\n", res);
    return 0;
  }

////

  res = rdp->rdpSession->Open();
  if (res != 0) {
    printf("RDPSession.Open() Failed\n");
    return 0;
  }

  res = rdp->rdpSession->get_ColorDepth((long int *)&rdp->depth);
  if (res != 0) {
    printf("RDPSession.get_ColorDepth() Failed\n");
    return 0;
  }

  res = rdp->rdpSession->get_Invitations(&rdp->rdpIM);
  if (res != 0) {
    printf("RDPSession.get_Invitations() Failed\n");
    return 0;
  }

  const char *cuser = e->GetStringUTFChars(user, NULL);
  BSTR bstr_user = bstr_alloc(cuser);
  e->ReleaseStringUTFChars(user, cuser);
  const char *cgroup = e->GetStringUTFChars(group, NULL);
  BSTR bstr_group = bstr_alloc(cgroup);
  e->ReleaseStringUTFChars(group, cgroup);
  const char *cpass = e->GetStringUTFChars(pass, NULL);
  BSTR bstr_pass = bstr_alloc(cpass);
  e->ReleaseStringUTFChars(pass, cpass);
  res = rdp->rdpIM->CreateInvitation(bstr_user, bstr_group, bstr_pass, numAttend, &rdp->rdpI);
  if (res != 0) {
    printf("CreateInvitation() Failed\n");
    return 0;
  }
  bstr_free(bstr_user);
  bstr_free(bstr_group);
  bstr_free(bstr_pass);

  if (rdp->rdpI == NULL) {
    printf("RDPSession.CreateInvitation() Failed\n");
    return 0;
  }

  BSTR cs;
  rdp->rdpI->get_ConnectionString(&cs);
  int sl = wstrlen(cs);
  rdp->cs = (char*)malloc(sl+1);
  wstrcpy(rdp->cs, cs);
  bstr_free(cs);

  return (jlong)rdp;
}

JNIEXPORT void JNICALL Java_server_WDS_runServer
  (JNIEnv *e, jclass cls, jlong id)
{
  RDP *rdp = (RDP*)id;
  rdp->threadId = GetCurrentThreadId();
  rdp->active = TRUE;
  MSG msg;
  while (rdp->active) {
    GetMessage(&msg,NULL,0,0);
    TranslateMessage(&msg);
    DispatchMessage(&msg);
  }

  printf("Cleanup\n");

  //do cleanup
  if (rdp->rdpSession != NULL) {
    rdp->rdpSession->Close();
    rdp->rdpSession->Release();
    rdp->rdpSession = NULL;
  }
  if (rdp->rdpUnknown != NULL) {
    rdp->rdpUnknown->Release();
    rdp->rdpUnknown = NULL;
  }
  if (rdp->rdpIM != NULL) {
    rdp->rdpIM->Release();
    rdp->rdpIM = NULL;
  }
  if (rdp->rdpI != NULL) {
    rdp->rdpI->Release();
    rdp->rdpI = NULL;
  }
  if (rdp->rdpProps != NULL) {
    rdp->rdpProps->Release();
    rdp->rdpProps = NULL;
  }
  if (rdp->rdpCPC != NULL) {
    rdp->rdpCPC->Release();
    rdp->rdpCPC = NULL;
  }
  if (rdp->rdpCP != NULL) {
    rdp->rdpCP->Unadvise(rdp->token);
    rdp->rdpCP->Release();
    rdp->rdpCP = NULL;
  }

  CoUninitialize();
}

JNIEXPORT void JNICALL Java_server_WDS_stopServer
  (JNIEnv *e, jclass cls, jlong id)
{
  RDP *rdp = (RDP*)id;
  rdp->active = FALSE;
  PostThreadMessage(rdp->threadId,WM_USER,0,0);
}

JNIEXPORT jstring JNICALL Java_server_WDS_getConnectionString
  (JNIEnv *e, jclass cls, jlong id)
{
  RDP *rdp = (RDP*)id;
  return e->NewStringUTF(rdp->cs);
}
