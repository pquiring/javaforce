package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCommandPool.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCommandPool.html
 *
 * @author pquiring
 */

public class VkCommandPool extends FFMType.Uint64 {
  public VkCommandPool() {}
  public VkCommandPool(long value) {super(value);}
  public VkCommandPool(FFMType.Uint64 value) {super(value);}
}
