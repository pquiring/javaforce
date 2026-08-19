package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineColorBlendAttachmentState.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineColorBlendAttachmentState.html
 *
 * @author pquiring
 */

public class VkPipelineColorBlendAttachmentState extends FFMStruct {
  /** VkBool32 */
  public int blendEnable;
  /** */
  public VkBlendFactor srcColorBlendFactor = new VkBlendFactor();
  /** */
  public VkBlendFactor dstColorBlendFactor = new VkBlendFactor();
  /** */
  public VkBlendOp colorBlendOp = new VkBlendOp();
  /** */
  public VkBlendFactor srcAlphaBlendFactor = new VkBlendFactor();
  /** */
  public VkBlendFactor dstAlphaBlendFactor = new VkBlendFactor();
  /** */
  public VkBlendOp alphaBlendOp = new VkBlendOp();
  /** VkColorComponentFlags */
  public int colorWriteMask;
}
