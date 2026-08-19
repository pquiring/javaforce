package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkComponentMapping.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkComponentMapping.html
 *
 * @author pquiring
 */

public class VkComponentMapping extends FFMStruct {
  /** VkComponentSwizzle enum */
  public int r;
  /** VkComponentSwizzle enum */
  public int g;
  /** VkComponentSwizzle enum */
  public int b;
  /** VkComponentSwizzle enum */
  public int a;
}
