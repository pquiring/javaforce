package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFramebufferCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFramebufferCreateInfo.html
 *
 * @author pquiring
 */

public class VkFramebufferCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
  /** pNext */
  public long pNext;
  /** VkFramebufferCreateFlags */
  public int flags;
  /** VkRenderPass */
  public VkRenderPass renderPass = new VkRenderPass();
  /** */
  public int attachmentCount;
  /** */
  public VkImageView[] ptr_pAttachments;
  /** */
  public int width;
  /** */
  public int height;
  /** */
  public int layers;
}
