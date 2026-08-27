#include <stdio.h>

#include "../vulkan/include/vulkan/vulkan.h"

#define getSize(x) \
  printf("sizeof(" #x ")=%d\n", sizeof(x));

struct sTest {
  int sType;
  int64_t pNext;
  int flags;
};

typedef struct sTest Test;

int main() {

  getSize(VkApplicationInfo);
  getSize(VkAttachmentDescription);
  getSize(VkAttachmentReference);
  getSize(VkAttachmentReference2);
  getSize(VkBufferCopy);
  getSize(VkBufferCreateInfo);
  getSize(VkBufferImageCopy);
  getSize(VkBufferMemoryBarrier);
  getSize(VkClearColorValue);
  getSize(VkClearDepthStencilValue);
  getSize(VkClearValue);
  getSize(VkCommandBufferAllocateInfo);
  getSize(VkCommandBufferBeginInfo);
  getSize(VkCommandBufferInheritanceInfo);
  getSize(VkCommandPoolCreateInfo);
  getSize(VkComponentMapping);
  getSize(VkCopyDescriptorSet);
  getSize(VkDescriptorBufferInfo);
  getSize(VkDescriptorImageInfo);
  getSize(VkDescriptorPoolCreateInfo);
  getSize(VkDescriptorPoolSize);
  getSize(VkDescriptorSetAllocateInfo);
  getSize(VkDescriptorSetLayoutBinding);
  getSize(VkDescriptorSetLayoutCreateInfo);
  getSize(VkDeviceCreateInfo);
  getSize(VkDeviceQueueCreateInfo);
  getSize(VkDeviceQueueInfo2);
  getSize(VkDisplayModeParametersKHR);
  getSize(VkDisplayModePropertiesKHR);
  getSize(VkDisplayPlaneCapabilitiesKHR);
  getSize(VkDisplayPlanePropertiesKHR);
  getSize(VkDisplayPropertiesKHR);
  getSize(VkExtensionProperties);
  getSize(VkExtent2D);
  getSize(VkExtent3D);
  getSize(VkFenceCreateInfo);
  getSize(VkFormatProperties);
  getSize(VkFramebufferCreateInfo);
  getSize(VkGraphicsPipelineCreateInfo);
  getSize(VkImageCreateInfo);
  getSize(VkImageMemoryBarrier);
  getSize(VkImageSubresourceLayers);
  getSize(VkImageSubresourceRange);
  getSize(VkImageViewCreateInfo);
  getSize(VkInstanceCreateInfo);
  getSize(VkMemoryAllocateInfo);
  getSize(VkMemoryBarrier);
  getSize(VkMemoryHeap);
  getSize(VkMemoryRequirements);
  getSize(VkMemoryType);
  getSize(VkOffset2D);
  getSize(VkOffset3D);
  getSize(VkPhysicalDeviceFeatures);
  getSize(VkPhysicalDeviceFeatures2);
  getSize(VkPhysicalDeviceLimits);
  getSize(VkPhysicalDeviceMemoryProperties);
  getSize(VkPhysicalDeviceMemoryProperties2);
  getSize(VkPhysicalDeviceProperties);
  getSize(VkPhysicalDeviceProperties2);
  getSize(VkPhysicalDeviceSurfaceInfo2KHR);
  getSize(VkPipelineColorBlendAttachmentState);
  getSize(VkPipelineColorBlendStateCreateInfo);
  getSize(VkPipelineDepthStencilStateCreateInfo);
  getSize(VkPipelineDynamicStateCreateInfo);
  getSize(VkPipelineInputAssemblyStateCreateInfo);
  getSize(VkPipelineLayoutCreateInfo);
  getSize(VkPipelineMultisampleStateCreateInfo);
  getSize(VkPipelineRasterizationStateCreateInfo);
  getSize(VkPipelineShaderStageCreateInfo);
  getSize(VkPipelineTessellationStateCreateInfo);
  getSize(VkPipelineVertexInputStateCreateInfo);
  getSize(VkPipelineViewportStateCreateInfo);
  getSize(VkPresentInfoKHR);
  getSize(VkPushConstantRange);
  getSize(VkQueueFamilyProperties);
  getSize(VkQueueFamilyProperties2);
  getSize(VkRect2D);
  getSize(VkRenderPassBeginInfo);
  getSize(VkRenderPassCreateInfo);
  getSize(VkRenderPassCreateInfo2);
  getSize(VkSamplerCreateInfo);
  getSize(VkSemaphoreCreateInfo);
  getSize(VkShaderModuleCreateInfo);
  getSize(VkSpecializationInfo);
  getSize(VkSpecializationMapEntry);
  getSize(VkStencilOpState);
  getSize(VkSubmitInfo);
  getSize(VkSubpassDependency);
  getSize(VkSubpassDescription);
  getSize(VkSurfaceCapabilitiesKHR);
  getSize(VkSurfaceFormat2KHR);
  getSize(VkSurfaceFormatKHR);
  getSize(VkSwapchainCreateInfoKHR);
  getSize(VkVertexInputAttributeDescription);
  getSize(VkVertexInputBindingDescription);
  getSize(VkViewport);
  getSize(VkWriteDescriptorSet);

//VkFenceCreateInfo
  printf("VkFenceCreateInfo:size=%d\n", sizeof(VkFenceCreateInfo));
  printf("offsetof(sType)=%d\n", offsetof(VkFenceCreateInfo, sType));
  printf("offsetof(pNext)=%d\n", offsetof(VkFenceCreateInfo, pNext));
  printf("offsetof(flags)=%d\n", offsetof(VkFenceCreateInfo, flags));
  VkFenceCreateInfo info;
  printf("sizeof(sType)=%d\n", sizeof(info.sType));
  printf("sizeof(sType)=%d\n", sizeof(info.pNext));
  printf("sizeof(sType)=%d\n", sizeof(info.flags));

//Test
  printf("Test:size=%d\n", sizeof(Test));
  printf("offsetof(sType)=%d\n", offsetof(Test, sType));
  printf("offsetof(pNext)=%d\n", offsetof(Test, pNext));
  printf("offsetof(flags)=%d\n", offsetof(Test, flags));
  Test test;
  printf("sizeof(sType)=%d\n", sizeof(test.sType));
  printf("sizeof(sType)=%d\n", sizeof(test.pNext));
  printf("sizeof(sType)=%d\n", sizeof(test.flags));
  return 0;
}
