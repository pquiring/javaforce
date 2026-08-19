package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSubpassDescription.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSubpassDescription.html
 *
 * @author pquiring
 */

public class VkSubpassDescription extends FFMStruct {
  /** */
  public int flags;
  /** VkPipelineBindPoint enum */
  public int pipelineBindPoint;
  /** */
  public int viewMask;
  /** */
  public int inputAttachmentCount;
  /** */
  public VkAttachmentReference[] ptr_pInputAttachments;
  /** */
  public int colorAttachmentCount;
  /** */
  public VkAttachmentReference[] ptr_pColorAttachments;
  /** */
  public VkAttachmentReference[] ptr_pResolveAttachments;
  /** */
  public VkAttachmentReference[] ptr_pDepthStencilAttachment;
  /** */
  public int preserveAttachmentCount;
  /** */
  public int[] ptr_pPreserveAttachments;
}
