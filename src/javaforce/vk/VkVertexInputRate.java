package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkVertexInputRate (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkVertexInputRate.html
 *
 * @author pquiring
 */

public class VkVertexInputRate extends FFMType.Uint32 {
  public VkVertexInputRate() {}
  public VkVertexInputRate(int value) {super(value);}
  public VkVertexInputRate(FFMType.Uint32 value) {super(value);}
}
