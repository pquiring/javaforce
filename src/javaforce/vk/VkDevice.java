package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDevice.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDevice.html
 *
 * @author pquiring
 */

public class VkDevice extends FFMType.Uint64 {
  public VkDevice() {}
  public VkDevice(long value) {super(value);}
  public VkDevice(FFMType.Uint64 value) {super(value);}
}
