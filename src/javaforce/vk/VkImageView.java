package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageView.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageView.html
 *
 * @author pquiring
 */

public class VkImageView extends FFMType.Uint64 {
  public VkImageView() {}
  public VkImageView(long value) {super(value);}
  public VkImageView(FFMType.Uint64 value) {super(value);}
}
