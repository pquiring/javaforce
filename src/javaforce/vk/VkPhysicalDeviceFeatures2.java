package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceFeatures2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceFeatures2.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceFeatures2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
  /** Array of VkBool32 features [55]. */
  public VkPhysicalDeviceFeatures features = new VkPhysicalDeviceFeatures();
}
