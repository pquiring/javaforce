package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkStencilOp.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkStencilOp.html
 *
 * @author pquiring
 */

public class VkStencilOp extends FFMType.Uint64 {
  public VkStencilOp() {}
  public VkStencilOp(long value) {super(value);}
  public VkStencilOp(FFMType.Uint64 value) {super(value);}
}
