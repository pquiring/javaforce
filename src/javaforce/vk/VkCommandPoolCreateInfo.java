package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCommandPoolCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCommandPoolCreateInfo.html
 *
 * @author pquiring
 */

public class VkCommandPoolCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkCommandPoolCreateFlags */
  public int flags;
  /** */
  public int queueFamilyIndex;
}
