package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkQueue.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkQueue.html
 *
 * @author pquiring
 */

public class VkQueue extends FFMType.Uint64 {
  public VkQueue() {}
  public VkQueue(long value) {super(value);}
  public VkQueue(FFMType.Uint64 value) {super(value);}
}
