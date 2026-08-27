package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFilter (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFilter.html
 *
 * @author pquiring
 */

public class VkFilter extends FFMType.Uint32 {
  public VkFilter() {}
  public VkFilter(int value) {super(value);}
  public VkFilter(FFMType.Uint32 value) {super(value);}
}
