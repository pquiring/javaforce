package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceSurfaceInfo2KHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceSurfaceInfo2KHR.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceSurfaceInfo2KHR extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SURFACE_INFO_2_KHR;
  /** pNext */
  public long pNext;
  /** VkSurfaceKHR */
  public long surface;
}
