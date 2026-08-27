package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkClearValue (union).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkClearValue.html
 *
 * @author pquiring
 */

public class VkClearValue extends FFMStruct.Union {
  /** */
  public VkClearColorValue color = new VkClearColorValue();
  /** */
  public VkClearDepthStencilValue depthStencil = new VkClearDepthStencilValue();
}
