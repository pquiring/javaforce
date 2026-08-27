package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkStencilOpState.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkStencilOpState.html
 *
 * @author pquiring
 */

public class VkStencilOpState extends FFMStruct {
  /** */
  public VkStencilOp failOp = new VkStencilOp();
  /** */
  public VkStencilOp passOp = new VkStencilOp();
  /** */
  public VkStencilOp depthFailOp = new VkStencilOp();
  /** */
  public VkCompareOp compareOp = new VkCompareOp();
  /** */
  public int compareMask;
  /** */
  public int writeMask;
  /** */
  public int reference;
}
