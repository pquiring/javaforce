package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageSubresourceRange.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageSubresourceRange.html
 *
 * @author pquiring
 */

public class VkImageSubresourceRange extends FFMStruct {
  /** VkImageAspectFlags */
  public int aspectMask;
  /** */
  public int baseMipLevel;
  /** */
  public int levelCount;
  /** */
  public int baseArrayLayer;
  /** */
  public int layerCount;
}
