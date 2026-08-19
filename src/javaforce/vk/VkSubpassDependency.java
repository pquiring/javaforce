package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSubpassDependency.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSubpassDependency.html
 *
 * @author pquiring
 */

public class VkSubpassDependency extends FFMStruct {
  /** */
  public int srcSubpass;
  /** */
  public int dstSubpass;
  /** */
  public int srcStageMask;
  /** */
  public int dstStageMask;
  /** */
  public int srcAccessMask;
  /** */
  public int dstAccessMask;
  /** */
  public int dependencyFlags;
}
