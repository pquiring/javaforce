//Java Launcher Win32/64

// - supports passing command line options to java main()
// - loads CLASSPATH and MAINCLASS from PE-EXE resource
// - globbs arguments (see ExpandStringArray())
// - supports console apps (type "console")
// - supports windows services (type "service")
// - define java.app.home to find exe/dll files
// - support graal
// - native functions are now included in executable

#include <windows.h>
#include <io.h>
#include <process.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <ctype.h>
#include <stddef.h>

#include <jni.h>

#include "../../native/headers/javaforce_controls_ni_DAQmx.h"
#include "../../native/headers/javaforce_gl_GL.h"
#include "../../native/headers/javaforce_jni_JFNative.h"
#include "../../native/headers/javaforce_jni_WinNative.h"
#include "../../native/headers/javaforce_media_Camera.h"
#include "../../native/headers/javaforce_media_MediaCoder.h"
#include "../../native/headers/javaforce_media_MediaDecoder.h"
#include "../../native/headers/javaforce_media_MediaEncoder.h"
#include "../../native/headers/javaforce_media_MediaVideoDecoder.h"
#include "../../native/headers/javaforce_media_VideoBuffer.h"
#include "../../native/headers/javaforce_ui_Font.h"
#include "../../native/headers/javaforce_ui_Image.h"
#include "../../native/headers/javaforce_ui_Window.h"
#include "../../native/headers/javaforce_net_PacketCapture.h"

/* Global variables */
HKEY key, subkey;
int type;
char version[MAX_PATH];
char javahome[MAX_PATH];
char expanded[MAX_PATH];
char dll[MAX_PATH];
int size = MAX_PATH;
HMODULE jvm_dll;
int (*CreateJavaVM)(JavaVM**,void**,void*);
HANDLE thread_handle;
int thread_id;
STARTUPINFO si;
PROCESS_INFORMATION pi;
char **g_argv;
int g_argc;
char module[MAX_PATH];
char exepath[MAX_PATH];
char classpath[1024];
char mainclass[MAX_PATH];
char method[MAX_PATH];
char xoptions[MAX_PATH];
#ifdef _JF_SERVICE
char service[MAX_PATH];
#endif
char err_msg[1024];
JavaVM *g_jvm = NULL;
JNIEnv *g_env = NULL;
bool graal = false;

/* Prototypes */
void error(char *msg);
bool JavaThread(void *ignore);
bool loadProperties();

/** Displays the error message in a dialog box. */
void error(char *msg) {
  char fullmsg[2048];
  sprintf(fullmsg, "Failed to start Java\nError:%s", msg);
#ifndef _JF_SERVICE
  MessageBox(NULL, fullmsg, "Application Loader", (MB_OK | MB_ICONSTOP | MB_APPLMODAL));
#else
  printf("%s", fullmsg);
#endif
}

void printLastError() {
  printf("Last Error=%x\n", GetLastError());
}

void printException(JNIEnv *env) {
  jthrowable exc;
  exc = env->ExceptionOccurred();
  if (exc == NULL) return;
  jclass newExcCls;
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
  jclass cls;
  jmethodID mid;
  jobject outArray;

  cls = env->FindClass("javaforce/JF");
  if (cls == NULL) {
    printException(g_env);
    error("Unable to find javaforce.JF class");
    return NULL;
  }
  mid = env->GetStaticMethodID(cls, "expandArgs", "([Ljava/lang/String;)[Ljava/lang/String;");
  if (mid == NULL) {
    printException(g_env);
    error("Unable to find javaforce.JF.expandArgs method");
    return NULL;
  }
  outArray = env->CallStaticObjectMethod(cls, mid, inArray);
  env->DeleteLocalRef(inArray);
  return outArray;
}

char *DOption = "-Djava.class.path=";

