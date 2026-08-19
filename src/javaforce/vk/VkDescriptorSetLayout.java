package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDescriptorSetLayout.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorSetLayout.html
 *
 * @author pquiring
 */

public class VkDescriptorSetLayout extends FFMType.Uint64 {
  public VkDescriptorSetLayout() {}
  public VkDescriptorSetLayout(long value) {super(value);}
  public VkDescriptorSetLayout(FFMType.Uint64 value) {super(value);}
}
