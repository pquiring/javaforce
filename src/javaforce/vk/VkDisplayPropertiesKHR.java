package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDisplayPropertiesKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDisplayPropertiesKHR.html
 *
 * @author pquiring
 */

public class VkDisplayPropertiesKHR extends FFMStruct {
  /** */
  public long VkDisplayKHR;
  /** */
  public byte[] ptr_displayName;
  /** */
  public VkExtent2D physicalDimensions = new VkExtent2D();
  /** */
  public VkExtent2D physicalResolution = new VkExtent2D();
  /** VkSurfaceTransformFlagsKHR */
  public int supportedTransforms;
  /** VkBool32 */
  public int planeReorderPossible;
  /** VkBool32 */
  public int persistentContent;
}
