package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkComputePipelineCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkComputePipelineCreateInfo.html
 *
 * @author pquiring
 */

public class VkComputePipelineCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineCreateFlags */
  public int flags;
  /** */
  public VkPipelineShaderStageCreateInfo stage = new VkPipelineShaderStageCreateInfo();
  /** */
  public VkPipelineLayout layout = new VkPipelineLayout();
  /** */
  public VkPipeline basePipelineHandle = new VkPipeline();
  /** */
  public int basePipelineIndex;
}
