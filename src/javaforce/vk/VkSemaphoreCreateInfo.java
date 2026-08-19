package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSemaphoreCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSemaphoreCreateInfo.html
 *
 * @author pquiring
 */

public class VkSemaphoreCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkSemaphoreCreateFlags */
  public int flags;
}
