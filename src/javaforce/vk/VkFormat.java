package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFormat (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFormat.html
 *
 * @author pquiring
 */

public class VkFormat extends FFMType.Uint32 {
  public VkFormat() {}
  public VkFormat(int value) {super(value);}
  public VkFormat(FFMType.Uint32 value) {super(value);}
}
