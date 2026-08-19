package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDeviceCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDeviceCreateInfo.html
 *
 * @author pquiring
 */

public class VkDeviceCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkInstanceCreateFlags (reserved) */
  public int flags;
  /** */
  public int queueCreateInfoCount;
  /** */
  public VkDeviceQueueCreateInfo[] ptr_pQueueCreateInfos;
  /** reserved */
  public int enabledLayerCount;
  /** reserved */
  public String[] ptr_ppEnabledLayerNames;
  /** extension count */
  public int enabledExtensionCount;
  /** extension names */
  public String[] ptr_ppEnabledExtensionNames;
  /** Array of VkBool32 features [55] */
  public VkPhysicalDeviceFeatures ptr_pEnabledFeatures = new VkPhysicalDeviceFeatures();
}
