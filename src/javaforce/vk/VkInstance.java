package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkInstance.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkInstance.html
 *
 * @author pquiring
 */

public class VkInstance extends FFMType.Uint64 {
  public VkInstance() {}
  public VkInstance(long value) {super(value);}
  public VkInstance(FFMType.Uint64 value) {super(value);}
}
