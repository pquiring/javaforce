package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSampler.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSampler.html
 *
 * @author pquiring
 */

public class VkSampler extends FFMType.Uint64 {
  public VkSampler() {}
  public VkSampler(long value) {super(value);}
  public VkSampler(FFMType.Uint64 value) {super(value);}
}
