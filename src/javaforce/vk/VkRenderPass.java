package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkRenderPass.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkRenderPass.html
 *
 * @author pquiring
 */

public class VkRenderPass extends FFMType.Uint64 {
  public VkRenderPass() {}
  public VkRenderPass(long value) {super(value);}
  public VkRenderPass(FFMType.Uint64 value) {super(value);}
}
