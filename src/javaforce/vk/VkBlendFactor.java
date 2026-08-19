package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBlendFactor (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBlendFactor.html
 *
 * @author pquiring
 */

public class VkBlendFactor extends FFMType.Uint32 {
  public VkBlendFactor() {}
  public VkBlendFactor(int value) {super(value);}
  public VkBlendFactor(FFMType.Uint32 value) {super(value);}
}
