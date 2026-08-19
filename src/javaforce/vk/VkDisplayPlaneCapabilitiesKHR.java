package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkDisplayPlaneCapabilitiesKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkDisplayPlaneCapabilitiesKHR.html
 *
 * @author pquiring
 */

public class VkDisplayPlaneCapabilitiesKHR extends FFMStruct {
  /** VkDisplayPlaneAlphaFlagsKHR */
  public int displayMode;
  /** */
  public VkOffset2D minSrcPosition = new VkOffset2D();
  /** */
  public VkOffset2D maxSrcPosition = new VkOffset2D();
  /** */
  public VkExtent2D minSrcExtent = new VkExtent2D();
  /** */
  public VkExtent2D maxSrcExtent = new VkExtent2D();
  /** */
  public VkOffset2D minDstPosition = new VkOffset2D();
  /** */
  public VkOffset2D maxDstPosition = new VkOffset2D();
  /** */
  public VkExtent2D minDstExtent = new VkExtent2D();
  /** */
  public VkExtent2D maxDstExtent = new VkExtent2D();
}
