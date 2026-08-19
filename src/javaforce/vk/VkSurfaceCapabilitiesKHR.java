package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSurfaceCapabilitiesKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSurfaceCapabilitiesKHR.html
 *
 * @author pquiring
 */

public class VkSurfaceCapabilitiesKHR extends FFMStruct {
  /** */
  public int minImageCount;
  /** */
  public int maxImageCount;
  /** */
  public VkExtent2D currentExtent = new VkExtent2D();
  /** */
  public VkExtent2D minImageExtent = new VkExtent2D();
  /** */
  public VkExtent2D maxImageExtent = new VkExtent2D();
  public int maxImageArrayLayers;
  /** VkSurfaceTransformFlagsKHR */
  public int supportedTransforms;
  /** VkSurfaceTransformFlagBitsKHR */
  public int currentTransform;
  /** VkCompositeAlphaFlagsKHR */
  public int supportedCompositeAlpha;
  /** VkImageUsageFlags */
  public int supportedUsageFlags;
}
