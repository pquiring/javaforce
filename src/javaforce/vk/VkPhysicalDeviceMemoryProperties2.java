package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceProperties2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceMemoryProperties2.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceMemoryProperties2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2;
  /** reserved */
  public long pNext;
  /** VkPhysicalDeviceMemoryProperties. */
  public VkPhysicalDeviceMemoryProperties memoryProperties = new VkPhysicalDeviceMemoryProperties();
}
