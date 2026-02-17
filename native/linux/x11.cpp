JNIEXPORT jlong JNICALL Java_javaforce_jni_X11_x11_1get_1id
  (JNIEnv *e, jclass c, jobject window)
{
  return getX11ID(e,window);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1set_1desktop
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = (*_XOpenDisplay)(NULL);
  int ret;
  Atom* states = (Atom*)malloc(sizeof(Atom) * 4);
  Atom* types = (Atom*)malloc(sizeof(Atom) * 1);
  for(int a=0;a<2;a++) {
    Atom state = (*_XInternAtom)(display, "_NET_WM_STATE", 0);
    states[0] = (*_XInternAtom)(display, "_NET_WM_STATE_BELOW", 0);
    states[1] = (*_XInternAtom)(display, "_NET_WM_STATE_SKIP_PAGER", 0);
    states[2] = (*_XInternAtom)(display, "_NET_WM_STATE_SKIP_TASKBAR", 0);
    states[3] = (*_XInternAtom)(display, "_NET_WM_STATE_STICKY", 0);
    ret = (*_XChangeProperty)(display, xid, state, XA_ATOM, 32, PropModeReplace, (const unsigned char*)states, 4);
    Atom type = (*_XInternAtom)(display, "_NET_WM_WINDOW_TYPE", 0);
    types[0] = (*_XInternAtom)(display, "_NET_WM_WINDOW_TYPE_DESKTOP", 0);
    ret = (*_XChangeProperty)(display, xid, type, XA_ATOM, 32, PropModeReplace, (const unsigned char*)types, 1);
  }
  free(states);
  free(types);
  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1set_1dock
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = (*_XOpenDisplay)(NULL);
  int ret;
  Atom* states = (Atom*)malloc(sizeof(Atom) * 4);
  Atom* types = (Atom*)malloc(sizeof(Atom) * 1);
  for(int a=0;a<2;a++) {
    Atom state = (*_XInternAtom)(display, "_NET_WM_STATE", 0);
    states[0] = (*_XInternAtom)(display, "_NET_WM_STATE_ABOVE", 0);
    states[1] = (*_XInternAtom)(display, "_NET_WM_STATE_SKIP_PAGER", 0);
    states[2] = (*_XInternAtom)(display, "_NET_WM_STATE_SKIP_TASKBAR", 0);
    states[3] = (*_XInternAtom)(display, "_NET_WM_STATE_STICKY", 0);
    ret = (*_XChangeProperty)(display, xid, state, XA_ATOM, 32, PropModeReplace, (const unsigned char*)states, 4);
    Atom type = (*_XInternAtom)(display, "_NET_WM_WINDOW_TYPE", 0);
    types[0] = (*_XInternAtom)(display, "_NET_WM_WINDOW_TYPE_DOCK", 0);
    ret = (*_XChangeProperty)(display, xid, type, XA_ATOM, 32, PropModeReplace, (const unsigned char*)types, 1);
  }
  free(states);
  free(types);
  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1set_1strut
  (JNIEnv *e, jclass c, jlong xid, jint panelHeight, jint x, jint y, jint width, jint height)
{
  Display* display = (*_XOpenDisplay)(NULL);
  Atom strut = (*_XInternAtom)(display, "_NET_WM_STRUT_PARTIAL", 0);
  Atom *values = (Atom*)malloc(sizeof(Atom) * 12);
  values[0]=(Atom)0;  //left
  values[1]=(Atom)0;  //right
  values[2]=(Atom)0;  //top
  values[3]=(Atom)panelHeight;   //bottom
  values[4]=(Atom)0;  //left_start_y
  values[5]=(Atom)(height-1);  //left_end_y
  values[6]=(Atom)0;  //right_start_y
  values[7]=(Atom)(height-1);  //right_end_y
  values[8]=(Atom)0;  //top_start_x
  values[9]=(Atom)(width-1);  //top_end_x
  values[10]=(Atom)0;  //bottom_start_x
  values[11]=(Atom)(width-1);  //bottom_end_x
  (*_XChangeProperty)(display, xid, strut, XA_CARDINAL, 32, PropModeReplace, (const unsigned char*)values, 12);
  free(values);
  (*_XCloseDisplay)(display);
}

#define MAX_TRAY_ICONS 64
static XID tray_icons[MAX_TRAY_ICONS];
static int screen_width;
static Atom tray_opcode;//, tray_data;
static XID tray_window;
static jboolean tray_active;
static int tray_count = 0;
static JavaVM *x11_VM;
static JNIEnv *x11_tray_e;
static jobject x11_listener;
static jmethodID mid_x11_listener_trayIconAdded;
static jmethodID mid_x11_listener_trayIconRemoved;
static jmethodID mid_x11_listener_windowsChanged;
static JNIEnv *x11_window_e;
static jclass cid_javaforce_linux_Linux;
static jmethodID mid_x11_window_add;
static jmethodID mid_x11_window_del;
#define tray_icon_size 24  //fixed
static int tray_height = 24+4;
static int tray_rows = 2;
static int borderSize = 4;
static int tray_pos = 0;
static int tray_pad = 2;
static int tray_width = 24+4;

static JNIEnv* x11_GetEnv() {
  JNIEnv *env;
  if (x11_VM->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) return env;
  x11_VM->AttachCurrentThread((void**)&env, NULL);
  return env;
}

static void tray_move_icons() {
  Display* display = (*_XOpenDisplay)(NULL);
  int a, x = tray_pad, y;
  int y1 = 0;
  int y2 = 0;
  if (tray_rows == 1) {
    y1 = (tray_height - tray_icon_size) / 2;
  } else {
    int d = (tray_height - (tray_icon_size*2))/3;
    y1 = d;
    y2 = d + tray_icon_size + d;
  }
  y = y1;
  for(a=0;a<MAX_TRAY_ICONS;a++) {
    if (tray_icons[a] == 0) continue;
    (*_XMoveResizeWindow)(display, tray_icons[a], x, y, tray_icon_size, tray_icon_size);
    if (y == y1 && tray_rows > 1) {
      y = y2;
    } else {
      y = y1;
      x += tray_icon_size + tray_pad;
    }
  }
  //reposition/resize tray window
  int cols = (tray_count + (tray_rows > 1 ? 1 : 0)) / tray_rows;
  if (cols == 0) cols = 1;
  int px = tray_pos - (cols * (tray_icon_size+tray_pad)) - tray_pad - borderSize;
  int py = borderSize;
  int sx = (cols * (tray_icon_size+tray_pad)) + tray_pad;
  tray_width = sx;
  int sy = tray_height;  //tray_rows * (tray_icon_size + tray_pad) + tray_pad;
//    JFLog.log("Tray Position:" + px + "," + py + ",size=" + sx + "," + sy);
  (*_XMoveResizeWindow)(display, tray_window, px, py, sx, sy);
  (*_XCloseDisplay)(display);
}

static void tray_add_icon(XID w) {
  if (tray_count == MAX_TRAY_ICONS) return;  //ohoh
  Display* display = (*_XOpenDisplay)(NULL);
  tray_count++;
  int a;
  for(a=0;a<MAX_TRAY_ICONS;a++) {
    if (tray_icons[a] == 0) {
      tray_icons[a] = w;
      break;
    }
  }
  (*_XReparentWindow)(display, w, tray_window, 0, 0);
  tray_move_icons();
  (*_XMapWindow)(display, w);
  x11_tray_e->CallVoidMethod(x11_listener, mid_x11_listener_trayIconAdded);
  if (x11_tray_e->ExceptionCheck()) x11_tray_e->ExceptionClear();
  (*_XCloseDisplay)(display);
}

/* Tray opcode messages from System Tray Protocol Specification
   http://standards.freedesktop.org/systemtray-spec/systemtray-spec-0.3.html
*/
#define SYSTEM_TRAY_REQUEST_DOCK   0
#define SYSTEM_TRAY_BEGIN_MESSAGE  1
#define SYSTEM_TRAY_CANCEL_MESSAGE 2

#define SYSTEM_TRAY_STOP 0x100  //more like a wake up
#define SYSTEM_TRAY_REPOSITION 0x101

static void tray_client_message(XClientMessageEvent *ev) {
  if (ev->message_type == tray_opcode) {
    switch (ev->data.l[1]) {
      case SYSTEM_TRAY_REQUEST_DOCK:
        tray_add_icon(ev->data.l[2]);
        break;
      case SYSTEM_TRAY_BEGIN_MESSAGE:
        break;
      case SYSTEM_TRAY_CANCEL_MESSAGE:
        break;
      case SYSTEM_TRAY_REPOSITION:
//          JFLog.log("Tray:ClientMessage = SYSTEM_TRAY_REPOSITION");
        tray_move_icons();
        break;
      case SYSTEM_TRAY_STOP:
        //does nothing, but main while loop will now exit
        break;
    }
  }
}

static void tray_remove_icon(XDestroyWindowEvent *ev) {
  int a;
  for(a=0;a<MAX_TRAY_ICONS;a++) {
    if (tray_icons[a] == 0) continue;
    if (tray_icons[a] == ev->window) {
      tray_icons[a] = 0;
      tray_count--;
      tray_move_icons();
      x11_tray_e->CallVoidMethod(x11_listener, mid_x11_listener_trayIconRemoved, tray_count);
      if (x11_tray_e->ExceptionCheck()) x11_tray_e->ExceptionClear();
      break;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1tray_1main
  (JNIEnv *e, jclass c, jlong pid, jint screenWidth, jint trayPos, jint trayHeight)
{
  x11_tray_e = e;
  for(int a=0;a<MAX_TRAY_ICONS;a++) {
    tray_icons[a] = 0;
  }

  XEvent ev;
  screen_width = screenWidth;
  tray_pos = trayPos;
  tray_height = trayHeight;
  if (tray_height >= tray_icon_size * 2 + tray_pad * 3) {
    tray_rows = 2;
  } else {
    tray_rows = 1;
  }
  Display* display = (*_XOpenDisplay)(NULL);
  Atom tray_atom = (*_XInternAtom)(display, "_NET_SYSTEM_TRAY_S0", False);
  tray_opcode = (*_XInternAtom)(display, "_NET_SYSTEM_TRAY_OPCODE", False);
//  tray_data = (*_XInternAtom)(display, "_NET_SYSTEM_TRAY_MESSAGE_DATA", False);

  tray_window = (*_XCreateSimpleWindow)(
    display,
    (XID)pid,  //parent id
    trayPos - tray_icon_size - 4 - borderSize, borderSize,  //pos
    tray_icon_size + 4, 52,  //size
    1,  //border_width
    (0xcccccc),  //border clr
    (0xdddddd));  //backgnd clr

  (*_XSetSelectionOwner)(display, tray_atom, tray_window, CurrentTime);

  //get DestroyNotify events
  (*_XSelectInput)(display, tray_window, SubstructureNotifyMask);

  (*_XMapWindow)(display, tray_window);

  tray_active = JNI_TRUE;
  while (tray_active) {
    (*_XNextEvent)(display, &ev);
    switch (ev.type) {
      case ClientMessage:
        tray_client_message((XClientMessageEvent*)&ev);
        break;
      case DestroyNotify:
        tray_remove_icon((XDestroyWindowEvent*)&ev);
        break;
    }
  }

  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1tray_1reposition
  (JNIEnv *e, jclass c, jint screenWidth, jint trayPos, jint trayHeight)
{
  if (screenWidth != -1) screen_width = screenWidth;
  if (trayPos != -1) tray_pos = trayPos;
  if (trayHeight != -1) tray_height = trayHeight;
  if (tray_height >= tray_icon_size * 2 + tray_pad * 3) {
    tray_rows = 2;
  } else {
    tray_rows = 1;
  }
  //X11 is not thread safe so can't call tray_move_icons() from here, send a msg instead
  Display* display = (*_XOpenDisplay)(NULL);

  XClientMessageEvent event;

  event.type = ClientMessage;
  event.display = display;
  event.window = tray_window;
  event.message_type = tray_opcode;
  event.format = 32;
  event.data.l[1] = SYSTEM_TRAY_REPOSITION;

  (*_XSendEvent)(display, event.window, True, 0, (XEvent*)&event);

  (*_XCloseDisplay)(display);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_X11_x11_1tray_1width
  (JNIEnv *e, jclass c)
{
  return tray_width;
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1tray_1stop
  (JNIEnv *e, jclass c)
{
  tray_active = JNI_FALSE;
  Display* display = (*_XOpenDisplay)(NULL);

  XClientMessageEvent event;

  event.type = ClientMessage;
  event.display = display;
  event.window = tray_window;
  event.message_type = tray_opcode;
  event.format = 32;
  event.data.l[1] = SYSTEM_TRAY_STOP;

  (*_XSendEvent)(display, event.window, True, 0, (XEvent*)&event);

  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1set_1listener
  (JNIEnv *e, jclass c, jobject obj)
{
  jclass cls = e->FindClass("javaforce/linux/X11Listener");
  x11_listener = e->NewGlobalRef(obj);
  mid_x11_listener_trayIconAdded = e->GetMethodID(cls, "trayIconAdded", "(I)V");
  mid_x11_listener_trayIconRemoved = e->GetMethodID(cls, "trayIconRemoved", "(I)V");
  mid_x11_listener_windowsChanged = e->GetMethodID(cls, "windowsChanged", "()V");
}

#define MAX_WINDOWS 1024

static jboolean window_list_active = JNI_FALSE;

static XID window_list[MAX_WINDOWS];
static int window_list_size = 0;
static int window_list_event_mask[MAX_WINDOWS];

static void x11_update_window_list(Display *display) {
  XID newList[MAX_WINDOWS];
  int newListSize = 0;

  XID root_window = (*_XDefaultRootWindow)(display);

  Atom net_client_list = (*_XInternAtom)(display, "_NET_CLIENT_LIST", False);
//    Atom net_client_list_stacking = (*_XInternAtom)(display, "_NET_CLIENT_LIST_STACKING", False);
  Atom net_name = (*_XInternAtom)(display, "_NET_WM_NAME", False);
  Atom net_pid = (*_XInternAtom)(display, "_NET_WM_PID", False);
  Atom net_state = (*_XInternAtom)(display, "_NET_WM_STATE", False);
  Atom net_skip_taskbar = (*_XInternAtom)(display, "_NET_WM_STATE_SKIP_TASKBAR", False);

  unsigned char *prop;
  unsigned long nItems;
  Atom l1;
  unsigned long l2;
  int i1;
  (*_XGetWindowProperty)(display, root_window, net_client_list, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
  int nWindows = nItems;
  XID *list = (XID*)prop;
  for(int a=0;a<nWindows;a++) {
    XID xid = list[a];

    //check state for skip taskbar
    (*_XGetWindowProperty)(display, xid, net_state, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
    if (nItems > 0) {
      Atom* atoms = (Atom*)prop;
      jboolean found = JNI_FALSE;
      for(int n=0;n<nItems;n++) {
        if (atoms[n] == net_skip_taskbar) {
          found = JNI_TRUE;
        }
      }
      (*_XFree)(prop);
      if (found) continue;
    }

    //get window pid
    (*_XGetWindowProperty)(display, xid, net_pid, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
    int pid = -1;
    XID *xids = NULL;
    if (nItems > 0) {
      xids = (XID*)prop;
      pid = xids[0];
    }

    //get title
    (*_XGetWindowProperty)(display, xid, net_name, 0, 1024, False, AnyPropertyType, &l1, &i1, &nItems, &l2, &prop);
    char *title = NULL;
    if (nItems > 0) {
      title = (char*)prop;
    }

    //get name
    char *name = NULL;
    (*_XFetchName)(display, xid, &name);

    //get res_name, res_class
    char *res_name = NULL, *res_class = NULL;
    XClassHint hint;
    (*_XGetClassHint)(display, xid, &hint);
    res_name = hint.res_name;
    res_class = hint.res_class;

    //add to list
    jstring jtitle = NULL;
    if (title != NULL) jtitle = x11_window_e->NewStringUTF(title);
    jstring jname = NULL;
    if (name != NULL) jname = x11_window_e->NewStringUTF(name);
    jstring jres_name = NULL;
    if (res_name != NULL) jres_name = x11_window_e->NewStringUTF(res_name);
    jstring jres_class = NULL;
    if (res_class != NULL) jres_class = x11_window_e->NewStringUTF(res_class);
    x11_window_e->CallStaticVoidMethod(cid_javaforce_linux_Linux, mid_x11_window_add, xid, pid, jtitle, jname, jres_name, jres_class);
    if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
    if (jtitle != NULL) x11_window_e->DeleteLocalRef(jtitle);
    if (jname != NULL) x11_window_e->DeleteLocalRef(jname);
    if (jres_name != NULL) x11_window_e->DeleteLocalRef(jres_name);
    if (jres_class != NULL) x11_window_e->DeleteLocalRef(jres_class);
    if (xids != NULL) {
      (*_XFree)(xids);
    }
    if (title != NULL) {
      (*_XFree)(title);
    }
    if (name != NULL) {
      (*_XFree)(name);
    }
    if (res_name != NULL) {
      (*_XFree)(res_name);
    }
    if (res_class != NULL) {
      (*_XFree)(res_class);
    }
    newList[newListSize++] = xid;
  }
  (*_XFree)(list);

  //add newList to currentList
  for(int a=0;a<newListSize;a++) {
    XID xid = newList[a];
    jboolean found = JNI_FALSE;
    for(int b=0;b<window_list_size;b++) {
      if (window_list[b] == xid) {
        found = JNI_TRUE;
        break;
      }
    }
    if (!found) {
      if (window_list_size == MAX_WINDOWS) break;  //Ohoh
      window_list[window_list_size] = xid;
      window_list_event_mask[window_list_size] = (*_XSelectInput)(display, xid, PropertyChangeMask);
      window_list_size++;
    }
  }
  //remove from currentList if not in newList
  for(int a=0;a<window_list_size;) {
    XID xid = window_list[a];
    jboolean found = JNI_FALSE;
    for(int b=0;b<newListSize;b++) {
      if (newList[b] == xid) {
        found = JNI_TRUE;
        break;
      }
    }
    if (!found) {
      (*_XSelectInput)(display, xid, window_list_event_mask[a]);
      for(int z=a+1;z<window_list_size;z++) {
        window_list[z-1] = window_list[z];
        window_list_event_mask[z-1] = window_list_event_mask[z];
      }
      window_list_size--;
      x11_window_e->CallStaticVoidMethod(cid_javaforce_linux_Linux, mid_x11_window_del, xid);
      if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
    } else {
      a++;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1window_1list_1main
  (JNIEnv *e, jclass c)
{
  cid_javaforce_linux_Linux = e->FindClass("javaforce/linux/Linux");
  mid_x11_window_add = e->GetStaticMethodID(cid_javaforce_linux_Linux, "x11_window_add", "(JILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
  mid_x11_window_del = e->GetStaticMethodID(cid_javaforce_linux_Linux, "x11_window_del", "(J)V");

  x11_window_e = e;

  XEvent ev;

  Display* display = (*_XOpenDisplay)(NULL);

  XID root_window = (*_XDefaultRootWindow)(display);

  Atom net_client_list = (*_XInternAtom)(display, "_NET_CLIENT_LIST", False);
  Atom net_client_list_stacking = (*_XInternAtom)(display, "_NET_CLIENT_LIST_STACKING", False);
  Atom net_name = (*_XInternAtom)(display, "_NET_WM_NAME", False);
  Atom net_pid = (*_XInternAtom)(display, "_NET_WM_PID", False);
  Atom net_state = (*_XInternAtom)(display, "_NET_WM_STATE", False);

  (*_XSelectInput)(display, root_window, PropertyChangeMask);

  window_list_active = JNI_TRUE;
  while (window_list_active) {
    (*_XNextEvent)(display, &ev);
    switch (ev.type) {
      case PropertyNotify:
        XPropertyEvent *xpropertyevent = (XPropertyEvent*)&ev;
        Atom atom = xpropertyevent->atom;
        if (
          (atom == net_client_list) ||
          (atom == net_client_list_stacking) ||
          (atom == net_name) ||
          (atom == net_pid) ||
          (atom == net_state)
           )
        {
          x11_update_window_list(display);
          x11_window_e->CallVoidMethod(x11_listener, mid_x11_listener_windowsChanged);
          if (x11_window_e->ExceptionCheck()) x11_window_e->ExceptionClear();
        }
        break;
    }
  }

  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1window_1list_1stop
  (JNIEnv *e, jclass c)
{
  window_list_active = JNI_FALSE;
  //TODO : send a message to ??? to cause main() loop to abort
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1minimize_1all
  (JNIEnv *e, jclass c)
{
  Display* display = (*_XOpenDisplay)(NULL);
  //TODO : need to lock list ???
  for(int a=0;a<window_list_size;a++) {
    (*_XIconifyWindow)(display, window_list[a], 0);
  }
  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1raise_1window
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = (*_XOpenDisplay)(NULL);
  (*_XRaiseWindow)(display, (XID)xid);
  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1map_1window
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = (*_XOpenDisplay)(NULL);
  (*_XMapWindow)(display, (XID)xid);
  (*_XCloseDisplay)(display);
}

JNIEXPORT void JNICALL Java_javaforce_jni_X11_x11_1unmap_1window
  (JNIEnv *e, jclass c, jlong xid)
{
  Display* display = (*_XOpenDisplay)(NULL);
  (*_XUnmapWindow)(display, (XID)xid);
  (*_XCloseDisplay)(display);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_X11_x11_1keysym_1to_1keycode
  (JNIEnv *e, jclass c, jchar keysym)
{
  Display* display = (*_XOpenDisplay)(NULL);
  int keycode = (*_XKeysymToKeycode)(display, keysym);
  (*_XCloseDisplay)(display);
  switch (keysym) {
    case '!':
    case '@':
    case '#':
    case '$':
    case '%':
    case '^':
    case '&':
    case '*':
    case '"':
    case ':':
      keycode |= 0x100;
  }
  return keycode;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_X11_x11_1send_1event__IZ
  (JNIEnv *e, jclass c, jint keycode, jboolean down)
{
  Display* display = (*_XOpenDisplay)(NULL);

  Window x11id;
  int revert;
  (*_XGetInputFocus)(display, &x11id, &revert);

  XKeyEvent event;

  event.type = (down ? KeyPress : KeyRelease);
  event.keycode = keycode & 0xff;
  event.display = display;
  event.window = x11id;
  event.root = (*_XDefaultRootWindow)(display);
  event.subwindow = None;
  event.time = CurrentTime;
  event.x = 1;
  event.y = 1;
  event.x_root = 1;
  event.y_root = 1;
  event.same_screen = True;
  if ((keycode & 0x100) == 0x100) event.state = ShiftMask;

  int status = (*_XSendEvent)(display, event.window, True, down ? KeyPressMask : KeyReleaseMask, (XEvent*)&event);

  (*_XCloseDisplay)(display);

  return status != 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_X11_x11_1send_1event__JIZ
  (JNIEnv *e, jclass c, jlong id, jint keycode, jboolean down)
{
  Display* display = (*_XOpenDisplay)(NULL);

  XKeyEvent event;

  event.type = (down ? KeyPress : KeyRelease);
  event.keycode = keycode & 0xff;
  event.display = display;
  event.window = (Window)id;
  event.root = (*_XDefaultRootWindow)(display);
  event.subwindow = None;
  event.time = CurrentTime;
  event.x = 1;
  event.y = 1;
  event.x_root = 1;
  event.y_root = 1;
  event.same_screen = True;
  if ((keycode & 0x100) == 0x100) event.state = ShiftMask;

  int status = (*_XSendEvent)(display, event.window, True, down ? KeyPressMask : KeyReleaseMask, (XEvent*)&event);

  (*_XCloseDisplay)(display);

  return status != 0;
}
