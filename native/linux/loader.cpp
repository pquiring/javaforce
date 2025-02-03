//Java Launcher Linux

// - supports passing command line options to java main()
// - now loads CLASSPATH and MAINCLASS from embedded resource file (*.cfg)
// - now globbs arguments (see ExpandStringArray())
// - native functions are now included in executable

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

#ifndef MAX_PATH
  #define MAX_PATH 255
#endif

#include <jni.h>

#include "javaforce_controls_ni_DAQmx.h"
#include "javaforce_gl_GL.h"
#include "javaforce_ui_Font.h"
#include "javaforce_ui_Image.h"
#include "javaforce_ui_Window.h"
#include "javaforce_jni_LnxNative.h"
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
char javahome[MAX_PATH];
char dll[MAX_PATH];
int size = MAX_PATH;
int (*CreateJavaVM)(void*,void*,void*);
int thread_handle;
int thread_id;
char **g_argv;
int g_argc;
JavaVM *g_jvm = NULL;
JNIEnv *g_env = NULL;
void *jvm_dll;
void *jawt_dll;
pthread_t thread;
pthread_attr_t thread_attr;
char link1[MAX_PATH];
char link2[MAX_PATH];
char classpath[1024];
char mainclass[MAX_PATH];
char method[MAX_PATH];
char xoptions[MAX_PATH];
char cfgargs[1024];
bool graal = false;
bool debug = false;
char errmsg[1024];

/* Prototypes */
void error(const char *msg);
bool JavaThread(void *ignore);
bool loadProperties();
bool InvokeMethodVoid(char *_class, char *_method, char *sign, jobject args);
jobject InvokeMethodObject(char *_class, char *_method, char *sign, jobject args);

/** Displays the error message in a dialog box. */
void error(const char *msg) {
  printf("Failed to start Java\nError:%s\n", msg);
  exit(0);
}

void printException(JNIEnv *env) {
  jthrowable exc;
  exc = env->ExceptionOccurred();
  if (exc == NULL) return;
  env->ExceptionDescribe();
  env->ExceptionClear();
}

/** Converts array of C strings into array of Java strings */
jobject
ConvertStringArray(JNIEnv *env, int strc, char **strv)
{
  jclass cls;
  jobjectArray outArray;
  jstring str;
  int i;

  cls = env->FindClass("java/lang/String");
  if (cls == NULL) {
    printException(g_env);
    error("Unable to find String class");
    return NULL;
  }
  outArray = env->NewObjectArray(strc, cls, 0);
  for (i = 0; i < strc; i++) {
    str = env->NewStringUTF(*strv++);
    env->SetObjectArrayElement(outArray, i, str);
    env->DeleteLocalRef(str);
  }
  return outArray;
}

/** Expands array of arguments (globbing)
 * Also releases inArray.
 */
jobject
ExpandStringArray(JNIEnv *env, jobject inArray) {
  jobject outArray;

  outArray = InvokeMethodObject("javaforce/JF", "expandArgs", "([Ljava/lang/String;)[Ljava/lang/String;", inArray);
  env->DeleteLocalRef(inArray);
  return outArray;
}

const char *DOption = "-Djava.class.path=";

