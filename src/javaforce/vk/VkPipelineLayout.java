package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineLayout.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineLayout.html
 *
 * @author pquiring
 */

public class VkPipelineLayout extends FFMType.Uint64 {
  public VkPipelineLayout() {}
  public VkPipelineLayout(long value) {super(value);}
  public VkPipelineLayout(FFMType.Uint64 value) {super(value);}
}
