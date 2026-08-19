package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkCommandBufferInheritanceInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkCommandBufferInheritanceInfo.html
 *
 * @author pquiring
 */

public class VkCommandBufferInheritanceInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO;
  /** pNext */
  public long pNext;
  /** VkRenderPass */
  public long renderPass;
  /** */
  public int subpass;
  /** VkFramebuffer */
  public int framebuffer;
  /** VkBool32 */
  public int occlusionQueryEnable;
  /** VkQueryControlFlags */
  public int queryFlags;
  /** VkQueryPipelineStatisticFlags */
  public int pipelineStatistics;
}
