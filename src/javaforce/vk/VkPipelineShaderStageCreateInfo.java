package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineShaderStageCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineShaderStageCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineShaderStageCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkPipelineShaderStageCreateFlags */
  public int flags;
  /** VkShaderStageFlagBits */
  public int stage;
  /**  */
  public VkShaderModule module = new VkShaderModule();
  /**  */
  public String ptr_pName;
  /** */
  public VkSpecializationInfo ptr_pSpecializationInfo = new VkSpecializationInfo();
}
