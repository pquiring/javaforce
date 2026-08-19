package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDeviceQueueCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDeviceQueueCreateInfo.html
 *
 * @author pquiring
 */

public class VkDeviceQueueCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkDeviceQueueCreateFlags */
  public int flags;
  /** */
  public int queueFamilyIndex;
  /** */
  public int queueCount;
  /** */
  public float[] ptr_pQueuePriorities;
}
