package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDeviceSize (uint64_t).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDeviceSize.html
 *
 * @author pquiring
 */

public class VkDeviceSize extends FFMType.Uint64 {
  public VkDeviceSize() {}
  public VkDeviceSize(long value) {super(value);}
  public VkDeviceSize(FFMType.Uint64 value) {super(value);}
}
