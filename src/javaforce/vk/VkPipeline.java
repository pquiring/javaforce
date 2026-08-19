package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipeline.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipeline.html
 *
 * @author pquiring
 */

public class VkPipeline extends FFMType.Uint64 {
  public VkPipeline() {}
  public VkPipeline(long value) {super(value);}
  public VkPipeline(FFMType.Uint64 value) {super(value);}
}
