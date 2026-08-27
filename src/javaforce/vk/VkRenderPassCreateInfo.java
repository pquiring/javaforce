package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkRenderPassCreateInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkRenderPassCreateInfo.html
 *
 * @author pquiring
 */

public class VkRenderPassCreateInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
  /** reserved */
  public long pNext;
  /** VkRenderPassCreateFlags */
  public int flags;
  /** */
  public int attachmentCount;
  /** */
  public VkAttachmentDescription[] ptr_pAttachments;
  /** */
  public int subpassCount;
  /** */
  public VkSubpassDescription[] ptr_pSubpasses;
  /** */
  public int dependencyCount;
  /** */
  public VkSubpassDependency[] ptr_pDependencies;
}
