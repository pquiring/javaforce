package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineViewportStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineViewportStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineViewportStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineViewportStateCreateFlags */
  public int flags;
  /** */
  public int viewportCount;
  /** */
  public VkViewport[] ptr_pViewports;
  /** */
  public int scissorCount;
  /** */
  public VkRect2D[] ptr_pScissors;
}
