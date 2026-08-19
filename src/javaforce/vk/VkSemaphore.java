package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSemaphore.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSemaphore.html
 *
 * @author pquiring
 */

public class VkSemaphore extends FFMType.Uint64 {
  public VkSemaphore() {}
  public VkSemaphore(long value) {super(value);}
  public VkSemaphore(FFMType.Uint64 value) {super(value);}
}
