package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkViewport.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkViewport.html
 *
 * @author pquiring
 */

public class VkViewport extends FFMStruct {
  /** */
  public float x;
  /** */
  public float y;
  /** */
  public float width;
  /** */
  public float height;
  /** */
  public float minDepth;
  /** */
  public float maxDepth;
}
