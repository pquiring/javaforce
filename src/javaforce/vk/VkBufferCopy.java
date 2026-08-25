package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBufferCopy.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBufferCopy.html
 *
 * @author pquiring
 */

public class VkBufferCopy extends FFMStruct {
  /** */
  public VkDeviceSize srcOffset = new VkDeviceSize();
  /** */
  public VkDeviceSize dstOffset = new VkDeviceSize();
  /** */
  public VkDeviceSize size = new VkDeviceSize();
}
