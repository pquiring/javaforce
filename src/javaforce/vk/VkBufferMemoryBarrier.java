package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBufferMemoryBarrier.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBufferMemoryBarrier.html
 *
 * @author pquiring
 */

public class VkBufferMemoryBarrier extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER;
  /** reserved */
  public long pNext;
  /** */
  public int srcAccessMask;
  /** */
  public int dstAccessMask;
  /** */
  public int srcQueueFamilyIndex;
  /** */
  public int dstQueueFamilyIndex;
  /** */
  public VkBuffer buffer = new VkBuffer();
  /** */
  public VkDeviceSize offset = new VkDeviceSize();
  /** */
  public VkDeviceSize size = new VkDeviceSize();
}
