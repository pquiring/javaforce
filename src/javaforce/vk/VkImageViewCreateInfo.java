package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageViewCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageViewCreateInfo.html
 *
 * @author pquiring
 */

public class VkImageViewCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkImageViewCreateFlags */
  public int flags;
  /** VkImage */
  public VkImage image = new VkImage();
  /** VkImageViewType enum */
  public int viewType;
  /** VkFormat enum */
  public VkFormat format = new VkFormat();
  /** */
  public VkComponentMapping components = new VkComponentMapping();
  /** */
  public VkImageSubresourceRange subresourceRange = new VkImageSubresourceRange();
}
