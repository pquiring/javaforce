package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorBufferInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorBufferInfo.html
 *
 * @author pquiring
 */

public class VkDescriptorBufferInfo extends FFMStruct {
  /** VkBuffer */
  public VkBuffer buffer;
  /** VkDeviceSize */
  public VkDeviceSize offset;
  /** VkDeviceSize */
  public VkDeviceSize range;
}
