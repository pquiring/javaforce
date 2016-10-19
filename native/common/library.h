#ifdef __WIN32__
  #define LIB_HANDLE HMODULE
  #define LIB_OPEN LoadLibrary
  #define LIB_OPTS
  #define LIB_GET_FUNC GetProcAddress
#else
  #define LIB_HANDLE void*
  #define LIB_OPEN dlopen
  #define LIB_OPTS ,RTLD_LAZY|RTLD_GLOBAL
  #define LIB_GET_FUNC dlsym
#endif

static void getFunction(LIB_HANDLE handle, void **funcPtr, const char *name) {
  void *func = (void*)LIB_GET_FUNC(handle, name);
  if (func == NULL) {
    printf("Error:Can not find function:%s\n", name);
  } else {
    *funcPtr = func;
  }
}
