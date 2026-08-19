//Windows Vulkan

HMODULE vk = NULL;

jboolean vkGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)GetProcAddress(vk, name);
  if (func != NULL) {
    *funcPtr = func;
    return JNI_TRUE;
  } else {
    printf("Vulkan:Error:Can not find function:%s\n", name);
    return JNI_FALSE;
  }
}

JNIEXPORT jboolean JNICALL VKinit(const char* libvulkan_so)
{
  //libvulkan_so is ignored on Windows
  if (vk == NULL) {
    vk = LoadLibrary("vulkan-1.dll");
    if (vk == NULL) {
      printf("LoadLibrary(vulkan-1.dll) failed\n");
      return JNI_FALSE;
    }
  }
  VK_get_functions();
  return TRUE;
}
