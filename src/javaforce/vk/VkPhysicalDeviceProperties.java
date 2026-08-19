package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPhysicalDeviceProperties.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDeviceProperties.html
 *
 * @author pquiring
 */

public class VkPhysicalDeviceProperties extends FFMStruct {
  /** apiVersion */
  public int apiVersion;
  /** driverVersion */
  public int driverVersion;
  /** vendorID */
  public int vendorID;
  /** deviceID */
  public int deviceID;
  /** deviceType (enum) */
  public int deviceType;
  /** deviceName */
  public byte[] deviceName = new byte[256];
  /** pipelineCacheUUID */
  public byte[] pipelineCacheUUID = new byte[16];
  /** limits */
  public VkPhysicalDeviceLimits limits = new VkPhysicalDeviceLimits();
  /** sparseProperties */
  public int[] sparseProperties = new int[5];
}
