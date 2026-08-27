package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkImageBlit.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkImageBlit.html
 *
 * @author pquiring
 */

public class VkImageBlit extends FFMStruct {
  public VkImageBlit() {
    for(int a=0;a<2;a++) {
      srcOffsets[a] = new VkOffset3D();
      dstOffsets[a] = new VkOffset3D();
    }
  }
  /** */
  public VkImageSubresourceLayers srcSubresource = new VkImageSubresourceLayers();
  /** */
  public VkOffset3D[] srcOffsets = new VkOffset3D[2];
  /** */
  public VkImageSubresourceLayers dstSubresource = new VkImageSubresourceLayers();
  /** */
  public VkOffset3D[] dstOffsets = new VkOffset3D[2];
}
