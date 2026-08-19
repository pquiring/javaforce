package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFence.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFence.html
 *
 * @author pquiring
 */

public class VkFence extends FFMType.Uint64 {
  public VkFence() {}
  public VkFence(long value) {super(value);}
  public VkFence(FFMType.Uint64 value) {super(value);}
}
