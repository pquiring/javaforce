package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSampleCountFlagBits.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSampleCountFlagBits.html
 *
 * @author pquiring
 */

public class VkSampleCountFlagBits extends FFMType.Uint32 {
  public VkSampleCountFlagBits() {}
  public VkSampleCountFlagBits(int value) {super(value);}
  public VkSampleCountFlagBits(FFMType.Uint32 value) {super(value);}
}
