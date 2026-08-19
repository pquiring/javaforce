package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceMemoryProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkMemoryHeap.html
 *
 * @author pquiring
 */

public class VkMemoryHeap extends FFMStruct {
  /** */
  public int size;
  /** */
  public int flags;
}
