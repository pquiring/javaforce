package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageMemoryBarrier.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageMemoryBarrier.html
 *
 * @author pquiring
 */

public class VkImageMemoryBarrier extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
  /** reserved */
  public long pNext;
  /** */
  public int srcAccessMask;
  /** */
  public int dstAccessMask;
  /** */
  public VkImageLayout oldLayout = new VkImageLayout();
  /** */
  public VkImageLayout newLayout = new VkImageLayout();
  /** */
  public int srcQueueFamilyIndex;
  /** */
  public int dstQueueFamilyIndex;
  /** */
  public VkImage image = new VkImage();
  /** */
  public VkImageSubresourceRange subresourceRange = new VkImageSubresourceRange();
}