#ifdef _JF_CLI
/** Create class path "as is" which should include jar files from current path. */
char *CreateClassPath() {
  int cplen = strlen(classpath);
  for(int a=0;a<cplen;a++) {
    if (classpath[a] == ';') {
      classpath[a] = ':';
    }
  }
  int len = strlen(DOption) + cplen + 1;
  char *ExpandedClassPath = (char*)malloc(len);
  ExpandedClassPath[0] = 0;
  strcat(ExpandedClassPath, DOption);
  strcat(ExpandedClassPath, classpath);
  return ExpandedClassPath;
}
#else
/** Create class path adding /usr/share/java to each element, and change ; to : */
char *CreateClassPath() {
  char *ClassPath;
  int sl = strlen(classpath);
  ClassPath = (char*)malloc(sl + 1);
  strcpy(ClassPath, classpath);
  int ml = strlen("/usr/share/java/");
  char *jar[32];
  jar[0] = ClassPath;
  int cnt = 1;
  int a;
  for(a=0;a<sl;a++) {
    if (ClassPath[a] == ';') {
      jar[cnt++] = ClassPath + a + 1;
      ClassPath[a] = 0;
    }
  }
  int len = strlen(DOption) + sl + (ml * cnt) + 1;
  char *env_classpath = getenv("CLASSPATH");
  if (env_classpath != NULL) {
    len += strlen(env_classpath) + 1;
  }
  char *ExpandedClassPath = (char*)malloc(len);
  ExpandedClassPath[0] = 0;
  strcat(ExpandedClassPath, DOption);
  for(a=0;a<cnt;a++) {
    if (a > 0) strcat(ExpandedClassPath, ":");
    if (strchr(jar[a], '/') == NULL) {
      strcat(ExpandedClassPath, "/usr/share/java/");
    }
    strcat(ExpandedClassPath, jar[a]);
  }
  if (env_classpath != NULL) {
    strcat(ExpandedClassPath, ":");
    strcat(ExpandedClassPath, env_classpath);
  }
  return ExpandedClassPath;
}
#endif

void convertClass(char *cls) {
  while (*cls) {
    if (*cls == '.') *cls = '/';
    cls++;
  }
}

/** invokes a static method returning void. */
bool InvokeMethodVoid(char *_class, char *_method, char *sign, jobject args) {
  convertClass(_class);
  jclass cls = g_env->FindClass(_class);
  if (cls == NULL) {
    printException(g_env);
    sprintf(errmsg, "Unable to find %s class", _class);
    error(errmsg);
    return false;
  }
  jmethodID mid = g_env->GetStaticMethodID(cls, _method, sign);
  if (mid == NULL) {
    printException(g_env);
    sprintf(errmsg, "Unable to find %s method", _method);
    error(errmsg);
    return false;
  }

  g_env->CallStaticVoidMethod(cls, mid, args);

  return true;
}

/** invokes a static method returning Object. */
jobject InvokeMethodObject(char *_class, char *_method, char *sign, jobject args) {
  convertClass(_class);
  jclass cls = g_env->FindClass(_class);
  if (cls == NULL) {
    printException(g_env);
    sprintf(errmsg, "Unable to find %s class", _class);
    error(errmsg);
    return NULL;
  }
  jmethodID mid = g_env->GetStaticMethodID(cls, _method, sign);
  if (mid == NULL) {
    printException(g_env);
    sprintf(errmsg, "Unable to find %s method", _method);
    error(errmsg);
    return NULL;
  }

  return g_env->CallStaticObjectMethod(cls, mid, args);
}

char* MakeString(char* fmt, char* path) {
  int len = strlen(fmt) - 2 + strlen(path) + 1;
  char* str = (char*)malloc(len);
  sprintf(str, fmt, path);
  return str;
}

