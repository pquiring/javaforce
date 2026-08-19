package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBlendOp (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBlendOp.html
 *
 * @author pquiring
 */

public class VkBlendOp extends FFMType.Uint32 {
  public VkBlendOp() {}
  public VkBlendOp(int value) {super(value);}
  public VkBlendOp(FFMType.Uint32 value) {super(value);}
}
