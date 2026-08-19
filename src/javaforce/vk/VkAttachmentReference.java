package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkAttachmentReference.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkAttachmentReference.html
 *
 * @author pquiring
 */

public class VkAttachmentReference extends FFMStruct {
  /** */
  public int attachment;
  /** VkImageLayout enum */
  public VkImageLayout layout = new VkImageLayout();
}
