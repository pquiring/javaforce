package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImage.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImage.html
 *
 * @author pquiring
 */

public class VkImage extends FFMType.Uint64 {
  public VkImage() {}
  public VkImage(long value) {super(value);}
  public VkImage(FFMType.Uint64 value) {super(value);}
}
