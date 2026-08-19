package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineCache.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineCache.html
 *
 * @author pquiring
 */

public class VkPipelineCache extends FFMType.Uint64 {
  public VkPipelineCache() {}
  public VkPipelineCache(long value) {super(value);}
  public VkPipelineCache(FFMType.Uint64 value) {super(value);}
}
