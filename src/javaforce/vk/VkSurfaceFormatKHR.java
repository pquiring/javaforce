package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSurfaceFormatKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSurfaceFormatKHR.html
 *
 * @author pquiring
 */

public class VkSurfaceFormatKHR extends FFMStruct {
  /** VkFormat enum. */
  public VkFormat format = new VkFormat();
  /** VkColorSpaceKHR enum */
  public VkColorSpaceKHR colorSpace = new VkColorSpaceKHR();
}
