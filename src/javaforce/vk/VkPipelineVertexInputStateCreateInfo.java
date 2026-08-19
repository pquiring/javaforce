package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineVertexInputStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineVertexInputStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineVertexInputStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkPipelineVertexInputStateCreateFlags */
  public int flags;
  /** */
  public int vertexBindingDescriptionCount;
  /** */
  public VkVertexInputBindingDescription pVertexBindingDescriptions = new VkVertexInputBindingDescription();
  /** */
  public int vertexAttributeDescriptionCount;
  /** */
  public VkVertexInputAttributeDescription pVertexAttributeDescriptions = new VkVertexInputAttributeDescription();
}
