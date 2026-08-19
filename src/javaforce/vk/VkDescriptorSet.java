package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorSet.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorSet.html
 *
 * @author pquiring
 */

public class VkDescriptorSet extends FFMType.Uint64 {
  public VkDescriptorSet() {}
  public VkDescriptorSet(long value) {super(value);}
  public VkDescriptorSet(FFMType.Uint64 value) {super(value);}
}
