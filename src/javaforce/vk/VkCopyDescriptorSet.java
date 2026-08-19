package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCopyDescriptorSet.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCopyDescriptorSet.html
 *
 * @author pquiring
 */

public class VkCopyDescriptorSet extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_COPY_DESCRIPTOR_SET;
  /** pNext */
  public long pNext;
  /** VkDescriptorSet */
  public long srcSet;
  /**  */
  public int srcBinding;
  /**  */
  public int srcArrayElement;
  /** VkDescriptorSet */
  public long dstSet;
  /**  */
  public int dstBinding;
  /**  */
  public int dstArrayElement;
  /**  */
  public int descriptorCount;
}
