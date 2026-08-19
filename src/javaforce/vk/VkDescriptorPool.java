package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorPool.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorPool.html
 *
 * @author pquiring
 */

public class VkDescriptorPool extends FFMType.Uint64 {
  public VkDescriptorPool() {}
  public VkDescriptorPool(long value) {super(value);}
  public VkDescriptorPool(FFMType.Uint64 value) {super(value);}
}
