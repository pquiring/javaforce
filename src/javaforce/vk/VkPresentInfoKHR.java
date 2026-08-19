package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkPresentInfoKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkPresentInfoKHR.html
 *
 * @author pquiring
 */

public class VkPresentInfoKHR extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
  /** pNext */
  public long pNext;
  /** */
  public int waitSemaphoreCount;
  /** */
  public VkSemaphore[] ptr_pWaitSemaphores;
  /** */
  public int swapchainCount;
  /** */
  public VkSwapchainKHR[] ptr_pSwapchains;
  /** */
  public int[] ptr_pImageIndices;
  /** */
  public int[] ptr_pResults;
}
