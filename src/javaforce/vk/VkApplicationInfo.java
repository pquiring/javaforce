package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkApplicationInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkApplicationInfo.html
 *
 * Version number format:
 * See https://docs.vulkan.org/spec/latest/chapters/extensions.html#extendingvulkan-coreversions-versionnumbers
 *
 * @author pquiring
 */

public class VkApplicationInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_APPLICATION_INFO;
  /** reserved */
  public long pNext;
  /** */
  public String ptr_pApplicationName;
  /** */
  public int applicationVersion;
  /** */
  public String ptr_pEngineName;
  /** */
  public int engineVersion;
  /** */
  public int apiVersion;
}
