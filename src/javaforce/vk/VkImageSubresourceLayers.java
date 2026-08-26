package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageSubresourceLayers.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageSubresourceLayers.html
 *
 * @author pquiring
 */

public class VkImageSubresourceLayers extends FFMStruct {
  /** */
  public int aspectMask;
  /** */
  public int mipLevel;
  /** */
  public int baseArrayLayer;
  /** */
  public int layerCount;
}
