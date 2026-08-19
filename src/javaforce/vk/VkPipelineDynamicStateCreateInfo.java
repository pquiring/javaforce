package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineDynamicStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineDynamicStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineDynamicStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineDynamicStateCreateFlags */
  public int flags;
  /** */
  public int dynamicStateCount;
  /** */
  public VkDynamicState[] ptr_pDynamicStates;
}
