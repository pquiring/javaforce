package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkWriteDescriptorSet.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkWriteDescriptorSet.html
 *
 * @author pquiring
 */

public class VkWriteDescriptorSet extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
  /** pNext */
  public long pNext;
  /** VkDescriptorSet */
  public long dstSet;
  /**  */
  public int dstBinding;
  /**  */
  public int dstArrayElement;
  /**  */
  public int descriptorCount;
  /** VkDescriptorType */
  public long descriptorType;
  /**  */
  public VkDescriptorImageInfo pImageInfo;
  /**  */
  public VkDescriptorBufferInfo pBufferInfo;
  /** VkBufferView */
  public long pTexelBufferView;
}
