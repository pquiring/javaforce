package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceMemoryProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceMemoryProperties.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceMemoryProperties extends FFMStruct {
  /** */
  public int memoryTypeCount;
  /** */
  public VkMemoryType[] memoryTypes = new VkMemoryType[32];
  /** */
  public int memoryHeapCount;
  /** */
  public VkMemoryHeap[] memoryHeaps = new VkMemoryHeap[16];
}
