//Java Launcher Linux

// version 1.8
// supports passing command line options to java main()
// now loads CLASSPATH and MAINCLASS from embedded resource file (*.cfg)
// now globbs arguments (see ExpandStringArray())

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

/** Create class path adding /usr/share/java to each element, and change ; to : */
char *CreateClassPath() {
  char *ClassPath;
  int sl = strlen(classpath);
  ClassPath = malloc(sl + 1);
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
  char *ExpandedClassPath = malloc(len);
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

char* DetectGC() {
  //ZGC is always available under JDK15+
  return (char*)"-XX:+UseZGC";
}

JavaVMInitArgs *BuildArgs() {
  JavaVMInitArgs *args;
  JavaVMOption *options;
  int nOpts = 0;
  char *opts[64];
  int idx;

  opts[nOpts++] = CreateClassPath();
  opts[nOpts++] = "-Djava.app.home=/usr/bin";
  opts[nOpts++] = DetectGC();
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

  args = malloc(sizeof(JavaVMInitArgs));
  memset(args, 0, sizeof(JavaVMInitArgs));
  options = malloc(sizeof(JavaVMOption) * nOpts);
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

void printException(JNIEnv *env) {
  jthrowable exc;
  exc = (*env)->ExceptionOccurred(env);
  if (exc == NULL) return;
  jclass newExcCls;
  (*env)->ExceptionDescribe(env);
  (*env)->ExceptionClear(env);
}

void convertClass(char *cls) {
  while (*cls) {
    if (*cls == '.') *cls = '/';
    cls++;
  }
}

/** Continues loading the JVM in a new Thread. */
int JavaThread(void *ignore) {
  JavaVM *jvm = NULL;
  JNIEnv *env = NULL;

  if ((*CreateJavaVM)(&jvm, &env, BuildArgs()) == -1) {
    error("Unable to create Java VM");
    return -1;
  }

  convertClass(mainclass);
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

  xoptions[0] = 0;

  strcpy(method, "main");
  cfgargs[0] = 0;

  strcpy(app, resolvelink("/proc/self/exe"));

  int file = open(app, O_RDONLY);
  if (file == -1) {
    error("app.cfg not found");
    return -1;
  }
  fs = lseek(file, 0, SEEK_END);
  lseek(file, fs-8, SEEK_SET);
  read(file, &header, 8);
  if (strncmp(header.name, ".cfg", 4)) {
    error("app.cfg not found");
    return -1;
  }
  lseek(file, fs - 8 - header.size, SEEK_SET);
  data = (char*)malloc(size + 1);
  read(file, data, fs);
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
    else if (strncmp(ln1, "METHOD=", 7) == 0) {
      strcpy(method, ln1 + 7);
    }
    else if (strncmp(ln1, "ARGS=", 5) == 0) {
      strcpy(cfgargs, ln1 + 5);
    }
    else if (strncmp(ln1, "OPTIONS=", 8) == 0) {
      strcpy(xoptions, ln1 + 8);
    }
    ln1 = ln2;
  }
  free(data);
  return 0;
}

/** Main entry point. */
int main(int argc, char **argv) {
  void *retval;
  g_argv = argv;
  g_argc = argc;

  loadProperties();

  //get java home
  strcpy(javahome, resolvelink("/usr/bin/java"));

  //remove /bin/java from javahome
  char *_java = strrchr(javahome, '/');
  *_java = 0;
  char *_bin = strrchr(javahome, '/');
  *_bin = 0;
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
    error("Unable to open libjawt.so");
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
