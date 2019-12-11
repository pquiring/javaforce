#ifdef _WIN32
  #define JF_LIB_HANDLE HMODULE
  #define JF_LIB_OPEN LoadLibrary
  #define JF_LIB_OPTS
  #define JF_LIB_GET_FUNC GetProcAddress
#else
  #define JF_LIB_HANDLE void*
  #define JF_LIB_OPEN dlopen
  #define JF_LIB_OPTS ,RTLD_LAZY|RTLD_GLOBAL
  #define JF_LIB_GET_FUNC dlsym
#endif

static void getFunction(JF_LIB_HANDLE handle, void **funcPtr, const char *name) {
  void *func = (void*)JF_LIB_GET_FUNC(handle, name);
  if (func == NULL) {
    printf("Error:Can not find function:%s\n", name);
  } else {
    *funcPtr = func;
  }
}
