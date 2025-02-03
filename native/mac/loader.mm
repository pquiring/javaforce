//Java Launcher Mac

// - supports passing command line options to java main()
// - now loads CLASSPATH and MAINCLASS from resource file (*.cfg)
// - now globbs arguments (see ExpandStringArray())
// - now support AWT
// - define java.app.home to find exe/dll files

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <ctype.h>
#include <stddef.h>
#include <stdarg.h>

#include <pthread.h>
#include <unistd.h>
#include <dlfcn.h>
#include <dirent.h>

/* Support Cocoa event loop on the main thread */
#include <Cocoa/Cocoa.h>
#include <objc/objc-runtime.h>
#include <objc/objc-auto.h>

#ifndef MAX_PATH
  #define MAX_PATH 255
#endif

#include <jni.h>

#include "javaforce_controls_ni_DAQmx.h"
#include "javaforce_gl_GL.h"
#include "javaforce_ui_Font.h"
#include "javaforce_ui_Image.h"
#include "javaforce_ui_Window.h"
#include "javaforce_jni_JFNative.h"
#include "javaforce_jni_MacNative.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"
#include "javaforce_media_VideoBuffer.h"
#include "javaforce_net_PacketCapture.h"
#include "javaforce_cl_CL.h"

/* Global variables */
int type;
char version[MAX_PATH];
int size = MAX_PATH;
int (*CreateJavaVM)(void*,void*,void*);
int thread_handle;
int thread_id;
char **g_argv;
int g_argc;
void *jvm_dll;
pthread_t thread;
pthread_attr_t thread_attr;
char link1[MAX_PATH];
char link2[MAX_PATH];
char classpath[1024];
char mainclass[MAX_PATH];
char method[MAX_PATH];
char cfgargs[1024];
char app_path[MAX_PATH];
char app_name[MAX_PATH];

/* Prototypes */
void error(char *msg);
int JavaThread(void *ignore);
int loadProperties();

/** Displays the error message in a dialog box. */
void error(char *msg) {
  printf("Failed to start Java\nPlease visit www.java.com and install Java\nError:%s\n", msg);
  exit(0);
}

/** Converts array of C strings into array of Java strings */
jobjectArray
ConvertStringArray(JNIEnv *env, char **strv, int strc)
{
  jarray cls;
  jarray outArray;
  jstring str;
  int i;
  int cfgargscnt = 0;
  int p = 0;

  if (strlen(cfgargs) > 0) {
    cfgargscnt++;
  }

  cls = (*env)->FindClass(env, "java/lang/String");
  outArray = (*env)->NewObjectArray(env, strc + cfgargscnt, cls, 0);
  for (i = 0; i < cfgargscnt; i++) {
    str = (*env)->NewStringUTF(env, cfgargs);
    (*env)->SetObjectArrayElement(env, outArray, p++, str);
    (*env)->DeleteLocalRef(env, str);
  }
  for (i = 0; i < strc; i++) {
    str = (*env)->NewStringUTF(env, *strv++);
    (*env)->SetObjectArrayElement(env, outArray, p++, str);
    (*env)->DeleteLocalRef(env, str);
  }
  return outArray;
}

/** Expands array of arguments (globbing)
 * Also releases inArray.
 */
jobjectArray
ExpandStringArray(JNIEnv *env, jobjectArray inArray) {
  jarray cls;
  jmethodID mid;
  jarray outArray;

  cls = (*env)->FindClass(env, "javaforce/JF");
  mid = (*env)->GetStaticMethodID(env, cls, "expandArgs", "([Ljava/lang/String;)[Ljava/lang/String;");
  outArray = (*env)->CallStaticObjectMethod(env, cls, mid, inArray);
  (*env)->DeleteLocalRef(env, inArray);
  return outArray;
}

char *DOption = "-Djava.class.path=";

