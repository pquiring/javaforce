package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;
import static javaforce.vk.VK.*;

/** VkPhysicalDeviceMemoryProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceMemoryProperties.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceMemoryProperties extends FFMStruct {
  public VkPhysicalDeviceMemoryProperties() {
    for(int i=0;i<VK_MAX_MEMORY_TYPES;i++) {
      memoryTypes[i] = new VkMemoryType();
    }
    for(int i=0;i<VK_MAX_MEMORY_HEAPS;i++) {
      memoryHeaps[i] = new VkMemoryHeap();
    }
  }
  /** */
  public int memoryTypeCount;
  /** */
  public VkMemoryType[] memoryTypes = new VkMemoryType[VK_MAX_MEMORY_TYPES];
  /** */
  public int memoryHeapCount;
  /** */
  public VkMemoryHeap[] memoryHeaps = new VkMemoryHeap[VK_MAX_MEMORY_HEAPS];
}
