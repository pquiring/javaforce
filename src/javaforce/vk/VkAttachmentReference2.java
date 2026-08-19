package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkAttachmentReference2.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkAttachmentReference2.html
 *
 * @author pquiring
 */

public class VkAttachmentReference2 extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_ATTACHMENT_REFERENCE_2;
  /** reserved */
  public long pNext;
  /** */
  public int attachment;
  /** VkImageLayout enum */
  public VkImageLayout layout = new VkImageLayout();
  /** VkImageAspectFlags */
  public int aspectMask;
}
