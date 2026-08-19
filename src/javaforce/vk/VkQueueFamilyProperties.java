package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkQueueFamilyProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkQueueFamilyProperties.html
 *
 * @author pquiring
 */

public class VkQueueFamilyProperties extends FFMStruct {
  /** */
  public int queueFlags;
  /** */
  public int queueCount;
  /** */
  public int timestampValidBits;
  /** */
  public VkExtent3D minImageTransferGranularity = new VkExtent3D();
}
