package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorImageInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorImageInfo.html
 *
 * @author pquiring
 */

public class VkDescriptorImageInfo extends FFMStruct {
  /** VkSampler */
  public long sampler;
  /** VkImageView */
  public long imageView;
  /** VkImageLayout */
  public long imageLayout;
}
