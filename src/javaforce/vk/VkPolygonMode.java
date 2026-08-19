package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPolygonMode (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPolygonMode.html
 *
 * @author pquiring
 */

public class VkPolygonMode extends FFMType.Uint32 {
  public VkPolygonMode() {}
  public VkPolygonMode(int value) {super(value);}
  public VkPolygonMode(FFMType.Uint32 value) {super(value);}
}
