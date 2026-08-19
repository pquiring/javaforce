package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBuffer.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBuffer.html
 *
 * @author pquiring
 */

public class VkBuffer extends FFMType.Uint64 {
  public VkBuffer() {}
  public VkBuffer(long value) {super(value);}
  public VkBuffer(FFMType.Uint64 value) {super(value);}
}
