package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPipelineRasterizationStateCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineRasterizationStateCreateInfo.html
 *
 * @author pquiring
 */

public class VkPipelineRasterizationStateCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkPipelineRasterizationStateCreateFlags */
  public int flags;
  /** VkBool32 */
  public int depthClampEnable;
  /** VkBool32 */
  public int rasterizerDiscardEnable;
  /** */
  public VkPolygonMode polygonMode = new VkPolygonMode();
  /** */
  public int cullMode;
  /** */
  public VkFrontFace frontFace = new VkFrontFace();
  /** VkBool32 */
  public int depthBiasEnable;
  /** */
  public float depthBiasConstantFactor;
  /** */
  public float depthBiasClamp;
  /** */
  public float depthBiasSlopeFactor;
  /** */
  public float lineWidth;
}
