#ifdef _WIN32
  #define JF_LIB_HANDLE HMODULE
#else
  #define JF_LIB_HANDLE void*
#endif

static JF_LIB_HANDLE loadLibrary(const char* name) {
#ifdef _WIN32
  return LoadLibrary(name);
#else
  return dlopen(name,RTLD_LAZY|RTLD_GLOBAL);
#endif
}

static void getFunction(JF_LIB_HANDLE handle, void **funcPtr, const char *name) {
  void *func;
#ifdef _WIN32
  func = (void*)GetProcAddress(handle, name);
#else
  func = (void*)dlsym(handle, name);
#endif
  if (func == NULL) {
    printf("Error:Can not find function:%s\n", name);
  } else {
    *funcPtr = func;
  }
}
