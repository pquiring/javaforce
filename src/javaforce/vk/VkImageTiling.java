package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageTiling.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageTiling.html
 *
 * @author pquiring
 */

public class VkImageTiling extends FFMType.Uint32 {
  public VkImageTiling() {}
  public VkImageTiling(int value) {super(value);}
  public VkImageTiling(FFMType.Uint32 value) {super(value);}
}
