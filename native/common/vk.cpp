//Vulkan functions

jboolean vkGetFunction(void **funcPtr, const char *name);  //platform impl

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL VKinit(const char* libvulkan_so);

jboolean isVulkanSupported() {
  return glfwVulkanSupported();
}

//func args are not required
JNIEXPORT jboolean (*_isVulkanSupported)() = &isVulkanSupported;

JNIEXPORT void (*_vkCreateInstance)();
JNIEXPORT void (*_vkCreateRenderPass)();
JNIEXPORT void (*_vkCreateRenderPass2)();
JNIEXPORT void (*_vkCreateCommandPool)();
JNIEXPORT void (*_vkCreateSemaphore)();
JNIEXPORT void (*_vkCreateImageView)();
JNIEXPORT void (*_vkCreateFramebuffer)();
JNIEXPORT void (*_vkCreateFence)();
JNIEXPORT void (*_vkCreateDescriptorSetLayout)();
JNIEXPORT void (*_vkCreatePipelineLayout)();
JNIEXPORT void (*_vkCreateShaderModule)();
JNIEXPORT void (*_vkCreateGraphicsPipelines)();
JNIEXPORT void (*_vkCreateBuffer)();
JNIEXPORT void (*_vkCreateDescriptorPool)();
JNIEXPORT void (*_vkCreateSampler)();

JNIEXPORT void (*_vkDestroyDevice)();
JNIEXPORT void (*_vkDestroyFence)();
JNIEXPORT void (*_vkDestroyFramebuffer)();
JNIEXPORT void (*_vkDestroyImageView)();
JNIEXPORT void (*_vkDestroyShaderModule)();
JNIEXPORT void (*_vkDestroySemaphore)();
JNIEXPORT void (*_vkDestroyCommandPool)();
JNIEXPORT void (*_vkDestroyPipeline)();
JNIEXPORT void (*_vkDestroyPipelineLayout)();
JNIEXPORT void (*_vkDestroyRenderPass)();
JNIEXPORT void (*_vkDestroyInstance)();
JNIEXPORT void (*_vkDestroyBuffer)();
JNIEXPORT void (*_vkDestroyDescriptorSetLayout)();
JNIEXPORT void (*_vkDestroyDescriptorPool)();
JNIEXPORT void (*_vkDestroyImage)();
JNIEXPORT void (*_vkDestroySampler)();

JNIEXPORT void (*_vkEnumeratePhysicalDevices)();
JNIEXPORT void (*_vkEnumerateDeviceExtensionProperties)();
JNIEXPORT void (*_vkCreateDevice)();
JNIEXPORT void (*_vkGetPhysicalDeviceFeatures2)();
JNIEXPORT void (*_vkGetPhysicalDeviceProperties2)();
JNIEXPORT void (*_vkGetPhysicalDeviceMemoryProperties2)();
JNIEXPORT void (*_vkGetPhysicalDeviceQueueFamilyProperties2)();
JNIEXPORT void (*_vkGetDeviceQueue)();
JNIEXPORT void (*_vkGetDeviceQueue2)();
JNIEXPORT void (*_vkAllocateCommandBuffers)();
JNIEXPORT void (*_vkMapMemory)();
JNIEXPORT void (*_vkUnmapMemory)();
JNIEXPORT void (*_vkCreateImage)();
JNIEXPORT void (*_vkGetImageMemoryRequirements)();
JNIEXPORT void (*_vkAllocateMemory)();
JNIEXPORT void (*_vkBindImageMemory)();
JNIEXPORT void (*_vkQueueWaitIdle)();
JNIEXPORT void (*_vkResetCommandBuffer)();
JNIEXPORT void (*_vkFreeCommandBuffers)();
JNIEXPORT void (*_vkFreeMemory)();

//HKR extensions
JNIEXPORT void (*_vkCreateSwapchainKHR)();

JNIEXPORT void (*_vkDestroySwapchainKHR)();
JNIEXPORT void (*_vkDestroySurfaceKHR)();

