#include <dlfcn.h>
#include <stdlib.h>
#include <fcntl.h>  //open
#include <termios.h>  //com ports
#include <unistd.h>  //close select stat
#include <stdio.h>
#ifndef __FreeBSD__
#include <linux/videodev2.h>  //V4L2
#endif
#include <sys/ioctl.h>  //ioctl
#include <sys/mman.h>  //mmap
#ifndef __FreeBSD__
#include <sys/inotify.h>
#endif  //__FreeBSD__
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <signal.h>
#include <errno.h>
#include <string.h>  //memcpy
#include <utime.h>
#include <time.h>  //nanosleep
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <security/pam_appl.h>
#include <ncurses.h>  //wtimeout wgetch

#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "../common/string.h"
#include "../common/array.h"
#include "../common/library.h"
#include "../common/register.h"

static bool debug = false;

#ifdef __arm__
  #define __RASPBERRY_PI__
#endif

#ifdef __aarch64__
  #define __RASPBERRY_PI__
#endif

#ifdef __RASPBERRY_PI__
  #include "gpio.c"
  #include "i2c.c"
#endif

#ifdef __GNUC__
  #pragma GCC diagnostic ignored "-Wint-to-pointer-cast"
#endif

void* jawt = NULL;
jboolean (JNICALL *_JAWT_GetAWT)(JNIEnv *e, JAWT *c) = NULL;
jboolean isWayland = JNI_FALSE;

void* x11 = NULL;
Display* (*_XOpenDisplay)(void*);
void (*_XCloseDisplay)(void*);
Atom (*_XInternAtom)(Display *display, char *atom_name, Bool only_if_exists);
int (*_XChangeProperty)(Display *display, Window w, Atom property, Atom type, int format, int mode, const unsigned char* data, int nelements);
Status (*_XSendEvent)(Display *display, Window w, Bool propagate, long event_mask, XEvent *event_send);
void (*_XSetSelectionOwner)(Display *display, Atom selection, Window owner, Time time);
int (*_XSelectInput)(Display *display, Window w, long event_mask);
void (*_XMapWindow)(Display *display, Window w);
void (*_XUnmapWindow)(Display *display, Window w);
void (*_XNextEvent)(Display *display, XEvent *event_return);
Status (*_XIconifyWindow)(Display *display, Window w, int screen_number);
void (*_XRaiseWindow)(Display *display, Window w);
KeyCode (*_XKeysymToKeycode)(Display *display, KeySym keysym);
void (*_XGetInputFocus)(Display *display, Window *focus_return, int *revert_to_return);
Window (*_XDefaultRootWindow)(Display *display);
int (*_XMoveResizeWindow)(Display *display, Window w, int x, int y, unsigned width, unsigned height);
int (*_XReparentWindow)(Display *display, Window w, Window parent, int x, int y);
Window (*_XCreateSimpleWindow)(Display *display, Window parent, int x,  int  y,  unsigned  int width,  unsigned  int  height,  unsigned  int  border_width, unsigned long border, unsigned long background);
int (*_XGetWindowProperty)(Display *display, Window w, Atom property, long long_offset, long long_length, Bool _delete, Atom req_type, Atom *actual_type_return, int *actual_format_return, unsigned long *nitems_return, unsigned long *bytes_after_return, unsigned char **prop_return);
int (*_XFree)(void *data);
Status (*_XGetClassHint)(Display *display, Window w, XClassHint *class_hints_return);
Status (*_XFetchName)(Display *display, Window w, char **window_name_return);

void* xgl = NULL;
void* (*_glXCreateContext)(void *x11, void *vi, void *shareList, int directRender);
int (*_glXDestroyContext)(void *x11, void *ctx);
int (*_glXMakeCurrent)(void *x11, int win, void *ctx);
void* (*_glXGetProcAddress)(const char *name);
void (*_glXSwapBuffers)(void *x11, int win);
void* (*_glXChooseVisual)(void *x11, int res, int *attrs);

void *v4l2 = NULL;
int (*_v4l2_open)(const char *file, int oflag, ...);
int (*_v4l2_close)(int fd);
int (*_v4l2_dup)(int fd);
int (*_v4l2_ioctl)(int fd, unsigned long int request, ...);
int (*_v4l2_read)(int fd, void* buffer, size_t n);
void* (*_v4l2_mmap)(void *start, size_t length, int prot, int flags, int fd, int64_t offset);
int (*_v4l2_munmap)(void *_start, size_t length);

void *pam = NULL;
int (*_pam_start)(const char *service_name, const char *user, const struct pam_conv *pam_conversation, pam_handle_t **pamh);
int (*_pam_authenticate)(pam_handle_t *pamh, int flags);
int (*_pam_end)(pam_handle_t *pamh, int pam_status);

void *ncurses = NULL;
WINDOW* (*_initscr)();
int (*_raw)();
int (*_noecho)();
void (*_wtimeout)(WINDOW *win, int delay);
int (*_wgetch)(WINDOW *win);
int (*_ungetch)(int ch);
int (*_endwin)();
WINDOW **_stdscr;

