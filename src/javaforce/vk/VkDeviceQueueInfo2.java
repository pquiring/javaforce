package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDeviceQueueInfo2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDeviceQueueInfo2.html
 *
 * @author pquiring
 */

public class VkDeviceQueueInfo2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_DEVICE_QUEUE_INFO_2;
  /** pNext */
  public long pNext;
  /** */
  public int flags;
  /** */
  public int queueFamilyIndex;
  /** */
  public int queueIndex;
}
