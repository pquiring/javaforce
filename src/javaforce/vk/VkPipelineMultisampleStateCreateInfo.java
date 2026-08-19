package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineMultisampleStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineMultisampleStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineMultisampleStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineMultisampleStateCreateFlags */
  public int flags;
  /** VkSampleCountFlagBits */
  public int rasterizationSamples;
  /** VkBool32 */
  public int sampleShadingEnable;
  /** */
  public float minSampleShading;
  /** */
  public VkSampleMask ptr_pSampleMask = new VkSampleMask();
  /** VkBool32 */
  public int alphaToCoverageEnable;
  /** VkBool32 */
  public int alphaToOneEnable;
}
