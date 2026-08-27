package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorSetLayoutBinding.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorSetLayoutBinding.html
 *
 * @author pquiring
 */

public class VkDescriptorSetLayoutBinding extends FFMStruct {
  /** */
  public int binding;
  /** VkDescriptorType enum */
  public int descriptorType;
  /** */
  public int descriptorCount;
  /** VkShaderStageFlags */
  public int stageFlags;
  /** */
  public VkSampler ptr_pImmutableSamplers;
}
