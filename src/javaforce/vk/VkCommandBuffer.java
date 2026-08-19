package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCommandBuffer.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCommandBuffer.html
 *
 * @author pquiring
 */

public class VkCommandBuffer extends FFMType.Uint64 {
  public VkCommandBuffer() {}
  public VkCommandBuffer(long value) {super(value);}
  public VkCommandBuffer(FFMType.Uint64 value) {super(value);}
}
