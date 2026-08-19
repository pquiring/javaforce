package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDevice.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDevice.html
 *
 * @author pquiring
 */

public class VkPhysicalDevice extends FFMType.Uint64 {
  public VkPhysicalDevice() {}
  public VkPhysicalDevice(long value) {super(value);}
  public VkPhysicalDevice(FFMType.Uint64 value) {super(value);}
}
