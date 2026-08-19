package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkInstanceCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkInstanceCreateInfo.html
 *
 * @author pquiring
 */

public class VkInstanceCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkInstanceCreateFlags (reserved) */
  public int flags;
  /** VkApplicationInfo */
  public VkApplicationInfo ptr_pApplicationInfo = new VkApplicationInfo();
  /** layer count */
  public int enabledLayerCount;
  /** layer names */
  public String[] ptr_ppEnabledLayerNames;
  /** extension count */
  public int enabledExtensionCount;
  /** extension names */
  public String[] ptr_ppEnabledExtensionNames;
}
