package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkMemoryAllocateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkMemoryAllocateInfo.html
 *
 * @author pquiring
 */

public class VkMemoryAllocateInfo extends FFMStruct {
  /** */
  public int sType = VK.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
  /** */
  public long pNext;
  /** */
  public long allocationSize;
  /** */
  public int memoryTypeIndex;
}
