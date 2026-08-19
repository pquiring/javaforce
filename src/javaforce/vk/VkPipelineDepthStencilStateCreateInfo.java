package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineDepthStencilStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineDepthStencilStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineDepthStencilStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineDepthStencilStateCreateFlags */
  public int flags;
  /** VkBool32 */
  public int depthTestEnable;
  /** VkBool32 */
  public int depthWriteEnable;
  /** */
  public VkCompareOp depthCompareOp = new VkCompareOp();
  /** VkBool32 */
  public int depthBoundsTestEnable;
  /** VkBool32 */
  public int stencilTestEnable;
  /** */
  public VkStencilOpState front = new VkStencilOpState();
  /** */
  public VkStencilOpState back = new VkStencilOpState();;
  /** */
  public float minDepthBounds;
  /** */
  public float maxDepthBounds;
}