void sleep_ms(int milliseconds) {
  struct timespec req, rem;

  req.tv_sec = milliseconds / 1000;
  req.tv_nsec = (milliseconds % 1000) * 1000000;

  // nanosleep will return -1 if interrupted by a signal
  // in which case the remaining time will be in 'rem'
  while (nanosleep(&req, &rem) == -1) {
    req = rem; // Continue sleeping for the remaining time
  }
}

jboolean lnxInit(const char* libX11_so, const char* libgl_so, const char* libv4l2_so, const char* libpam_so, const char* libncurses_so)
{
  if (jawt == NULL) {
    jawt = loadLibrary("libjawt.so");
    if (jawt == NULL) {
      printf("Warning:dlopen(libjawt.so) unsuccessful\n");
      return JNI_FALSE;
    }
    getFunction(jawt, (void**)&_JAWT_GetAWT, "JAWT_GetAWT");
  }
  isWayland = getenv("WAYLAND_DISPLAY") != NULL;
  if (x11 == NULL && libX11_so != NULL) {
    x11 = dlopen(libX11_so, RTLD_LAZY | RTLD_GLOBAL);
    if (x11 == NULL) {
      printf("Warning:dlopen(libX11.so) unsuccessful\n");
    } else {
      getFunction(x11, (void**)&_XOpenDisplay, "XOpenDisplay");
      getFunction(x11, (void**)&_XCloseDisplay, "XCloseDisplay");
      getFunction(x11, (void**)&_XInternAtom, "XInternAtom");
      getFunction(x11, (void**)&_XChangeProperty, "XChangeProperty");
      getFunction(x11, (void**)&_XSendEvent, "XSendEvent");
      getFunction(x11, (void**)&_XSetSelectionOwner, "XSetSelectionOwner");
      getFunction(x11, (void**)&_XSelectInput, "XSelectInput");
      getFunction(x11, (void**)&_XMapWindow, "XMapWindow");
      getFunction(x11, (void**)&_XUnmapWindow, "XUnmapWindow");
      getFunction(x11, (void**)&_XNextEvent, "XNextEvent");
      getFunction(x11, (void**)&_XIconifyWindow, "XIconifyWindow");
      getFunction(x11, (void**)&_XRaiseWindow, "XRaiseWindow");
      getFunction(x11, (void**)&_XKeysymToKeycode, "XKeysymToKeycode");
      getFunction(x11, (void**)&_XGetInputFocus, "XGetInputFocus");
      getFunction(x11, (void**)&_XDefaultRootWindow, "XDefaultRootWindow");
      getFunction(x11, (void**)&_XMoveResizeWindow, "XMoveResizeWindow");
      getFunction(x11, (void**)&_XReparentWindow, "XReparentWindow");
      getFunction(x11, (void**)&_XCreateSimpleWindow, "XCreateSimpleWindow");
      getFunction(x11, (void**)&_XGetWindowProperty, "XGetWindowProperty");
      getFunction(x11, (void**)&_XFree, "XFree");
      getFunction(x11, (void**)&_XGetClassHint, "XGetClassHint");
      getFunction(x11, (void**)&_XFetchName, "XFetchName");
    }
  }
  if (xgl == NULL && libgl_so != NULL) {
    xgl = dlopen(libgl_so, RTLD_LAZY | RTLD_GLOBAL);
    if (xgl == NULL) {
      printf("Warning:dlopen(libGL.so) unsuccessful\n");
    } else {
      getFunction(xgl, (void**)&_glXCreateContext, "glXCreateContext");
      getFunction(xgl, (void**)&_glXDestroyContext, "glXDestroyContext");
      getFunction(xgl, (void**)&_glXMakeCurrent, "glXMakeCurrent");
      getFunction(xgl, (void**)&_glXGetProcAddress, "glXGetProcAddress");
      getFunction(xgl, (void**)&_glXSwapBuffers, "glXSwapBuffers");
      getFunction(xgl, (void**)&_glXChooseVisual, "glXChooseVisual");
    }
  }
  if (v4l2 == NULL && libv4l2_so != NULL) {
    v4l2 = dlopen(libv4l2_so, RTLD_LAZY | RTLD_GLOBAL);
    if (v4l2 == NULL) {
      printf("Warning:dlopen(libv4l2.so) unsuccessful\n");
    } else {
      getFunction(v4l2, (void**)&_v4l2_open, "v4l2_open");
      getFunction(v4l2, (void**)&_v4l2_close, "v4l2_close");
      getFunction(v4l2, (void**)&_v4l2_dup, "v4l2_dup");
      getFunction(v4l2, (void**)&_v4l2_ioctl, "v4l2_ioctl");
      getFunction(v4l2, (void**)&_v4l2_read, "v4l2_read");
      getFunction(v4l2, (void**)&_v4l2_mmap, "v4l2_mmap");
      getFunction(v4l2, (void**)&_v4l2_munmap, "v4l2_munmap");
    }
  }
  if (pam == NULL && libpam_so != NULL) {
    pam = dlopen(libpam_so, RTLD_LAZY | RTLD_GLOBAL);
    if (pam == NULL) {
      printf("Warning:dlopen(libpam.so) unsuccessful\n");
    } else {
      getFunction(pam, (void**)&_pam_start, "pam_start");
      getFunction(pam, (void**)&_pam_authenticate, "pam_authenticate");
      getFunction(pam, (void**)&_pam_end, "pam_end");
    }
  }
  if (ncurses == NULL && libncurses_so != NULL) {
    ncurses = dlopen(libncurses_so, RTLD_LAZY | RTLD_GLOBAL);
    if (ncurses == NULL) {
      printf("Warning:dlopen(libncurses.so) unsuccessful\n");
    } else {
      getFunction(ncurses, (void**)&_initscr, "initscr");
      getFunction(ncurses, (void**)&_raw, "raw");
      getFunction(ncurses, (void**)&_noecho, "noecho");
      getFunction(ncurses, (void**)&_wtimeout, "wtimeout");
      getFunction(ncurses, (void**)&_wgetch, "wgetch");
      getFunction(ncurses, (void**)&_ungetch, "ungetch");
      getFunction(ncurses, (void**)&_endwin, "endwin");
      getFunction(ncurses, (void**)&_stdscr, "stdscr");
    }
  }
  return JNI_TRUE;
}

