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

#include "javaforce_jni_LnxNative.h"
#include "javaforce_jni_GLJNI.h"
#include "javaforce_jni_CameraJNI.h"
#include "javaforce_jni_MediaJNI.h"
#include "javaforce_jni_UIJNI.h"
#include "javaforce_jni_PCapJNI.h"
#include "javaforce_jni_CLJNI.h"

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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_lnxInit
  (JNIEnv *e, jclass c, jstring libX11_so, jstring libgl_so, jstring libv4l2_so, jstring libpam_so, jstring libncurses_so)
{
  if (jawt == NULL) {
    jawt = dlopen("libjawt.so", RTLD_LAZY | RTLD_GLOBAL);
    if (jawt == NULL) {
      printf("Warning:dlopen(libjawt.so) unsuccessful\n");
      return JNI_FALSE;
    }
    getFunction(jawt, (void**)&_JAWT_GetAWT, "JAWT_GetAWT");
  }
  if (x11 == NULL && libX11_so != NULL) {
    const char *clibX11_so = e->GetStringUTFChars(libX11_so,NULL);
    x11 = dlopen(clibX11_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libX11_so, clibX11_so);
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
    const char *clibgl_so = e->GetStringUTFChars(libgl_so,NULL);
    xgl = dlopen(clibgl_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libgl_so, clibgl_so);
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
    const char *clibv4l2_so = e->GetStringUTFChars(libv4l2_so,NULL);
    v4l2 = dlopen(clibv4l2_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libv4l2_so, clibv4l2_so);
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
    const char *clibpam_so = e->GetStringUTFChars(libpam_so,NULL);
    pam = dlopen(clibpam_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libpam_so, clibpam_so);
    if (pam == NULL) {
      printf("Warning:dlopen(libpam.so) unsuccessful\n");
    } else {
      getFunction(pam, (void**)&_pam_start, "pam_start");
      getFunction(pam, (void**)&_pam_authenticate, "pam_authenticate");
      getFunction(pam, (void**)&_pam_end, "pam_end");
    }
  }
  if (ncurses == NULL && libncurses_so != NULL) {
    const char *clibncurses_so = e->GetStringUTFChars(libncurses_so,NULL);
    ncurses = dlopen(clibncurses_so, RTLD_LAZY | RTLD_GLOBAL);
    e->ReleaseStringUTFChars(libncurses_so, clibncurses_so);
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

static long getX11ID(JNIEnv *e, jobject c) {
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
  long handle = xdsi->drawable;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

#include "../common/ui-jni.cpp"
#include "../common/ui-ffm.cpp"

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

JNIEXPORT void JNICALL Java_javaforce_jni_UIJNI_uiWindowSetIcon
  (JNIEnv *e, jobject c, jlong id, jstring filename, jint x, jint y)
{
  //TODO
}

extern "C" {
  JNIEXPORT void (*_uiWindowSetIcon)(GLFWContextFFM*,const char*,jint,jint) = &uiWindowSetIcon;
}

#include "camera-jni.cpp"
#include "camera-ffm.cpp"

#include "comport-jni.cpp"
#include "comport-ffm.cpp"

#include "pty.cpp"

#include "x11.cpp"

#include "pam.cpp"

#ifndef __FreeBSD__

#include "monitor-folder-jni.cpp"
#include "monitor-folder-ffm.cpp"

#endif  //__FreeBSD__

//misc

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_setenv
  (JNIEnv *e, jclass c, jstring name, jstring value)
{
  const char *cname = e->GetStringUTFChars(name,NULL);
  const char *cvalue = e->GetStringUTFChars(value,NULL);
  setenv(cname, cvalue, 1);
  e->ReleaseStringUTFChars(name, cname);
  e->ReleaseStringUTFChars(value, cvalue);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_fileGetMode
  (JNIEnv *e, jclass c, jstring name)
{
  struct stat s;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  e->ReleaseStringUTFChars(name, cname);
  return s.st_mode;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fileSetMode
  (JNIEnv *e, jclass c, jstring name, jint mode)
{
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::chmod((const char *)cname, mode);
  e->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fileSetAccessTime
  (JNIEnv *e, jclass c, jstring name, jlong ts)
{
  struct stat s;
  struct utimbuf tb;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  ts /= 1000L;
  tb.actime = ts;
  tb.modtime = s.st_mtime;
  ::utime((const char *)cname, &tb);
  e->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_fileSetModifiedTime
  (JNIEnv *e, jclass c, jstring name, jlong ts)
{
  struct stat s;
  struct utimbuf tb;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  ts /= 1000L;
  tb.actime = s.st_atime;
  tb.modtime = ts;
  ::utime((const char *)cname, &tb);
  e->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_fileGetID
  (JNIEnv *e, jclass c, jstring name)
{
  struct stat s;
  const char *cname = e->GetStringUTFChars(name,NULL);
  ::lstat((const char *)cname, (struct stat*)&s);
  e->ReleaseStringUTFChars(name, cname);
  return s.st_ino;
}

#include "console.cpp"

#include "../common/ffmpeg.cpp"

#include "../common/videobuffer.cpp"

#include "../common/opencl-jni.cpp"
#include "../common/opencl-ffm.cpp"

#include "../common/types.h"

#include "../common/font-jni.cpp"
#include "../common/font-ffm.cpp"

#include "../common/image-jni.cpp"
#include "../common/image-ffm.cpp"

#include "../common/pcap-jni.cpp"
#include "../common/pcap-ffm.cpp"

#include "../common/vm.cpp"

#include "../speexdsp/speex_dsp.c"

#ifndef __FreeBSD__

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

#endif  //__FreeBSD__

//Linux native methods
static JNINativeMethod javaforce_jni_LnxNative[] = {
  {"lnxInit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_LnxNative_lnxInit},
  {"lnxServiceStop", "()Z", (void *)&Java_javaforce_jni_LnxNative_lnxServiceStop},
  {"ptyAlloc", "()J", (void *)&Java_javaforce_jni_LnxNative_ptyAlloc},
  {"ptyFree", "(J)V", (void *)&Java_javaforce_jni_LnxNative_ptyFree},
  {"ptyOpen", "(J)Ljava/lang/String;", (void *)&Java_javaforce_jni_LnxNative_ptyOpen},
  {"ptyClose", "(J)V", (void *)&Java_javaforce_jni_LnxNative_ptyClose},
  {"ptyRead", "(J[B)I", (void *)&Java_javaforce_jni_LnxNative_ptyRead},
  {"ptyWrite", "(J[B)V", (void *)&Java_javaforce_jni_LnxNative_ptyWrite},
  {"ptySetSize", "(JII)V", (void *)&Java_javaforce_jni_LnxNative_ptySetSize},
  {"ptyChildExec", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)J", (void *)&Java_javaforce_jni_LnxNative_ptyChildExec},
  {"authUser", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_LnxNative_authUser},
  {"setenv", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)&Java_javaforce_jni_LnxNative_setenv},
  {"enableConsoleMode", "()V", (void *)&Java_javaforce_jni_LnxNative_enableConsoleMode},
  {"disableConsoleMode", "()V", (void *)&Java_javaforce_jni_LnxNative_disableConsoleMode},
  {"getConsoleSize", "()[I", (void *)&Java_javaforce_jni_LnxNative_getConsoleSize},
  {"getConsolePos", "()[I", (void *)&Java_javaforce_jni_LnxNative_getConsolePos},
  {"readConsole", "()C", (void *)&Java_javaforce_jni_LnxNative_readConsole},
  {"peekConsole", "()Z", (void *)&Java_javaforce_jni_LnxNative_peekConsole},
  {"writeConsole", "(I)V", (void *)&Java_javaforce_jni_LnxNative_writeConsole},
  {"writeConsoleArray", "([BII)V", (void *)&Java_javaforce_jni_LnxNative_writeConsoleArray},
  {"fileGetMode", "(Ljava/lang/String;)I", (void *)&Java_javaforce_jni_LnxNative_fileGetMode},
  {"fileSetMode", "(Ljava/lang/String;I)V", (void *)&Java_javaforce_jni_LnxNative_fileSetMode},
  {"fileSetAccessTime", "(Ljava/lang/String;J)V", (void *)&Java_javaforce_jni_LnxNative_fileSetAccessTime},
  {"fileSetModifiedTime", "(Ljava/lang/String;J)V", (void *)&Java_javaforce_jni_LnxNative_fileSetModifiedTime},
  {"fileGetID", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_LnxNative_fileGetID},
};

static JNINativeMethod javaforce_jni_X11[] = {
  {"x11_get_id", "(Ljava/awt/Window;)J", (void *)&Java_javaforce_jni_X11_x11_1get_1id},
  {"x11_set_desktop", "(J)V", (void *)&Java_javaforce_jni_X11_x11_1set_1desktop},
  {"x11_set_dock", "(J)V", (void *)&Java_javaforce_jni_X11_x11_1set_1dock},
  {"x11_set_strut", "(JIIIII)V", (void *)&Java_javaforce_jni_X11_x11_1set_1strut},
  {"x11_tray_main", "(JIII)V", (void *)&Java_javaforce_jni_X11_x11_1tray_1main},
  {"x11_tray_reposition", "(III)V", (void *)&Java_javaforce_jni_X11_x11_1tray_1reposition},
  {"x11_tray_width", "()I", (void *)&Java_javaforce_jni_X11_x11_1tray_1width},
  {"x11_tray_stop", "()V", (void *)&Java_javaforce_jni_X11_x11_1tray_1stop},
  {"x11_set_listener", "(Ljavaforce/linux/X11Listener;)V", (void *)&Java_javaforce_jni_X11_x11_1set_1listener},
  {"x11_window_list_main", "()V", (void *)&Java_javaforce_jni_X11_x11_1window_1list_1main},
  {"x11_window_list_stop", "()V", (void *)&Java_javaforce_jni_X11_x11_1window_1list_1stop},
  {"x11_minimize_all", "()V", (void *)&Java_javaforce_jni_X11_x11_1minimize_1all},
  {"x11_raise_window", "(J)V", (void *)&Java_javaforce_jni_X11_x11_1raise_1window},
  {"x11_map_window", "(J)V", (void *)&Java_javaforce_jni_X11_x11_1map_1window},
  {"x11_unmap_window", "(J)V", (void *)&Java_javaforce_jni_X11_x11_1unmap_1window},
  {"x11_keysym_to_keycode", "(C)I", (void *)&Java_javaforce_jni_X11_x11_1keysym_1to_1keycode},
  {"x11_send_event", "(IZ)Z", (void *)&Java_javaforce_jni_X11_x11_1send_1event__IZ},
  {"x11_send_event", "(JIZ)Z", (void *)&Java_javaforce_jni_X11_x11_1send_1event__JIZ},
};

static JNINativeMethod javaforce_jni_ComPortJNI[] = {
  {"comOpen", "(Ljava/lang/String;I)J", (void *)&Java_javaforce_jni_ComPortJNI_comOpen},
  {"comClose", "(J)V", (void *)&Java_javaforce_jni_ComPortJNI_comClose},
  {"comRead", "(J[BI)I", (void *)&Java_javaforce_jni_ComPortJNI_comRead},
  {"comWrite", "(J[BI)I", (void *)&Java_javaforce_jni_ComPortJNI_comWrite},
};

#ifndef __FreeBSD__
static JNINativeMethod javaforce_jni_MonitorFolderJNI[] = {
  {"monitorFolderCreate", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_MonitorFolderJNI_monitorFolderCreate},
  {"monitorFolderPoll", "(JLjavaforce/io/FolderListener;)V", (void *)&Java_javaforce_jni_MonitorFolderJNI_monitorFolderPoll},
  {"monitorFolderClose", "(J)V", (void *)&Java_javaforce_jni_MonitorFolderJNI_monitorFolderClose},
};
#endif

extern "C" void lnxnative_register(JNIEnv *env);

void lnxnative_register(JNIEnv *env) {
  jclass cls;

  cls = findClass(env, "javaforce/jni/LnxNative");
  registerNatives(env, cls, javaforce_jni_LnxNative, sizeof(javaforce_jni_LnxNative)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/jni/X11");
  registerNatives(env, cls, javaforce_jni_X11, sizeof(javaforce_jni_X11)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/jni/ComPortJNI");
  registerNatives(env, cls, javaforce_jni_ComPortJNI, sizeof(javaforce_jni_ComPortJNI)/sizeof(JNINativeMethod));

#ifndef __FreeBSD__
  cls = findClass(env, "javaforce/jni/MonitorFolderJNI");
  registerNatives(env, cls, javaforce_jni_MonitorFolderJNI, sizeof(javaforce_jni_MonitorFolderJNI)/sizeof(JNINativeMethod));
#endif
}

#include "../common/register.cpp"
