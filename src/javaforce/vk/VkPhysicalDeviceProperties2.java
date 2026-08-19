package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceProperties2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceProperties2.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceProperties2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2;
  /** reserved */
  public long pNext;
  /** VkPhysicalDeviceProperties. */
  public VkPhysicalDeviceProperties properties = new VkPhysicalDeviceProperties();
}