static jlong getX11ID(JNIEnv *e, jobject c) {
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  if (jawt == NULL) return 0;
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
  JAWT_X11DrawingSurfaceInfo* xdsi = (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;
  if (xdsi == NULL) {
    printf("JAWT.platformInfo == NULL\n");
    return 0;
  }
  jlong handle = xdsi->drawable;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

struct WLToolkit {
  void* wl_surface;
  void* wl_view;
};

static jlong getWaylandID(JNIEnv *e, jobject c) {
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  if (jawt == NULL) return 0;
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
  WLToolkit* xdsi = (WLToolkit*)dsi->platformInfo;
  printf("xdsi=%p\n", xdsi);
  if (xdsi == NULL) {
    printf("JAWT.platformInfo == NULL\n");
    return 0;
  }
  jlong handle = (jlong)xdsi->wl_surface;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

#include "../common/ui.cpp"

#include "../common/gl.cpp"

jboolean glPlatformInit() {
  return JNI_TRUE;
}

jboolean glGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)(*_glXGetProcAddress)(name);  //get OpenGL 1.x function
  if (func == NULL) {
    func = (void*)dlsym(xgl, name);  //get OpenGL 2.0+ function
  }
  if (func != NULL) {
    *funcPtr = func;
    return JNI_TRUE;
  } else {
    printf("OpenGL:Error:Can not find function:%s\n", name);
    return JNI_FALSE;
  }
}

void uiWindowSetIcon(GLFWContextFFM* ctx, const char* filename, jint x, jint y)
{
  //TODO
}

extern "C" {
  JNIEXPORT void (*_uiWindowSetIcon)(GLFWContextFFM*,const char*,jint,jint) = &uiWindowSetIcon;
}

#include "camera.cpp"

#include "comport.cpp"

#include "pty.cpp"

#include "x11.cpp"

#include "pam.cpp"

#ifndef __FreeBSD__

#include "monitor-folder.cpp"

#endif  //__FreeBSD__

#include "file.cpp"

#include "console.cpp"

#include "../common/ffmpeg.cpp"

#include "../common/videobuffer.cpp"

#include "../common/opencl.cpp"

#include "../common/types.h"

#include "../common/font.cpp"

#include "../common/image.cpp"

#include "../common/pcap.cpp"

#include "../common/vm.cpp"

#include "../speexdsp/speex_dsp.c"

#include "../common/register.cpp"

//misc

void setEnv(const char* name, const char* value)
{
  setenv(name, value, 1);
}

jint getUID()
{
  return getuid();
}

JNI_GetCreatedJavaVMs_t get_JNI_GetCreatedJavaVMs() {
  void* lib = dlopen("libjvm.so", RTLD_NOW | RTLD_GLOBAL);
  if (lib == NULL) {
    printf("dlopen('libjvm.so') failed\n");
    return NULL;
  }
  return (JNI_GetCreatedJavaVMs_t)dlsym(lib, "JNI_GetCreatedJavaVMs");
}

extern "C" {
JNIEXPORT void* _ignored() {
  void* _setup_JFHeap = (void*)&setup_JFHeap;
  void* _set_upcall_FFMArray = (void*)&set_upcall_FFMArray;
  return _setup_JFHeap;
}
}

extern "C" {
  JNIEXPORT jboolean (*_lnxInit)(const char*,const char*,const char*,const char*,const char*) = &lnxInit;
  JNIEXPORT void (*_setEnv)(const char*,const char*) = &setEnv;
  JNIEXPORT jint (*_getUID)() = & getUID;

  JNIEXPORT jboolean JNICALL LinuxAPIinit() {return JNI_TRUE;}
}
