package javaforce.vk;

import javaforce.*;
import javaforce.ffm.*;

/** VkSwapchainCreateInfoKHR.
 *
 * See https://docs.vulkan.org/refpages/latest/refpages/source/VkSwapchainCreateInfoKHR.html
 *
 * @author pquiring
 */

public class VkSwapchainCreateInfoKHR extends FFMStruct {
  /** VKStructureType */
  public int sType = VK.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
  /** pNext */
  public long pNext;
  /** VkSwapchainCreateFlagsKHR */
  public int flags;
  /** VkSurfaceKHR */
  public VkSurfaceKHR surface = new VkSurfaceKHR();
  public int minImageCount;
  /** VkFormat enum */
  public VkFormat imageFormat = new VkFormat();
  /** VkColorSpaceKHR enum */
  public VkColorSpaceKHR imageColorSpace = new VkColorSpaceKHR();
  /** */
  public VkExtent2D imageExtent = new VkExtent2D();
  /** */
  public int imageArrayLayers;
  /** VkImageUsageFlags */
  public int imageUsage;
  /** VkSharingMode enum */
  public int imageSharingMode;
  /** */
  public int queueFamilyIndexCount;
  /** */
  public int[] ptr_pQueueFamilyIndices;
  /** VkSurfaceTransformFlagBitsKHR */
  public int preTransform;
  /** VkCompositeAlphaFlagBitsKHR */
  public int compositeAlpha;
  /** VkPresentModeKHR */
  public VkPresentModeKHR presentMode = new VkPresentModeKHR();
  /** VkBool32 */
  public int clipped;
  /** VkSwapchainKHR */
  public VkSwapchainKHR oldSwapchain = new VkSwapchainKHR();
}