JNIEXPORT void (*_vkGetPhysicalDeviceSurfaceFormats2KHR)();
JNIEXPORT void (*_vkGetPhysicalDeviceSurfaceCapabilitiesKHR)();
JNIEXPORT void (*_vkGetPhysicalDeviceSurfaceSupportKHR)();
JNIEXPORT void (*_vkGetPhysicalDeviceSurfacePresentModesKHR)();
JNIEXPORT void (*_vkGetPhysicalDeviceSurfaceFormatsKHR)();
JNIEXPORT void (*_vkGetPhysicalDeviceDisplayPropertiesKHR)();
JNIEXPORT void (*_vkGetPhysicalDeviceDisplayPlanePropertiesKHR)();
JNIEXPORT void (*_vkGetDisplayModePropertiesKHR)();
JNIEXPORT void (*_vkGetDisplayPlaneSupportedDisplaysKHR)();
JNIEXPORT void (*_vkGetDisplayPlaneCapabilitiesKHR)();
JNIEXPORT void (*_vkGetSwapchainImagesKHR)();
JNIEXPORT void (*_vkAcquireNextImageKHR)();
JNIEXPORT void (*_vkQueuePresentKHR)();

JNIEXPORT void (*_vkGetBufferMemoryRequirements)();
JNIEXPORT void (*_vkBindBufferMemory)();
JNIEXPORT void (*_vkAllocateDescriptorSets)();
JNIEXPORT void (*_vkUpdateDescriptorSets)();
JNIEXPORT void (*_vkWaitForFences)();
JNIEXPORT void (*_vkResetFences)();
JNIEXPORT void (*_vkBeginCommandBuffer)();
JNIEXPORT void (*_vkEndCommandBuffer)();
JNIEXPORT void (*_vkQueueSubmit)();
JNIEXPORT void (*_vkDeviceWaitIdle)();

JNIEXPORT void (*_vkCmdBeginRenderPass)();
JNIEXPORT void (*_vkCmdBindVertexBuffers)();
JNIEXPORT void (*_vkCmdBindPipeline)();
JNIEXPORT void (*_vkCmdBindDescriptorSets)();
JNIEXPORT void (*_vkCmdSetViewport)();
JNIEXPORT void (*_vkCmdSetScissor)();
JNIEXPORT void (*_vkCmdBindIndexBuffer)();
JNIEXPORT void (*_vkCmdDraw)();
JNIEXPORT void (*_vkCmdDrawIndexed)();
JNIEXPORT void (*_vkCmdEndRenderPass)();
JNIEXPORT void (*_vkCmdCopyBuffer)();
JNIEXPORT void (*_vkCmdPipelineBarrier)();
JNIEXPORT void (*_vkCmdCopyBufferToImage)();

#ifdef __cplusplus
}
#endif

