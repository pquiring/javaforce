package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorSetLayoutCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorSetLayoutCreateInfo.html
 *
 * @author pquiring
 */

public class VkDescriptorSetLayoutCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkDescriptorSetLayoutCreateFlags */
  public int flags;
  /** */
  public int bindingCount;
  /** */
  public VkDescriptorSetLayoutBinding ptr_pBindings;
}
