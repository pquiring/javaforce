package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSurfaceFormat2KHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSurfaceFormat2KHR.html
 *
 * @author pquiring
 */

public class VkSurfaceFormat2KHR extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_SURFACE_FORMAT_2_KHR;
  /** pNext */
  public long pNext;
  /** */
  public VkSurfaceFormatKHR surfaceFormat;
}
