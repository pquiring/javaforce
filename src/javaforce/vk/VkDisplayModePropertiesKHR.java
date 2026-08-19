package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDisplayModePropertiesKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDisplayModePropertiesKHR.html
 *
 * @author pquiring
 */

public class VkDisplayModePropertiesKHR extends FFMStruct {
  /** VkDisplayModeKHR */
  public long displayMode;
  /** VkDisplayModeParametersKHR */
  public VkDisplayModeParametersKHR parameters;
}
