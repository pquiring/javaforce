package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceLimits.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceLimits.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceLimits extends FFMStruct {
  public int maxImageDimension1D;
  public int maxImageDimension2D;
  public int maxImageDimension3D;
  public int maxImageDimensionCube;
  public int maxImageArrayLayers;
  public int maxTexelBufferElements;
  public int maxUniformBufferRange;
  public int maxStorageBufferRange;
  public int maxPushConstantsSize;
  public int maxMemoryAllocationCount;
  public int maxSamplerAllocationCount;
  public VkDeviceSize bufferImageGranularity = new VkDeviceSize();
  public VkDeviceSize sparseAddressSpaceSize = new VkDeviceSize();
  public int maxBoundDescriptorSets;
  public int maxPerStageDescriptorSamplers;
  public int maxPerStageDescriptorUniformBuffers;
  public int maxPerStageDescriptorStorageBuffers;
  public int maxPerStageDescriptorSampledImages;
  public int maxPerStageDescriptorStorageImages;
  public int maxPerStageDescriptorInputAttachments;
  public int maxPerStageResources;
  public int maxDescriptorSetSamplers;
  public int maxDescriptorSetUniformBuffers;
  public int maxDescriptorSetUniformBuffersDynamic;
  public int maxDescriptorSetStorageBuffers;
  public int maxDescriptorSetStorageBuffersDynamic;
  public int maxDescriptorSetSampledImages;
  public int maxDescriptorSetStorageImages;
  public int maxDescriptorSetInputAttachments;
  public int maxVertexInputAttributes;
  public int maxVertexInputBindings;
  public int maxVertexInputAttributeOffset;
  public int maxVertexInputBindingStride;
  public int maxVertexOutputComponents;
  public int maxTessellationGenerationLevel;
  public int maxTessellationPatchSize;
  public int maxTessellationControlPerVertexInputComponents;
  public int maxTessellationControlPerVertexOutputComponents;
  public int maxTessellationControlPerPatchOutputComponents;
  public int maxTessellationControlTotalOutputComponents;
  public int maxTessellationEvaluationInputComponents;
  public int maxTessellationEvaluationOutputComponents;
  public int maxGeometryShaderInvocations;
  public int maxGeometryInputComponents;
  public int maxGeometryOutputComponents;
  public int maxGeometryOutputVertices;
  public int maxGeometryTotalOutputComponents;
  public int maxFragmentInputComponents;
  public int maxFragmentOutputAttachments;
  public int maxFragmentDualSrcAttachments;
  public int maxFragmentCombinedOutputResources;
  public int maxComputeSharedMemorySize;
  public int[] maxComputeWorkGroupCount = new int[3];
  public int maxComputeWorkGroupInvocations;
  public int[] maxComputeWorkGroupSize = new int[3];
  public int subPixelPrecisionBits;
  public int subTexelPrecisionBits;
  public int mipmapPrecisionBits;
  public int maxDrawIndexedIndexValue;
  public int maxDrawIndirectCount;
  public float maxSamplerLodBias;
  public float maxSamplerAnisotropy;
  public int maxViewports;
  public int[] maxViewportDimensions = new int[2];
  public float[] viewportBoundsRange = new float[2];
  public int viewportSubPixelBits;
  public long minMemoryMapAlignment;
  public VkDeviceSize minTexelBufferOffsetAlignment = new VkDeviceSize();
  public VkDeviceSize minUniformBufferOffsetAlignment = new VkDeviceSize();
  public VkDeviceSize minStorageBufferOffsetAlignment = new VkDeviceSize();
  public int minTexelOffset;
  public int maxTexelOffset;
  public int minTexelGatherOffset;
  public int maxTexelGatherOffset;
  public float minInterpolationOffset;
  public float maxInterpolationOffset;
  public int subPixelInterpolationOffsetBits;
  public int maxFramebufferWidth;
  public int maxFramebufferHeight;
  public int maxFramebufferLayers;
  /** VkSampleCountFlags */
  public int framebufferColorSampleCounts;
  /** VkSampleCountFlags */
  public int framebufferDepthSampleCounts;
  /** VkSampleCountFlags */
  public int framebufferStencilSampleCounts;
  /** VkSampleCountFlags */
  public int framebufferNoAttachmentsSampleCounts;
  public int maxColorAttachments;
  /** VkSampleCountFlags */
  public int sampledImageColorSampleCounts;
  /** VkSampleCountFlags */
  public int sampledImageIntegerSampleCounts;
  /** VkSampleCountFlags */
  public int sampledImageDepthSampleCounts;
  /** VkSampleCountFlags */
  public int sampledImageStencilSampleCounts;
  /** VkSampleCountFlags */
  public int storageImageSampleCounts;
  public int maxSampleMaskWords;
  /** VkBool32 */
  public int timestampComputeAndGraphics;
  public float timestampPeriod;
  public int maxClipDistances;
  public int maxCullDistances;
  public int maxCombinedClipAndCullDistances;
  public int discreteQueuePriorities;
  public float[] pointSizeRange = new float[2];
  public float[] lineWidthRange = new float[2];
  public float pointSizeGranularity;
  public float lineWidthGranularity;
  /** VkBool32 */
  public int strictLines;
  /** VkBool32 */
  public int standardSampleLocations;
  public VkDeviceSize optimalBufferCopyOffsetAlignment = new VkDeviceSize();
  public VkDeviceSize optimalBufferCopyRowPitchAlignment = new VkDeviceSize();
  public VkDeviceSize nonCoherentAtomSize = new VkDeviceSize();
}
