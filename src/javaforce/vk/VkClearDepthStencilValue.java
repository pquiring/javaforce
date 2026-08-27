package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkClearDepthStencilValue.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkClearDepthStencilValue.html
 *
 * @author pquiring
 */

public class VkClearDepthStencilValue extends FFMStruct {
  /** */
  public float depth;
  /** */
  public int stencil;
}
