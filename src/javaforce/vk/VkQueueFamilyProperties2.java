package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkQueueFamilyProperties2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkQueueFamilyProperties2.html
 *
 * @author pquiring
 */

public class VkQueueFamilyProperties2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_QUEUE_FAMILY_PROPERTIES_2;
  /** reserved */
  public long pNext;
  /** VkPhysicalDeviceMemoryProperties. */
  public VkQueueFamilyProperties queueFamilyProperties = new VkQueueFamilyProperties();
}
