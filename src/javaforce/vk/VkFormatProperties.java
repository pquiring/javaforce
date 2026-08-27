package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFormatProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFormatProperties.html
 *
 * @author pquiring
 */

public class VkFormatProperties extends FFMStruct {
  /** VkFormatFeatureFlags */
  public int linearTilingFeatures;
  /** VkFormatFeatureFlags */
  public int optimalTilingFeatures;
  /** VkFormatFeatureFlags */
  public int bufferFeatures;
}