#ifdef _JF_CLI
/** Create class path "as is" which should include jar files from current path. */
char *CreateClassPath() {
  int len = strlen(DOption) + strlen(classpath) + 1;
  char *ExpandedClassPath = (char*)malloc(len);
  ExpandedClassPath[0] = 0;
  strcat(ExpandedClassPath, DOption);
  strcat(ExpandedClassPath, classpath);
  return ExpandedClassPath;
}
#else
/** Create class path adding exe path to each element (because the current path is not where the EXE is). */
char *CreateClassPath() {
  char *ClassPath;
  int sl = strlen(classpath);
  ClassPath = (char*)malloc(sl + 1);
  strcpy(ClassPath, classpath);
  int ml = strlen(exepath) + 1;
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
  char env_classpath[1024];
  int env_classpath_len = GetEnvironmentVariable("CLASSPATH", env_classpath, 1024);
  if (env_classpath_len > 0) {
    env_classpath[env_classpath_len] = 0;
    len += env_classpath_len + 1;
  }
  char *ExpandedClassPath = (char*)malloc(len);
  ExpandedClassPath[0] = 0;
  strcat(ExpandedClassPath, DOption);
  for(a=0;a<cnt;a++) {
    if (a > 0) strcat(ExpandedClassPath, ";");
    if (strchr(jar[a], '/') == NULL && strchr(jar[a], '\\') == NULL) {
      strcat(ExpandedClassPath, exepath);
      strcat(ExpandedClassPath, "\\");
    }
    strcat(ExpandedClassPath, jar[a]);
  }
  if (env_classpath_len > 0) {
    strcat(ExpandedClassPath, ";");
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

bool InvokeMethod(char *_method, jobject args, char *sign) {
  convertClass(mainclass);
  jclass cls = g_env->FindClass(mainclass);
  if (cls == NULL) {
    printException(g_env);
    error("Unable to find main class");
    return false;
  }
  jmethodID mid = g_env->GetStaticMethodID(cls, _method, sign);
  if (mid == NULL) {
    printException(g_env);
    error("Unable to find main method");
    return false;
  }

  g_env->CallStaticVoidMethod(cls, mid, args);

  return true;
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
  int idx;
  char *opts[64];

#ifdef _JF_DEBUG
  opts[nOpts++] = "-Djava.debug=true";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.port=9010";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.local.only=false";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.authenticate=false";
  opts[nOpts++] = "-Dcom.sun.management.jmxremote.ssl=false";
#endif
  opts[nOpts++] = MakeString("-Djava.app.home=%s", exepath);
  if (graal) {
    opts[nOpts++] = (char*)"-Djava.graal=true";
    opts[nOpts++] = MakeString("-Djava.home=%s", exepath);
  } else {
    opts[nOpts++] = CreateClassPath();
  }
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

/** Attachs current thread to JVM. */
void AttachJVM() {
  g_jvm->AttachCurrentThread((void**)&g_env, NULL);
}

#include "../common/register.cpp"

//Windows native methods
static JNINativeMethod javaforce_jni_WinNative[] = {
  {"winInit", "()Z", (void *)&Java_javaforce_jni_WinNative_winInit},
  {"comOpen", "(Ljava/lang/String;I)J", (void *)&Java_javaforce_jni_WinNative_comOpen},
  {"comClose", "(J)V", (void *)&Java_javaforce_jni_WinNative_comClose},
  {"comRead", "(J[B)I", (void *)&Java_javaforce_jni_WinNative_comRead},
  {"comWrite", "(J[B)I", (void *)&Java_javaforce_jni_WinNative_comWrite},
  {"getWindowRect", "(Ljava/lang/String;[I)Z", (void *)&Java_javaforce_jni_WinNative_getWindowRect},
  {"peBegin", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_peBegin},
  {"peAddIcon", "(J[B)V", (void *)&Java_javaforce_jni_WinNative_peAddIcon},
  {"peAddString", "(JII[B)V", (void *)&Java_javaforce_jni_WinNative_peAddString},
  {"peEnd", "(J)V", (void *)&Java_javaforce_jni_WinNative_peEnd},
  {"impersonateUser", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_impersonateUser},
  {"findJDKHome", "()Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_findJDKHome},
  {"enableConsoleMode", "()V", (void *)&Java_javaforce_jni_WinNative_enableConsoleMode},
  {"disableConsoleMode", "()V", (void *)&Java_javaforce_jni_WinNative_disableConsoleMode},
  {"getConsoleSize", "()[I", (void *)&Java_javaforce_jni_WinNative_getConsoleSize},
  {"getConsolePos", "()[I", (void *)&Java_javaforce_jni_WinNative_getConsolePos},
  {"readConsole", "()C", (void *)&Java_javaforce_jni_WinNative_readConsole},
  {"peekConsole", "()Z", (void *)&Java_javaforce_jni_WinNative_peekConsole},
  {"writeConsole", "(I)V", (void *)&Java_javaforce_jni_WinNative_writeConsole},
  {"writeConsoleArray", "([BII)V", (void *)&Java_javaforce_jni_WinNative_writeConsoleArray},
  {"tapeOpen", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_tapeOpen},
  {"tapeClose", "(J)V", (void *)&Java_javaforce_jni_WinNative_tapeClose},
  {"tapeFormat", "(JI)Z", (void *)&Java_javaforce_jni_WinNative_tapeFormat},
  {"tapeRead", "(J[BII)I", (void *)&Java_javaforce_jni_WinNative_tapeRead},
  {"tapeWrite", "(J[BII)I", (void *)&Java_javaforce_jni_WinNative_tapeWrite},
  {"tapeSetpos", "(JJ)Z", (void *)&Java_javaforce_jni_WinNative_tapeSetpos},
  {"tapeGetpos", "(J)J", (void *)&Java_javaforce_jni_WinNative_tapeGetpos},
  {"tapeMedia", "(J)Z", (void *)&Java_javaforce_jni_WinNative_tapeMedia},
  {"tapeMediaSize", "()J", (void *)&Java_javaforce_jni_WinNative_tapeMediaSize},
  {"tapeMediaBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeMediaBlockSize},
  {"tapeMediaReadOnly", "()Z", (void *)&Java_javaforce_jni_WinNative_tapeMediaReadOnly},
  {"tapeDrive", "(J)Z", (void *)&Java_javaforce_jni_WinNative_tapeDrive},
  {"tapeDriveMinBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeDriveMinBlockSize},
  {"tapeDriveMaxBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeDriveMaxBlockSize},
  {"tapeDriveDefaultBlockSize", "()I", (void *)&Java_javaforce_jni_WinNative_tapeDriveDefaultBlockSize},
  {"tapeLastError", "()I", (void *)&Java_javaforce_jni_WinNative_tapeLastError},
  {"changerOpen", "(Ljava/lang/String;)J", (void *)&Java_javaforce_jni_WinNative_changerOpen},
  {"changerClose", "(J)V", (void *)&Java_javaforce_jni_WinNative_changerClose},
  {"changerList", "(J)[Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_changerList},
  {"changerMove", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_changerMove},
//vss
  {"vssInit", "()Z", (void *)&Java_javaforce_jni_WinNative_vssInit},
  {"vssListVols", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_vssListVols},
  {"vssListShadows", "()[[Ljava/lang/String;", (void *)&Java_javaforce_jni_WinNative_vssListShadows},
  {"vssCreateShadow", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssCreateShadow},
  {"vssDeleteShadow", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssDeleteShadow},
  {"vssDeleteShadowAll", "()Z", (void *)&Java_javaforce_jni_WinNative_vssDeleteShadowAll},
  {"vssMountShadow", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssMountShadow},
  {"vssUnmountShadow", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_WinNative_vssUnmountShadow},
//test
  {"add", "(II)I", (void *)&Java_javaforce_jni_WinNative_add},
  {"hold", "([II)V", (void *)&Java_javaforce_jni_WinNative_hold},
};

/** Register natives embedded with executable. */
void registerAllNatives(JNIEnv *env) {
  jclass cls;

  registerCommonNatives(env);

  cls = findClass(env, "javaforce/jni/WinNative");
  registerNatives(env, cls, javaforce_jni_WinNative, sizeof(javaforce_jni_WinNative)/sizeof(JNINativeMethod));
}

/** Invokes the main method in a new thread. */
bool JavaThread(void *ignore) {
  CreateJVM();

  registerAllNatives(g_env);

  g_env->FindClass("javaforce/jni/Startup");

  char **argv = g_argv;
  int argc = g_argc;
  //skip argv[0]
  argv++;
  argc--;
  InvokeMethod(method, ExpandStringArray(g_env, ConvertStringArray(g_env, argc, argv)), "([Ljava/lang/String;)V");

  g_jvm->DestroyJavaVM();  //waits till all threads are complete

  return true;
}

#ifdef _JF_CLI
//load properties from command line
bool loadProperties() {
  //jfexec [-cp] CLASSPATH MAINCLASS
  bool have_classpath = false;
  bool have_mainclass = false;
  char** argv = g_argv;
  int argc = g_argc;
  for(int a=1;a<argc;a++) {
    //skip arg
    g_argv++;
    g_argc--;
    char* arg = argv[a];
    if (arg[0] == 0) continue;
    if (arg[0] == '-') continue;  //TODO : support -D, etc.
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
    printf("Usage : jfexec [-cp] CLASSPATH MAINCLASS ...\r\n");
    return false;
  }
#ifdef _JF_CLI_SERVICE
  strcpy(method, "serviceStart");  //default method name
#else
  strcpy(method, "main");  //default method name
#endif
  javahome[0] = 0;  //detect later
  xoptions[0] = 0;
  return true;
}
#else
//load properties from executable
bool loadProperties() {
  void *data;
  char *str, *ln1, *ln2;
  HRSRC res;
  HGLOBAL global;
  int size;

  xoptions[0] = 0;

#ifdef _JF_CLI_SERVICE
  strcpy(method, "serviceStart");  //default method name
#else
  strcpy(method, "main");  //default method name
#endif
  javahome[0] = 0;  //detect later

  res = FindResource(NULL, MAKEINTRESOURCE(1), RT_RCDATA);
  if (res == NULL) {error("Unable to FindResource"); return false;}
  size = SizeofResource(NULL, res);
  global = LoadResource(NULL, res);
  if (global == NULL) {error("Unable to LoadResource"); return false;}
  data = LockResource(global);
  if (data == NULL) {error("Unable to LockResource"); return false;}
  str = (char*)malloc(size+1);
  memcpy(str, data, size);
  str[size] = 0;  //NULL terminate
  FreeResource(global);

  ln1 = str;
  classpath[0] = 0;
  mainclass[0] = 0;
  while (ln1 != NULL) {
    ln2 = strstr(ln1, "\r\n");
    if (ln2 != NULL) {
      *ln2 = 0;
      ln2++;
//      *ln2 = 0;
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
    else if (strncmp(ln1, "OPTIONS=", 8) == 0) {
      strcpy(xoptions, ln1 + 8);
    }
#ifdef _JF_SERVICE
    else if (strncmp(ln1, "SERVICE=", 8) == 0) {
      strcpy(service, ln1 + 8);
    }
#else
    else if (strncmp(ln1, "METHOD=", 7) == 0) {
      strcpy(method, ln1 + 7);
    }
#endif
    ln1 = ln2;
  }
  free(str);
  return true;
}
#endif

bool exists(char *file) {
  if (GetFileAttributes(file) == INVALID_FILE_ATTRIBUTES) return false;
  return true;
}

bool findJavaHomeEnvironment() {
  //try to find JRE in JAVA_HOME environment variable
  char env_java_home[1024];
  int env_java_home_len = GetEnvironmentVariable("JAVA_HOME", env_java_home, 1024);
  if (env_java_home_len == 0) return false;
  env_java_home[env_java_home_len] = 0;
  strcpy(javahome, env_java_home);
  int sl = strlen(javahome);
  strcat(javahome, "\\bin\\server\\jvm.dll");
  if (exists(javahome)) {
    javahome[sl] = 0;
    return true;
  }
  return false;
}

bool findJavaHomeAppFolder() {
  //try to find JRE in Apps folder
  strcpy(javahome, exepath);
  strcat(javahome, "\\jre");
  int sl = strlen(javahome);
  strcat(javahome, "\\bin\\server\\jvm.dll");
  if (exists(javahome)) {
    javahome[sl] = 0;
    return true;
  }
  return false;
}

bool findJavaHomeAppDataFolder() {
  //try to find JRE in %AppData% folder
  int javahome_len = GetEnvironmentVariable("APPDATA", javahome, MAX_PATH);
  if (javahome_len == 0) return false;
  strcat(javahome, "\\java\\jre");
  if (sizeof(void*) == 4)
    strcat(javahome, "32");
  else
    strcat(javahome, "64");
  int sl = strlen(javahome);
  strcat(javahome, "\\bin\\server\\jvm.dll");
  if (exists(javahome)) {
    javahome[sl] = 0;
    return true;
  }
  return false;
}

bool findJavaHomeRegistry() {
  //try to find JRE in Registry
  if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, "Software\\JavaSoft\\JRE", 0, KEY_READ, &key) != 0) {
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, "Software\\JavaSoft\\Java Runtime Environment", 0, KEY_READ, &key) != 0) {
      if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, "Software\\JavaSoft\\JDK", 0, KEY_READ, &key) != 0) {
        sprintf(err_msg, "Unable to open JavaSoft registry key");
        return false;
      }
    }
  }

  size = 0;
  if (RegQueryValueEx(key, "CurrentVersion", 0, (LPDWORD)&type, 0, (LPDWORD)&size) != 0 || (type != REG_SZ) || (size > MAX_PATH)) {
    sprintf(err_msg, "Unable to find CurrentVersion registry key");
    return false;
  }

  size = MAX_PATH;
  if (RegQueryValueEx(key, "CurrentVersion", 0, 0, (LPBYTE)version, (LPDWORD)&size) != 0) {
    sprintf(err_msg, "Unable to load CurrentVersion registry key");
    return false;
  }

  if (RegOpenKeyEx(key, version, 0, KEY_READ, &subkey) != 0) {
    sprintf(err_msg, "Unable to open CurrentVersion registry key");
    return false;
  }

  size = 0;
  if (RegQueryValueEx(subkey, "JavaHome", 0, (LPDWORD)&type, 0, (LPDWORD)&size) != 0 || (type != REG_SZ) || (size > MAX_PATH)) {
    sprintf(err_msg, "Unable to find JavaHome registry key");
    return false;
  }

  size = MAX_PATH;
  if (RegQueryValueEx(subkey, "JavaHome", 0, 0, (LPBYTE)javahome, (LPDWORD)&size) != 0) {
    sprintf(err_msg, "Unable to load JavaHome registry key");
    return false;
  }

  RegCloseKey(key);
  RegCloseKey(subkey);
  return true;
}

#ifdef _JF_SERVICE

SERVICE_STATUS_HANDLE ServiceHandle;
int s_argc;
char **s_argv;

void ServiceStatus(int state) {
  SERVICE_STATUS ss;

  ss.dwServiceType = SERVICE_WIN32;
  ss.dwWin32ExitCode = 0;
  ss.dwCurrentState = state;
  ss.dwControlsAccepted = SERVICE_ACCEPT_STOP;
  ss.dwWin32ExitCode = 0;
  ss.dwServiceSpecificExitCode = 0;
  ss.dwCheckPoint = 0;
  ss.dwWaitHint = 0;

  SetServiceStatus(ServiceHandle, &ss);
}

void __stdcall ServiceControl(int OpCode) {
  switch (OpCode) {
    case SERVICE_CONTROL_STOP:
      AttachJVM();
      ServiceStatus(SERVICE_STOPPED);
      InvokeMethod("serviceStop", NULL, "()V");
      break;
  }
}

void __stdcall ServiceMain(int argc, char **argv) {
  ServiceHandle = RegisterServiceCtrlHandler(service, (void (__stdcall *)(unsigned long))ServiceControl);
  ServiceStatus(SERVICE_RUNNING);
  CreateJVM();
  registerAllNatives(g_env);
  g_env->FindClass("javaforce/jni/Startup");
  InvokeMethod("serviceStart", ConvertStringArray(g_env, argc, argv), "([Ljava/lang/String;)V");
  g_jvm->DestroyJavaVM();
}

#endif

bool try_graal() {
  strcpy(dll, exepath);
  strcat(dll, "\\");
  strcat(dll, mainclass);
  strcat(dll, ".dll");
  jvm_dll = LoadLibrary(dll);
  return jvm_dll == NULL ? false : true;
}

bool try_jvm() {
  sprintf(err_msg, "Unable to find Java");
  if (javahome[0] == 0) {
    if (findJavaHomeEnvironment() == 0) {
      if (findJavaHomeRegistry() == 0) {
        if (findJavaHomeAppFolder() == 0) {
          if (findJavaHomeAppDataFolder() == 0) {
            error(err_msg);
            return false;
          }
        }
      }
    }
  }

  if (strchr(javahome, '%') != NULL) {
    //expand environment strings
    ExpandEnvironmentStrings(javahome, expanded, MAX_PATH);
    strcpy(javahome, expanded);
  }

#if 0
  //this is causing other file related errors in apps
  strcpy(dll, javahome);
  strcat(dll, "\\bin");
  SetDllDirectory(dll);
#endif

  strcpy(dll, javahome);
  strcat(dll, "\\bin\\server\\jvm.dll");
  if ((jvm_dll = LoadLibrary(dll)) == NULL) {
    error("Unable to open jvm.dll");
    return false;
  }
  return true;
}

void trim(char *str, char ch) {
  char *lastchar = strrchr(str, ch);
  if (lastchar != NULL) {
    *lastchar = 0;
  }
}

/** Main entry point. */
int main(int argc, char **argv)
{
  g_argv = argv;
  g_argc = argc;

  GetModuleFileName(NULL, module, MAX_PATH);
  trim(module, '.');
  strcpy(exepath, module);
  trim(exepath, '\\');

  if (!loadProperties()) {
    return 2;
  }

  if (try_graal()) {
    graal = true;
  } else {
    if (!try_jvm()) {
      return 2;
    }
  }

  CreateJavaVM = (int (*)(JavaVM**,void**,void*)) GetProcAddress(jvm_dll, "JNI_CreateJavaVM");
  if (CreateJavaVM == NULL) {
    error("Unable to find Java interfaces in jvm.dll");
    return 2;
  }

#ifdef _JF_SERVICE
  void *ServiceTable[4];
  ServiceTable[0] = (void*)service;
  ServiceTable[1] = (void*)ServiceMain;
  ServiceTable[2] = NULL;
  ServiceTable[3] = NULL;
  StartServiceCtrlDispatcher((LPSERVICE_TABLE_ENTRY)&ServiceTable);  //does not return until all services have been stopped
#else
  thread_handle = CreateThread(NULL, 64 * 1024, (LPTHREAD_START_ROUTINE)&JavaThread, NULL, 0, (LPDWORD)&thread_id);
  WaitForSingleObject(thread_handle, INFINITE);
#endif

  return 0;
}