/** Create class path change ; to : */
char *CreateClassPath() {
  char *ClassPath;
  int sl = strlen(classpath);
  for(int a=0;a<sl;a++) {
    if (classpath[a] == ';') {
      classpath[a] = ':';
    }
  }
  ClassPath = (char*)malloc(sl + strlen(DOption) + 1);
  strcpy(ClassPath, DOption);
  strcat(ClassPath, classpath);
  return ClassPath;
}

void printException(JNIEnv *env) {
  jthrowable exc;
  exc = (*env)->ExceptionOccurred(env);
  if (exc == NULL) return;
  jclass newExcCls;
  (*env)->ExceptionDescribe(env);
  (*env)->ExceptionClear(env);
}

JavaVMInitArgs *BuildArgs() {
  JavaVMInitArgs *args;
  JavaVMOption *options;
  int nOpts = 0;
  char *opts[64];
  int idx;

#ifdef _JF_DEBUG
  opts[nOpts++] = "-Djava.debug=true";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.port=9010";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.local.only=false";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.authenticate=false";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.ssl=false";
#endif
  opts[nOpts++] = (char*)"-Djava.app.home=.";
  if (graal) {
    opts[nOpts++] = (char*)"-Djava.graal=true";
    opts[nOpts++] = (char*)"-Djava.home=.";
  } else {
    opts[nOpts++] = CreateClassPath();
  }
  opts[nOpts++] = (char*)"-Djavaforce.loader=true";
  if (strlen(xoptions) > 0) {
    char *x = xoptions;
    while (x != NULL) {
      opts[nOpts++] = x;
      x = strchr(x, ' ');
      if (x != NULL) {
        *x = 0;
        x++;
      }
    }
  }

  args = (JavaVMInitArgs*)malloc(sizeof(JavaVMInitArgs));
  memset(args, 0, sizeof(JavaVMInitArgs));
  options = (JavaVMOption*)malloc(sizeof(JavaVMOption) * nOpts);
  memset(options, 0, sizeof(JavaVMOption) * nOpts);

  for(idx=0;idx<nOpts;idx++) {
    options[idx].optionString = opts[idx];
  }

  args->version = JNI_VERSION_1_2;
  args->nOptions = nOpts;
  args->options = options;
  args->ignoreUnrecognized = JNI_FALSE;

  return args;
}

#include "../common/register.h"

/** Register natives embedded with executable. */
void registerAllNatives(JNIEnv *env) {
  registerCommonNatives(env);
}

/** Continues loading the JVM in a new Thread. */
int JavaThread(void *ignore) {
  JavaVM *jvm = NULL;
  JNIEnv *env = NULL;

  if ((*CreateJavaVM)(&jvm, &env, BuildArgs()) == -1) {
    error("Unable to create Java VM");
    return -1;
  }

  jclass cls = (*env)->FindClass(env, mainclass);
  if (cls == NULL) {
    printException(env);
    error("Unable to find main class");
    return -1;
  }
  jmethodID mid = (*env)->GetStaticMethodID(env, cls, method, "([Ljava/lang/String;)V");
  if (mid == NULL) {
    error("Unable to find main method");
    return -1;
  }
  char **argv = g_argv;
  int argc = g_argc;
  //skip argv[0]
  argv++;
  argc--;
  (*env)->CallStaticVoidMethod(env, cls, mid, ExpandStringArray(env, ConvertStringArray(env, argv, argc)));
  (*jvm)->DestroyJavaVM(jvm);  //waits till all threads are complete
  //NOTE : Swing creates the EDT to keep Java alive until all windows are disposed
  return 0;
}

char *resolvelink(char *in) {
  strcpy(link1, in);
  do {
    //with alternatives this can resolve a few times
    int len = readlink(link1, link2, MAX_PATH);
    if (len == -1) break;
    link2[len] = 0;
    strcpy(link1, link2);
  } while (1);
  return link1;
}

struct Header {
  char name[4];
  int size;
};

