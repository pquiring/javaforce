package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkShaderModule.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkShaderModule.html
 *
 * @author pquiring
 */

public class VkShaderModule extends FFMType.Uint64 {
  public VkShaderModule() {}
  public VkShaderModule(long value) {super(value);}
  public VkShaderModule(FFMType.Uint64 value) {super(value);}
}
