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
  /**  */
  public VkDescriptorSet dstSet = new VkDescriptorSet();
  /**  */
  public int dstBinding;
  /**  */
  public int dstArrayElement;
  /**  */
  public int descriptorCount;
  /** VkDescriptorType */
  public int descriptorType;
  /**  */
  public VkDescriptorImageInfo ptr_pImageInfo = new VkDescriptorImageInfo();
  /**  */
  public VkDescriptorBufferInfo ptr_pBufferInfo = new VkDescriptorBufferInfo();
  /** VkBufferView */
  public VkBufferView ptr_pTexelBufferView;
}
