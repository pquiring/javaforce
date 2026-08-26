package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineLayoutCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineLayoutCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineLayoutCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineLayoutCreateFlags */
  public int flags;
  /** */
  public int setLayoutCount;
  /** */
  public VkDescriptorSetLayout[] ptr_pSetLayouts;
  /** */
  public int pushConstantRangeCount;
  /** */
  public VkPushConstantRange ptr_pPushConstantRanges = new VkPushConstantRange();
}