int loadProperties() {
  char app[MAX_PATH];
  char *data, *ln1, *ln2;
  int sl, fs;
  struct Header header;

  strcpy(method, "main");
  cfgargs[0] = 0;

  strcpy(app, app_name);
  strcat(app, ".cfg");

  int file = open(app, O_RDONLY);
  if (file == -1) {
    printf("cfg=%s\n", app);
    error("cfg not found");
    return -1;
  }
  fs = lseek(file, 0, SEEK_END);
  lseek(file, 0, SEEK_SET);
  data = (char*)malloc(fs + 1);
  read(file, data, fs);
  close(file);
  data[fs] = 0;
  ln1 = data;
  classpath[0] = 0;
  mainclass[0] = 0;
  while (ln1 != NULL) {
    ln2 = strstr(ln1, "\r\n");
    if (ln2 != NULL) {
      *ln2 = 0;
      ln2++;
      *ln2 = 0;
      ln2++;
    } else {
      ln2 = strchr(ln1, '\n');
      if (ln2 != NULL) {
        *ln2 = 0;
        ln2++;
      }
    }
    if (strncmp(ln1, "CLASSPATH=", 10) == 0) {
      strcpy(classpath, ln1 + 10);
    }
    else if (strncmp(ln1, "MAINCLASS=", 10) == 0) {
      strcpy(mainclass, ln1 + 10);
    }
    else if (strncmp(ln1, "METHOD=", 7) == 0) {
      strcpy(method, ln1 + 7);
    }
    else if (strncmp(ln1, "ARGS=", 5) == 0) {
      strcpy(cfgargs, ln1 + 5);
    }
    ln1 = ln2;
  }
  free(data);
  return 0;
}

static void dummyTimer(CFRunLoopTimerRef timer, void *info) {}

static void ParkEventLoop() {
    // RunLoop needs at least one source, and 1e20 is pretty far into the future
    CFRunLoopTimerRef t = CFRunLoopTimerCreate(kCFAllocatorDefault, 1.0e20, 0.0, 0, 0, dummyTimer, NULL);
    CFRunLoopAddTimer(CFRunLoopGetCurrent(), t, kCFRunLoopDefaultMode);
    CFRelease(t);

    // Park this thread in the main run loop.
    int32_t result;
    do {
        result = CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1.0e20, false);
    } while (result != kCFRunLoopRunFinished);
}

void changeToAppHome(char* path_exe) {
  char *slash = strrchr(path_exe, '/');
  if (slash == NULL) {
    strcpy(app_path, ".");
    strcpy(app_name, path_exe);
    return;
  }
  *slash = 0;
  strcpy(app_path, path_exe);
  chdir(app_path);
  strcpy(app_name, slash+1);
}

/** Main entry point. */
int main(int argc, char **argv) {
  void *retval;
  char var[80];
  char *path;

  g_argv = argv;
  g_argc = argc;

  changeToAppHome(argv[0]);

  loadProperties();
  snprintf(var, sizeof(var), "JAVA_MAIN_CLASS_%d", getpid());
  //TODO : mainclass convert '/' to '.'
  setenv(var, mainclass, 1);

  //open libjli.dylib
  jvm_dll = dlopen("./jre/lib/libjli.dylib", RTLD_NOW);
  if (jvm_dll == NULL) {
    error("Unable to open libjli.dylib");
  }

  CreateJavaVM = (int (*)(void*,void*,void*)) dlsym(jvm_dll, "JNI_CreateJavaVM");
  if (CreateJavaVM == NULL) {
    error("Unable to find Java interfaces in libjli.dylib");
  }

  //now continue in new thread (not really necessary but avoids some Java bugs)
  pthread_attr_init(&thread_attr);

  pthread_create(&thread, &thread_attr, (void *(*) (void *))&JavaThread, NULL);

  //Must run a GUI loop on main thread in MacOSX
  ParkEventLoop();

  pthread_join(thread, &retval);

  return 0;
}