JavaVMInitArgs *BuildArgs() {
  JavaVMInitArgs *args;
  JavaVMOption *options;
  int nOpts = 0;
  char *opts[64];
  int idx;

#ifdef _JF_DEBUG
  debug = true;
#endif

  if (debug) {
    opts[nOpts++] = "-Djava.debug=true";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.port=9010";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.local.only=false";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.authenticate=false";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.ssl=false";
  }

  opts[nOpts++] = (char*)"-Djava.app.home=/usr/bin";
  opts[nOpts++] = MakeString("-Djava.app.name=%s", mainclass);
  opts[nOpts++] = MakeString("-Dvisualvm.display.name=%s", mainclass);
  if (graal) {
    opts[nOpts++] = (char*)"-Djava.graal=true";
    opts[nOpts++] = (char*)"-Djava.home=/usr/bin";
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

void FreeArgs(JavaVMInitArgs *args) {
  int idx;
  for(idx=0;idx<args->nOptions;idx++) {
    free(args->options[idx].optionString);
  }
  free(args->options);
  free(args);
}

/** Creates a new JVM. */
bool CreateJVM() {
  JavaVMInitArgs *args = BuildArgs();
  if ((*CreateJavaVM)(&g_jvm, (void**)&g_env, args) == -1) {
    error("Unable to create Java VM");
    return false;
  }

//  FreeArgs(args);

  return true;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_lnxServiceStop(JNIEnv *env, jclass c) {
  return InvokeMethodVoid(mainclass, "serviceStop", "()V", NULL);
}

#include "../common/register.h"

extern "C" void lnxnative_register(JNIEnv *env);

extern "C" void vm_register(JNIEnv *env);

/** Register natives embedded with executable. */
void registerAllNatives(JNIEnv *env) {
  jclass cls;

  registerCommonNatives(env);

  lnxnative_register(env);

  vm_register(env);
}

/** Continues loading the JVM in a new Thread. */
bool JavaThread(void *ignore) {
  CreateJVM();

  registerAllNatives(g_env);

  //load linux shared libraries
  InvokeMethodVoid("javaforce/jni/LnxNative", "load", "()V", NULL);

#ifdef _JF_SERVICE
  if (g_argc == 2 && (strcmp(g_argv[1], "--stop") == 0)) {
    //request service shutdown
    return InvokeMethodVoid("javaforce/jni/LnxNative", "lnxServiceRequestStop", "()V", NULL);
  }
  //setup service shutdown
  InvokeMethodVoid("javaforce/jni/LnxNative", "lnxServiceInit", "()V", NULL);
#endif

  char **argv = g_argv;
  int argc = g_argc;
  //skip argv[0]
  argv++;
  argc--;
  InvokeMethodVoid(mainclass, method, "([Ljava/lang/String;)V", ExpandStringArray(g_env, ConvertStringArray(g_env, argc, argv)));

  g_jvm->DestroyJavaVM();  //waits till all threads are complete

  return true;
}

char *resolvelink(const char *in) {
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

#ifdef _JF_CLI
bool loadProperties() {
  bool have_classpath = false;
  bool have_mainclass = false;

  javahome[0] = 0;
  xoptions[0] = 0;

  char** argv = g_argv;
  int argc = g_argc;
  for(int a=1;a<argc;a++) {
    //skip arg
    g_argv++;
    g_argc--;
    char* arg = argv[a];
    if (arg[0] == 0) continue;
    if (arg[0] == '-') {
      if (strcmp(arg, "-cp") == 0) {
        continue;
      }
      if (strcmp(arg, "-classpath") == 0) {
        continue;
      }
      if (xoptions[0] != 0) {
        strcat(xoptions, " ");
      }
      strcat(xoptions, arg);
      continue;
    }
    if (!have_classpath) {
      strcpy(classpath, arg);
      have_classpath = true;
    } else if (!have_mainclass) {
      strcpy(mainclass, arg);
      have_mainclass = true;
      break;
    }
  }
  if (!have_classpath || !have_mainclass) {
    printf("Usage : jfexec [-cp] CLASSPATH MAINCLASS ...\n");
    return false;
  }
#ifdef _JF_SERVICE
  strcpy(method, "serviceStart");
#else
  strcpy(method, "main");
#endif
  return true;
}
#else
bool loadProperties() {
  char app[MAX_PATH];
  char *data, *ln1, *ln2;
  int sl, fs;
  int res;
  struct Header header;

  javahome[0] = 0;
  xoptions[0] = 0;

#ifdef _JF_SERVICE
  strcpy(method, "serviceStart");
#else
  strcpy(method, "main");
#endif
  cfgargs[0] = 0;

  strcpy(app, resolvelink("/proc/self/exe"));

  int file = open(app, O_RDONLY);
  if (file == -1) {
    error("app.cfg not found");
    return false;
  }
  fs = lseek(file, 0, SEEK_END);
  lseek(file, fs-8, SEEK_SET);
  res = read(file, &header, 8);
  if (strncmp(header.name, ".cfg", 4)) {
    error("app.cfg not found");
    return false;
  }
  lseek(file, fs - 8 - header.size, SEEK_SET);
  data = (char*)malloc(size + 1);
  res = read(file, data, header.size);
  close(file);
  data[header.size] = 0;
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
    else if (strncmp(ln1, "JAVA_HOME=", 10) == 0) {
      strcpy(javahome, ln1 + 10);
    }
    else if (strncmp(ln1, "METHOD=", 7) == 0) {
      strcpy(method, ln1 + 7);
    }
    else if (strncmp(ln1, "ARGS=", 5) == 0) {
      strcpy(cfgargs, ln1 + 5);
    }
    else if (strncmp(ln1, "OPTIONS=", 8) == 0) {
      strcpy(xoptions, ln1 + 8);
    }
    else if (strncmp(ln1, "DEBUG=", 6) == 0) {
      debug = true;
    }
    ln1 = ln2;
  }
  free(data);
  return true;
}
#endif

char* strlwr(char* str) {
  for(int i = 0; str[i]; i++){
    str[i] = tolower(str[i]);
  }
  return str;
}

bool exists(char *file) {
  if (access(file, F_OK) == 0) return true;
  return false;
}

bool findJavaHomeEnvironment() {
  //try to find JRE in JAVA_HOME environment variable
  char* env_java_home;
  env_java_home = getenv("JAVA_HOME");
  if (env_java_home == NULL) return false;
  strcpy(javahome, env_java_home);
  int sl = strlen(javahome);
  strcat(javahome, "/lib/server/libjvm.so");
  if (exists(javahome)) {
    javahome[sl] = 0;
    return true;
  }
  return false;
}

bool findJavaHomeApp() {
  //try /usr/bin/java
  char *java_home = resolvelink("/usr/bin/java");
  if (java_home == NULL) {
    return false;
  }
  strcpy(javahome, java_home);
  //remove /bin/java from javahome
  char *_java = strrchr(javahome, '/');
  *_java = 0;
  char *_bin = strrchr(javahome, '/');
  *_bin = 0;
  int sl = strlen(javahome);
  strcat(javahome, "/lib/server/libjvm.so");
  if (exists(javahome)) {
    javahome[sl] = 0;
    return true;
  }
  return false;
}

bool try_graal() {
  strcpy(dll, "/usr/lib/");
  strcat(dll, mainclass);
  strcat(dll, ".so");
  strlwr(dll);
  jvm_dll = dlopen(dll, RTLD_NOW);
  return jvm_dll == NULL ? false : true;
}

bool try_jvm() {
  if (javahome[0] == 0) {
    if (findJavaHomeEnvironment() == 0) {
      if (findJavaHomeApp() == 0) {
        error("Unable to find java");
        return false;
      }
    }
  }

  strcat(javahome, "/lib");

  //open libjvm.so
  strcpy(dll, javahome);
  strcat(dll, "/server/libjvm.so");

  jvm_dll = dlopen(dll, RTLD_NOW);
  if (jvm_dll == NULL) {
    error("Unable to open libjvm.so");
  }

  //open libjawt.so (otherwise Linux can't find it later)
  strcpy(dll, javahome);
  strcat(dll, "/libjawt.so");

  jawt_dll = dlopen(dll, RTLD_NOW);
  if (jawt_dll == NULL) {
    //if this fails most likely X11 libraries are not installed so user is most likely not running AWT app anyways
    printf("Warning:Unable to open libjawt.so\n");
  }
  return true;
}

void replace(char *str, char find, char with) {
  while (*str) {
    if (*str == find) *str = with;
    str++;
  }
}

/** Main entry point. */
int main(int argc, char **argv) {
  void *retval;
  g_argv = argv;
  g_argc = argc;

  loadProperties();

  replace(mainclass, '/', '.');

  if (try_graal()) {
    graal = true;
  } else {
    if (!try_jvm()) {
      return 2;
    }
  }

  CreateJavaVM = (int (*)(void*,void*,void*)) dlsym(jvm_dll, "JNI_CreateJavaVM");
  if (CreateJavaVM == NULL) {
    error("Unable to find Java interfaces in libjvm.so");
  }

  //now continue in new thread (not really necessary but avoids some Java bugs)
  pthread_attr_init(&thread_attr);

  pthread_create(&thread, &thread_attr, (void *(*) (void *))&JavaThread, NULL);

  pthread_join(thread, &retval);

  return 0;
}
