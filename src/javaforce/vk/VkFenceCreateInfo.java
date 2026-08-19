package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFenceCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFenceCreateInfo.html
 *
 * @author pquiring
 */

public class VkFenceCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkFenceCreateFlags */
  public int flags;
}
