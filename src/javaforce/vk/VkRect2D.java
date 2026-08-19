package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkRect2D.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkRect2D.html
 *
 * @author pquiring
 */

public class VkRect2D extends FFMStruct {
  /** */
  public VkOffset2D offset = new VkOffset2D();
  /** */
  public VkExtent2D extent = new VkExtent2D();
}
