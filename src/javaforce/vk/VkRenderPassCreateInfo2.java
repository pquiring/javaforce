package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkRenderPassCreateInfo2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkRenderPassCreateInfo2.html
 *
 * @author pquiring
 */

public class VkRenderPassCreateInfo2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO_2;
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
  /** */
  public int correlatedViewMaskCount;
  /** */
  public int[] ptr_pCorrelatedViewMasks;
}
