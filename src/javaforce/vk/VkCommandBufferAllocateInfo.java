package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCommandBufferAllocateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCommandBufferAllocateInfo.html
 *
 * @author pquiring
 */

public class VkCommandBufferAllocateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
  /** pNext */
  public long pNext;
  /** */
  public VkCommandPool commandPool;
  /** VkCommandBufferLevel enum */
  public int level;
  /** */
  public int commandBufferCount;
}
