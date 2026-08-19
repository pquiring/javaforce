package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFramebuffer.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFramebuffer.html
 *
 * @author pquiring
 */

public class VkFramebuffer extends FFMType.Uint64 {
  public VkFramebuffer() {}
  public VkFramebuffer(long value) {super(value);}
  public VkFramebuffer(FFMType.Uint64 value) {super(value);}
}
