package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDisplayModeParametersKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDisplayModeParametersKHR.html
 *
 * @author pquiring
 */

public class VkDisplayModeParametersKHR extends FFMStruct {
  /** */
  public VkExtent2D visibleRegion = new VkExtent2D();
  /** */
  public VkDisplayModeParametersKHR refreshRate;
}
