package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCompareOp (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCompareOp.html
 *
 * @author pquiring
 */

public class VkCompareOp extends FFMType.Uint32 {
  public VkCompareOp() {}
  public VkCompareOp(int value) {super(value);}
  public VkCompareOp(FFMType.Uint32 value) {super(value);}
}
