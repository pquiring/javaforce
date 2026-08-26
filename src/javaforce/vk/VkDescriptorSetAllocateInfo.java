package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorSetAllocateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorSetAllocateInfo.html
 *
 * @author pquiring
 */

public class VkDescriptorSetAllocateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
  /** reserved */
  public long pNext;
  /** */
  public VkDescriptorPool descriptorPool;
  /** */
  public int descriptorSetCount;
  /** */
  public VkDescriptorSetLayout[] ptr_pSetLayouts;
}
