//Linux Vulkan

void* vk = NULL;

jboolean vkGetFunction(void **funcPtr, const char *name)
{
  void *func;
  func = (void*)dlsym(vk, name);
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
  if (vk == NULL && libvulkan_so != NULL) {
    vk = dlopen(libvulkan_so, RTLD_LAZY | RTLD_GLOBAL);
    if (vk == NULL) {
      printf("Warning:dlopen(libvulkan.so) unsuccessful\n");
    }
  }
  VK_get_functions();
  return TRUE;
}
