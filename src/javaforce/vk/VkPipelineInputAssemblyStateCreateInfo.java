package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineInputAssemblyStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineInputAssemblyStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineInputAssemblyStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkPipelineInputAssemblyStateCreateFlags */
  public int flags;
  /** */
  public VkPrimitiveTopology topology = new VkPrimitiveTopology();
  /** VkBool32 */
  public int primitiveRestartEnable;
}
