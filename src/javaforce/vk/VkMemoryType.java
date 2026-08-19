package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceMemoryProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkMemoryType.html
 *
 * @author pquiring
 */

public class VkMemoryType extends FFMStruct {
  /** */
  public int propertyFlags;
  /** */
  public int heapIndex;
}
