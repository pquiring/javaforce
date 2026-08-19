package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPrimitiveTopology (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPrimitiveTopology.html
 *
 * @author pquiring
 */

public class VkPrimitiveTopology extends FFMType.Uint32 {
  public VkPrimitiveTopology() {}
  public VkPrimitiveTopology(int value) {super(value);}
  public VkPrimitiveTopology(FFMType.Uint32 value) {super(value);}
}
