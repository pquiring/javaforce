package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorPoolCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorPoolCreateInfo.html
 *
 * @author pquiring
 */

public class VkDescriptorPoolCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkDescriptorPoolCreateFlags */
  public int flags;
  /** */
  public int maxSets;
  /** */
  public int poolSizeCount;
  /** */
  public VkDescriptorPoolSize[] ptr_pPoolSizes;
}
