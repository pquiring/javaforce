package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkMemoryBarrier.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkMemoryBarrier.html
 *
 * @author pquiring
 */

public class VkMemoryBarrier extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_MEMORY_BARRIER;
  /** reserved */
  public long pNext;
  /** */
  public int srcAccessMask;
  /** */
  public int dstAccessMask;
}
