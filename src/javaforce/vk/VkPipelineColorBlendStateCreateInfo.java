package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineColorBlendStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineColorBlendStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineColorBlendStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineColorBlendStateCreateFlags */
  public int flags;
  /** VkBool32 */
  public int logicOpEnable;
  /** */
  public VkLogicOp logicOp = new VkLogicOp();
  /** */
  public int attachmentCount;
  /** */
  public VkPipelineColorBlendAttachmentState ptr_pAttachments = new VkPipelineColorBlendAttachmentState();
  /** */
  public float[] blendConstants = new float[4];
}
