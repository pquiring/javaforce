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

#include "../common/ui.cpp"

#include "../common/gl.cpp"

#include "gl.cpp"

#include "../common/vk.cpp"

#include "vk.cpp"

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

#include "wayland.cpp"

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
  JNIEXPORT void (*_setEnv)(const char*,const char*) = &setEnv;
  JNIEXPORT jint (*_getUID)() = & getUID;

  JNIEXPORT jboolean JNICALL LinuxAPIinit(const char* libpam_so, const char* libncurses_so) {
    isWayland = getenv("WAYLAND_DISPLAY") != NULL;
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
}
