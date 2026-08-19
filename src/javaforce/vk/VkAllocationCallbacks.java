package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkAllocationCallbacks.
 *
 * VkAllocationCallbacks are not supported and MUST be null (zero).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkAllocationCallbacks.html
 *
 * @author pquiring
 */

public class VkAllocationCallbacks extends FFMType.Uint64 {
  public VkAllocationCallbacks() {}
  public VkAllocationCallbacks(long value) {super(value);}
  public VkAllocationCallbacks(FFMType.Uint64 value) {super(value);}
}
