package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDeviceMemory.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDeviceMemory.html
 *
 * @author pquiring
 */

public class VkDeviceMemory extends FFMType.Uint64 {
  public VkDeviceMemory() {}
  public VkDeviceMemory(long value) {super(value);}
  public VkDeviceMemory(FFMType.Uint64 value) {super(value);}
}
