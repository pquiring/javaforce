package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceFeatures.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceFeatures.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceFeatures extends FFMStruct {
  public int robustBufferAccess;
  public int fullDrawIndexUint32;
  public int imageCubeArray;
  public int independentBlend;
  public int geometryShader;
  public int tessellationShader;
  public int sampleRateShading;
  public int dualSrcBlend;
  public int logicOp;
  public int multiDrawIndirect;
  public int drawIndirectFirstInstance;
  public int depthClamp;
  public int depthBiasClamp;
  public int fillModeNonSolid;
  public int depthBounds;
  public int wideLines;
  public int largePoints;
  public int alphaToOne;
  public int multiViewport;
  public int samplerAnisotropy;
  public int textureCompressionETC2;
  public int textureCompressionASTC_LDR;
  public int textureCompressionBC;
  public int occlusionQueryPrecise;
  public int pipelineStatisticsQuery;
  public int vertexPipelineStoresAndAtomics;
  public int fragmentStoresAndAtomics;
  public int shaderTessellationAndGeometryPointSize;
  public int shaderImageGatherExtended;
  public int shaderStorageImageExtendedFormats;
  public int shaderStorageImageMultisample;
  public int shaderStorageImageReadWithoutFormat;
  public int shaderStorageImageWriteWithoutFormat;
  public int shaderUniformBufferArrayDynamicIndexing;
  public int shaderSampledImageArrayDynamicIndexing;
  public int shaderStorageBufferArrayDynamicIndexing;
  public int shaderStorageImageArrayDynamicIndexing;
  public int shaderClipDistance;
  public int shaderCullDistance;
  public int shaderFloat64;
  public int shaderInt64;
  public int shaderInt16;
  public int shaderResourceResidency;
  public int shaderResourceMinLod;
  public int sparseBinding;
  public int sparseResidencyBuffer;
  public int sparseResidencyImage2D;
  public int sparseResidencyImage3D;
  public int sparseResidency2Samples;
  public int sparseResidency4Samples;
  public int sparseResidency8Samples;
  public int sparseResidency16Samples;
  public int sparseResidencyAliased;
  public int variableMultisampleRate;
  public int inheritedQueries;
}
