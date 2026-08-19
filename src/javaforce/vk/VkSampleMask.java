package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSampleMask (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSampleMask.html
 *
 * @author pquiring
 */

public class VkSampleMask extends FFMType.Uint32 {
  public VkSampleMask() {}
  public VkSampleMask(int value) {super(value);}
  public VkSampleMask(FFMType.Uint32 value) {super(value);}
}
