package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkGraphicsPipelineCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkGraphicsPipelineCreateInfo.html
 *
 * @author pquiring
 */

public class VkGraphicsPipelineCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkPipelineCreateFlags */
  public int flags;
  /** */
  public int stageCount;
  /** */
  public VkPipelineShaderStageCreateInfo[] ptr_pStages;
  /** */
  public VkPipelineVertexInputStateCreateInfo ptr_pVertexInputState = new VkPipelineVertexInputStateCreateInfo();
  /** */
  public VkPipelineInputAssemblyStateCreateInfo ptr_pInputAssemblyState = new VkPipelineInputAssemblyStateCreateInfo();
  /** */
  public VkPipelineTessellationStateCreateInfo ptr_pTessellationState = new VkPipelineTessellationStateCreateInfo();
  /** */
  public VkPipelineViewportStateCreateInfo ptr_pViewportState = new VkPipelineViewportStateCreateInfo();
  /** */
  public VkPipelineRasterizationStateCreateInfo ptr_pRasterizationState = new VkPipelineRasterizationStateCreateInfo();
  /** */
  public VkPipelineMultisampleStateCreateInfo ptr_pMultisampleState = new VkPipelineMultisampleStateCreateInfo();
  /** */
  public VkPipelineDepthStencilStateCreateInfo ptr_pDepthStencilState = new VkPipelineDepthStencilStateCreateInfo();
  /** */
  public VkPipelineColorBlendStateCreateInfo ptr_pColorBlendState = new VkPipelineColorBlendStateCreateInfo();
  /** */
  public VkPipelineDynamicStateCreateInfo ptr_pDynamicState = new VkPipelineDynamicStateCreateInfo();
  /** VkPipelineLayout */
  public VkPipelineLayout layout = new VkPipelineLayout();
  /** VkRenderPass */
  public VkRenderPass renderPass = new VkRenderPass();
  /** */
  public int subpass;
  /** VkPipeline */
  public VkPipeline basePipelineHandle = new VkPipeline();
  /** */
  public int basePipelineIndex;
}
