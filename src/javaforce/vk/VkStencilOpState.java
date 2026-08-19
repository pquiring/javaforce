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
  public VkStencilOp failOp;
  /** */
  public VkStencilOp passOp;
  /** */
  public VkStencilOp depthFailOp;
  /** */
  public VkCompareOp compareOp;
  /** */
  public int compareMask;
  /** */
  public int writeMask;
  /** */
  public int reference;
}
