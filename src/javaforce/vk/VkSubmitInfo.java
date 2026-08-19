package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSubmitInfo.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSubmitInfo.html
 *
 * @author pquiring
 */

public class VkSubmitInfo extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_SUBMIT_INFO;
  /** pNext */
  public long pNext;
  /** */
  public int waitSemaphoreCount;
  /** */
  public VkSemaphore[] ptr_pWaitSemaphores;
  /** VkPipelineStageFlags */
  public int[] ptr_pWaitDstStageMask;
  /** */
  public int commandBufferCount;
  /** */
  public VkCommandBuffer[] ptr_pCommandBuffers;
  /** */
  public int signalSemaphoreCount;
  /** */
  public VkSemaphore[] ptr_pSignalSemaphores;
}
