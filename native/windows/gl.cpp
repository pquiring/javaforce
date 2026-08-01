//Windows OpenGL

HMODULE wgl = NULL;

jboolean glGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)wglGetProcAddress(name);  //get OpenGL 1.x function
  if (func == NULL) {
    func = (void*)GetProcAddress(wgl, name);  //get OpenGL 2.0+ function
  }
  if (func != NULL) {
    *funcPtr = func;
    return JNI_TRUE;
  } else {
    printf("OpenGL:Error:Can not find function:%s\n", name);
    return JNI_FALSE;
  }
}

JNIEXPORT jboolean JNICALL GLinit(const char* libgl_so)
{
  //libgl_so is ignored on Windows
  if (wgl == NULL) {
    wgl = LoadLibrary("opengl32.dll");
    if (wgl == NULL) {
      printf("LoadLibrary(opengl32.dll) failed\n");
      return JNI_FALSE;
    }
  }
  GL_get_functions();
  return TRUE;
}
