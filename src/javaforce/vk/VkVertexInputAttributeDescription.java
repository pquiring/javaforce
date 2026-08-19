package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkVertexInputAttributeDescription.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkVertexInputAttributeDescription.html
 *
 * @author pquiring
 */

public class VkVertexInputAttributeDescription extends FFMStruct {
  /** */
  public int width;
  /** */
  public int height;
  /** */
  public VkFormat format = new VkFormat();
  /** */
  public int offset;
}
