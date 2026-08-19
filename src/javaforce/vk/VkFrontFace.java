package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkFrontFace (enum).
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkFrontFace.html
 *
 * @author pquiring
 */

public class VkFrontFace extends FFMType.Uint32 {
  public VkFrontFace() {}
  public VkFrontFace(int value) {super(value);}
  public VkFrontFace(FFMType.Uint32 value) {super(value);}
}
