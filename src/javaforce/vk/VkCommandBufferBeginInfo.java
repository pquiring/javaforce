package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCommandBufferBeginInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCommandBufferBeginInfo.html
 *
 * @author pquiring
 */

public class VkCommandBufferBeginInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
  /** pNext */
  public long pNext;
  /** VkCommandBufferUsageFlags */
  public int flags;
  /** */
  public VkCommandBufferInheritanceInfo[] ptr_pInheritanceInfo;
}
