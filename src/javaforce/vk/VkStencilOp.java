package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkStencilOp.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkStencilOp.html
 *
 * @author pquiring
 */

public class VkStencilOp extends FFMType.Uint32 {
  public VkStencilOp() {}
  public VkStencilOp(int value) {super(value);}
  public VkStencilOp(FFMType.Uint32 value) {super(value);}
}
