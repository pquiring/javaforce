package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBufferCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBufferCreateInfo.html
 *
 * @author pquiring
 */

public class VkBufferCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkBufferCreateFlags */
  public int flags;
  /** VkDeviceSize */
  public int size;
  /** VkBufferUsageFlags */
  public int usage;
  /** VkSharingMode enum */
  public int sharingMode;
  /** */
  public int queueFamilyIndexCount;
  /** */
  public int[] ptr_pQueueFamilyIndices;
}
