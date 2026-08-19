package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageCreateInfo.html
 *
 * @author pquiring
 */

public class VkImageCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkImageCreateFlags */
  public int flags;
  /** VkImageType enum */
  public int imageType;
  /** VkFormat enum */
  public int format;
  /** VkExtent3D */
  public VkExtent3D extent = new VkExtent3D();
  /** */
  public int mipLevels;
  /** */
  public int arrayLayers;
  /** VkSampleCountFlagBits */
  public int samples;
  /** VkImageTiling enum */
  public int tiling;
  /** VkImageUsageFlags */
  public int usage;
  /** VkSharingMode enum */
  public int sharingMode;
  /** */
  public int queueFamilyIndexCount;
  /** */
  public int[] ptr_pQueueFamilyIndices;
  /** VkImageLayout enum */
  public int initialLayout;
}