jboolean VK_get_functions()
{
  vkGetFunction((void**)&_vkCreateInstance,"vkCreateInstance");
  vkGetFunction((void**)&_vkCreateDescriptorSetLayout,"vkCreateDescriptorSetLayout");
  vkGetFunction((void**)&_vkCreatePipelineLayout,"vkCreatePipelineLayout");
  vkGetFunction((void**)&_vkCreateShaderModule,"vkCreateShaderModule");
  vkGetFunction((void**)&_vkCreateGraphicsPipelines,"vkCreateGraphicsPipelines");
  vkGetFunction((void**)&_vkCreateBuffer,"vkCreateBuffer");
  vkGetFunction((void**)&_vkCreateRenderPass,"vkCreateRenderPass");
  vkGetFunction((void**)&_vkCreateRenderPass2,"vkCreateRenderPass2");
  vkGetFunction((void**)&_vkCreateCommandPool,"vkCreateCommandPool");
  vkGetFunction((void**)&_vkCreateSemaphore,"vkCreateSemaphore");
  vkGetFunction((void**)&_vkCreateImageView,"vkCreateImageView");
  vkGetFunction((void**)&_vkCreateFramebuffer,"vkCreateFramebuffer");
  vkGetFunction((void**)&_vkCreateFence,"vkCreateFence");
  vkGetFunction((void**)&_vkCreateDescriptorPool,"vkCreateDescriptorPool");
  vkGetFunction((void**)&_vkCreateSampler,"vkCreateSampler");

  vkGetFunction((void**)&_vkDestroyDevice,"vkDestroyDevice");
  vkGetFunction((void**)&_vkDestroyFence,"vkDestroyFence");
  vkGetFunction((void**)&_vkDestroyFramebuffer,"vkDestroyFramebuffer");
  vkGetFunction((void**)&_vkDestroyImageView,"vkDestroyImageView");
  vkGetFunction((void**)&_vkDestroyShaderModule,"vkDestroyShaderModule");
  vkGetFunction((void**)&_vkDestroySemaphore,"vkDestroySemaphore");
  vkGetFunction((void**)&_vkDestroyCommandPool,"vkDestroyCommandPool");
  vkGetFunction((void**)&_vkDestroyPipeline,"vkDestroyPipeline");
  vkGetFunction((void**)&_vkDestroyPipelineLayout,"vkDestroyPipelineLayout");
  vkGetFunction((void**)&_vkDestroyRenderPass,"vkDestroyRenderPass");
  vkGetFunction((void**)&_vkDestroyInstance,"vkDestroyInstance");
  vkGetFunction((void**)&_vkDestroyBuffer,"vkDestroyBuffer");
  vkGetFunction((void**)&_vkDestroyDescriptorSetLayout,"vkDestroyDescriptorSetLayout");
  vkGetFunction((void**)&_vkDestroyDescriptorPool,"vkDestroyDescriptorPool");
  vkGetFunction((void**)&_vkDestroyImage,"vkDestroyImage");
  vkGetFunction((void**)&_vkDestroySampler,"vkDestroySampler");

  vkGetFunction((void**)&_vkEnumeratePhysicalDevices,"vkEnumeratePhysicalDevices");
  vkGetFunction((void**)&_vkEnumerateDeviceExtensionProperties,"vkEnumerateDeviceExtensionProperties");
  vkGetFunction((void**)&_vkCreateDevice,"vkCreateDevice");
  vkGetFunction((void**)&_vkGetPhysicalDeviceFeatures2,"vkGetPhysicalDeviceFeatures2");
  vkGetFunction((void**)&_vkGetPhysicalDeviceProperties2,"vkGetPhysicalDeviceProperties2");
  vkGetFunction((void**)&_vkGetPhysicalDeviceMemoryProperties2,"vkGetPhysicalDeviceMemoryProperties2");
  vkGetFunction((void**)&_vkGetPhysicalDeviceQueueFamilyProperties2,"vkGetPhysicalDeviceQueueFamilyProperties2");
  vkGetFunction((void**)&_vkGetDeviceQueue,"vkGetDeviceQueue");
  vkGetFunction((void**)&_vkGetDeviceQueue2,"vkGetDeviceQueue2");
  vkGetFunction((void**)&_vkAllocateCommandBuffers,"vkAllocateCommandBuffers");
  vkGetFunction((void**)&_vkMapMemory,"vkMapMemory");
  vkGetFunction((void**)&_vkUnmapMemory,"vkUnmapMemory");
  vkGetFunction((void**)&_vkCreateImage,"vkCreateImage");
  vkGetFunction((void**)&_vkGetImageMemoryRequirements,"vkGetImageMemoryRequirements");
  vkGetFunction((void**)&_vkAllocateMemory,"vkAllocateMemory");
  vkGetFunction((void**)&_vkBindImageMemory,"vkBindImageMemory");
  vkGetFunction((void**)&_vkQueueWaitIdle,"vkQueueWaitIdle");
  vkGetFunction((void**)&_vkResetCommandBuffer,"vkResetCommandBuffer");
  vkGetFunction((void**)&_vkFreeCommandBuffers,"vkFreeCommandBuffers");
  vkGetFunction((void**)&_vkFreeMemory,"vkFreeMemory");

  //HKR extensions
  vkGetFunction((void**)&_vkCreateSwapchainKHR,"vkCreateSwapchainKHR");

  vkGetFunction((void**)&_vkDestroySwapchainKHR,"vkDestroySwapchainKHR");
  vkGetFunction((void**)&_vkDestroySurfaceKHR,"vkDestroySurfaceKHR");

  vkGetFunction((void**)&_vkGetPhysicalDeviceSurfaceFormats2KHR,"vkGetPhysicalDeviceSurfaceFormats2KHR");
  vkGetFunction((void**)&_vkGetPhysicalDeviceSurfaceCapabilitiesKHR,"vkGetPhysicalDeviceSurfaceCapabilitiesKHR");
  vkGetFunction((void**)&_vkGetPhysicalDeviceSurfaceSupportKHR,"vkGetPhysicalDeviceSurfaceSupportKHR");
  vkGetFunction((void**)&_vkGetPhysicalDeviceSurfacePresentModesKHR,"vkGetPhysicalDeviceSurfacePresentModesKHR");
  vkGetFunction((void**)&_vkGetPhysicalDeviceSurfaceFormatsKHR,"vkGetPhysicalDeviceSurfaceFormatsKHR");
  vkGetFunction((void**)&_vkGetPhysicalDeviceDisplayPropertiesKHR,"vkGetPhysicalDeviceDisplayPropertiesKHR");
  vkGetFunction((void**)&_vkGetPhysicalDeviceDisplayPlanePropertiesKHR,"vkGetPhysicalDeviceDisplayPlanePropertiesKHR");
  vkGetFunction((void**)&_vkGetDisplayModePropertiesKHR,"vkGetDisplayModePropertiesKHR");
  vkGetFunction((void**)&_vkGetDisplayPlaneSupportedDisplaysKHR,"vkGetDisplayPlaneSupportedDisplaysKHR");
  vkGetFunction((void**)&_vkGetDisplayPlaneCapabilitiesKHR,"vkGetDisplayPlaneCapabilitiesKHR");
  vkGetFunction((void**)&_vkGetSwapchainImagesKHR,"vkGetSwapchainImagesKHR");
  vkGetFunction((void**)&_vkAcquireNextImageKHR,"vkAcquireNextImageKHR");
  vkGetFunction((void**)&_vkQueuePresentKHR,"vkQueuePresentKHR");

  vkGetFunction((void**)&_vkGetBufferMemoryRequirements,"vkGetBufferMemoryRequirements");
  vkGetFunction((void**)&_vkBindBufferMemory,"vkBindBufferMemory");
  vkGetFunction((void**)&_vkAllocateDescriptorSets,"vkAllocateDescriptorSets");
  vkGetFunction((void**)&_vkUpdateDescriptorSets,"vkUpdateDescriptorSets");
  vkGetFunction((void**)&_vkWaitForFences,"vkWaitForFences");
  vkGetFunction((void**)&_vkResetFences,"vkResetFences");
  vkGetFunction((void**)&_vkBeginCommandBuffer,"vkBeginCommandBuffer");
  vkGetFunction((void**)&_vkEndCommandBuffer,"vkEndCommandBuffer");
  vkGetFunction((void**)&_vkQueueSubmit,"vkQueueSubmit");
  vkGetFunction((void**)&_vkDeviceWaitIdle,"vkDeviceWaitIdle");

  vkGetFunction((void**)&_vkCmdBeginRenderPass,"vkCmdBeginRenderPass");
  vkGetFunction((void**)&_vkCmdBindVertexBuffers,"vkCmdBindVertexBuffers");
  vkGetFunction((void**)&_vkCmdBindPipeline,"vkCmdBindPipeline");
  vkGetFunction((void**)&_vkCmdBindDescriptorSets,"vkCmdBindDescriptorSets");
  vkGetFunction((void**)&_vkCmdSetViewport,"vkCmdSetViewport");
  vkGetFunction((void**)&_vkCmdSetScissor,"vkCmdSetScissor");
  vkGetFunction((void**)&_vkCmdBindIndexBuffer,"vkCmdBindIndexBuffer");
  vkGetFunction((void**)&_vkCmdDraw,"vkCmdDraw");
  vkGetFunction((void**)&_vkCmdDrawIndexed,"vkCmdDrawIndexed");
  vkGetFunction((void**)&_vkCmdEndRenderPass,"vkCmdEndRenderPass");
  vkGetFunction((void**)&_vkCmdCopyBuffer,"vkCmdCopyBuffer");
  vkGetFunction((void**)&_vkCmdPipelineBarrier,"vkCmdPipelineBarrier");
  vkGetFunction((void**)&_vkCmdCopyBufferToImage,"vkCmdCopyBufferToImage");

  return JNI_TRUE;
}
