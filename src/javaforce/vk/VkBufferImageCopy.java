package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkBufferImageCopy.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkBufferImageCopy.html
 *
 * @author pquiring
 */

public class VkBufferImageCopy extends FFMStruct {
  /** */
  public VkDeviceSize bufferOffset = new VkDeviceSize();
  /** */
  public int bufferRowLength;
  /** */
  public int bufferImageHeight;
  /** */
  public VkImageSubresourceLayers imageSubresource = new VkImageSubresourceLayers();
  /** */
  public VkOffset3D imageOffset = new VkOffset3D();
  /** */
  public VkExtent3D imageExtent = new VkExtent3D();
}
