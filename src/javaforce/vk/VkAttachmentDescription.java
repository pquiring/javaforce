package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkAttachmentDescription.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkAttachmentDescription.html
 *
 * @author pquiring
 */

public class VkAttachmentDescription extends FFMStruct {
  /** VkAttachmentDescriptionFlags */
  public int flags;
  /** VkFormat enum */
  public VkFormat format = new VkFormat();
  /** VkSampleCountFlagBits enum */
  public int samples;
  /** VkAttachmentLoadOp enum */
  public int loadOp;
  /** VkAttachmentStoreOp enum */
  public int storeOp;
  /** VkAttachmentLoadOp enum */
  public int stencilLoadOp;
  /** VkAttachmentStoreOp enum */
  public int stencilStoreOp;
  /** VkImageLayout enum */
  public VkImageLayout initialLayout = new VkImageLayout();
  /** VkImageLayout enum */
  public VkImageLayout finalLayout = new VkImageLayout();
}
