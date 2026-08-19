package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineTessellationStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineTessellationStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineTessellationStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineTessellationStateCreateFlags */
  public int flags;
  /** */
  public int patchControlPoints;
}
