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

#include "javaforce_controls_ni_DAQmx.h"
#include "javaforce_gl_GL.h"
#include "javaforce_jni_WinNative.h"
#include "javaforce_media_Camera.h"
#include "javaforce_media_MediaCoder.h"
#include "javaforce_media_MediaDecoder.h"
#include "javaforce_media_MediaEncoder.h"
#include "javaforce_media_MediaVideoDecoder.h"
#include "javaforce_media_VideoBuffer.h"
#include "javaforce_ui_Font.h"
#include "javaforce_ui_Image.h"
#include "javaforce_ui_Window.h"
#include "javaforce_net_PacketCapture.h"
#include "javaforce_cl_CL.h"

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
bool debug = false;
char errmsg[1024];

/* Prototypes */
void error(char *msg);
bool JavaThread(void *ignore);
bool loadProperties();
bool InvokeMethodVoid(char *_class, char *_method, char *sign, jobject args);
jobject InvokeMethodObject(char *_class, char *_method, char *sign, jobject args);

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
  int idx;
  char *opts[64];

#ifdef DEBUG
  debug = true;
#endif

  if (debug) {
// Warning : this will cause the app not to load if MSVCRT is not installed at the system level - the one included in the MSI is not found by jmx
    opts[nOpts++] = "-Djava.debug=true";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.port=9010";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.local.only=false";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.authenticate=false";
    opts[nOpts++] = "-Dcom.sun.management.jmxremote.ssl=false";
  }
  opts[nOpts++] = MakeString("-Djava.app.home=%s", exepath);
  opts[nOpts++] = MakeString("-Djava.app.name=%s", mainclass);
  opts[nOpts++] = MakeString("-Dvisualvm.display.name=%s", mainclass);
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

extern "C" void winnative_register(JNIEnv *env);

/** Register natives embedded with executable. */
void registerAllNatives(JNIEnv *env) {
  jclass cls;

  registerCommonNatives(env);

  winnative_register(env);
}

/** Invokes the main method in a new thread. */
bool JavaThread(void *ignore) {
  CreateJVM();

  registerAllNatives(g_env);

  char **argv = g_argv;
  int argc = g_argc;
  //skip argv[0]
  argv++;
  argc--;
  InvokeMethodVoid(mainclass, method, "([Ljava/lang/String;)V", ExpandStringArray(g_env, ConvertStringArray(g_env, argc, argv)));

  g_jvm->DestroyJavaVM();  //waits till all threads are complete

  return true;
}

#ifdef _JF_CLI
//load properties from command line
bool loadProperties() {
  //jfexec [-cp] CLASSPATH MAINCLASS
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
      if (strcmp(arg, "-vm") == 0) {
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
    printf("Usage : jfexec [-cp] CLASSPATH MAINCLASS ...\r\n");
    return false;
  }
#ifdef _JF_CLI_SERVICE
  strcpy(method, "serviceStart");  //default method name
#else
  strcpy(method, "main");  //default method name
#endif
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
    else if (strncmp(ln1, "DEBUG=", 6) == 0) {
      debug = true;
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
  ss.dwControlsAccepted = SERVICE_ACCEPT_STOP | SERVICE_ACCEPT_SESSIONCHANGE;
  ss.dwWin32ExitCode = 0;
  ss.dwServiceSpecificExitCode = 0;
  ss.dwCheckPoint = 0;
  ss.dwWaitHint = 0;

  SetServiceStatus(ServiceHandle, &ss);
}

void __stdcall ServiceControlEx(DWORD dwControl, DWORD dwEventType, LPVOID lpEventData, LPVOID lpContext) {
  switch (dwControl) {
    case SERVICE_CONTROL_STOP:
      AttachJVM();
      ServiceStatus(SERVICE_STOPPED);
      InvokeMethodVoid(mainclass, "serviceStop", "()V", NULL);
      break;
    case SERVICE_CONTROL_SESSIONCHANGE:
      break;
  }
}

void __stdcall ServiceMain(int argc, char **argv) {
  ServiceHandle = RegisterServiceCtrlHandlerEx(service, (LPHANDLER_FUNCTION_EX)ServiceControlEx, NULL);
  ServiceStatus(SERVICE_RUNNING);
  CreateJVM();
  registerAllNatives(g_env);
  InvokeMethodVoid(mainclass, "serviceStart", "([Ljava/lang/String;)V", ConvertStringArray(g_env, argc, argv));
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
    //use app supplied java before system supplied
    if (findJavaHomeAppFolder() == 0) {
      if (findJavaHomeAppDataFolder() == 0) {
        if (findJavaHomeEnvironment() == 0) {
          if (findJavaHomeRegistry() == 0) {
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

  //add $JAVA_HOME/bin to DLL search path (this is required to find MSVCRT DLLs)
  strcpy(dll, javahome);
  strcat(dll, "\\bin");
  SetDllDirectory(dll);

  strcpy(dll, javahome);
  strcat(dll, "\\bin\\server\\jvm.dll");
  if ((jvm_dll = LoadLibrary(dll)) == NULL) {
    error("Unable to open jvm.dll");
    return false;
  }

  //restore default DLL search path (without default some apps have file related issues)
  SetDllDirectory(NULL);

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
