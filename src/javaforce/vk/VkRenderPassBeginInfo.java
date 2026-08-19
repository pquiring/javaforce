package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkRenderPassBeginInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkRenderPassBeginInfo.html
 *
 * @author pquiring
 */

public class VkRenderPassBeginInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
  /** pNext */
  public long pNext;
  /** */
  public VkRenderPass renderPass = new VkRenderPass();
  /** */
  public VkFramebuffer framebuffer = new VkFramebuffer();
  /** */
  public VkRect2D renderArea = new VkRect2D();
  /** */
  public int clearValueCount;
  /** */
  public VkColor[] ptr_pClearValues;
}
