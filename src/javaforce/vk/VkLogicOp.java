package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkLogicOp (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkLogicOp.html
 *
 * @author pquiring
 */

public class VkLogicOp extends FFMType.Uint32 {
  public VkLogicOp() {}
  public VkLogicOp(int value) {super(value);}
  public VkLogicOp(FFMType.Uint32 value) {super(value);}
}
