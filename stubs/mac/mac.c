//Java Launcher Mac

// version 1.5
// supports passing command line options to java main()
// now loads CLASSPATH and MAINCLASS from resource file (*.cfg)
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
#include <dirent.h>

#ifndef MAX_PATH
  #define MAX_PATH 255
#endif

#include <jni.h>

/* Global variables */
int type;
char version[MAX_PATH];
char javahome[MAX_PATH];
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

/** Continues loading the JVM in a new Thread. */
int JavaThread(void *ignore) {
  JavaVM *jvm = NULL;
  JNIEnv *env = NULL;
  JavaVMInitArgs args;
  JavaVMOption options[1];

  memset(&args, 0, sizeof(args));
  args.version = JNI_VERSION_1_2;
  args.nOptions = 1;
  args.options = options;
  args.ignoreUnrecognized = JNI_FALSE;

  options[0].optionString = CreateClassPath();
  options[0].extraInfo = NULL;

  if ((*CreateJavaVM)(&jvm, &env, &args) == -1) {
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

  strcpy(app, g_argv[0]);
  strcat(app, ".cfg");

//  printf("cfg=%s\n", app);

  int file = open(app, O_RDONLY);
  if (file == -1) {
    error("app.cfg not found");
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

void findJava() {
  strcpy(javahome, "jre/lib/libjli.dylib");
}

/** Main entry point. */
int main(int argc, char **argv) {
  void *retval;
  g_argv = argv;
  g_argc = argc;

  loadProperties();

  //get java home
  findJava();

  //open libjli.dylib
  jvm_dll = dlopen(javahome, RTLD_NOW);
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

  pthread_join(thread, &retval);

  return 0;
}
