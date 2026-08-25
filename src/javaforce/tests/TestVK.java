package javaforce.tests;

import java.lang.foreign.*;
import java.util.*;

import javaforce.*;
import javaforce.ui.*;
import javaforce.gl.*;
import javaforce.vk.*;
import static javaforce.vk.VK.*;

/** Vulkan Test.
 *
 * Currently supports tutorial #16 at https://vulkan-tutorial.com/
 *
 * @author pquiring
 */

public class TestVK implements WindowEvents {
  public static boolean debug = false;
  public static boolean debug_ext = false;

  public static Window window;
  public static Cube cube;
  public static VK vk;
  public static boolean active = true;

  public static void main(String args[]) {
    try {
      window = new Window();
      window.create(Window.STYLE_VISIBLE | Window.STYLE_TITLEBAR | Window.STYLE_RESIZABLE | Window.STYLE_VULKAN, "Test", 512, 512, null);
      window.show();
      window.setWindowListener(new TestVK());
      vk = VK.getInstance();
      boolean supported = vk.isVulkanSupported();
      JFLog.log("vulkan supported=" + supported);
      if (!supported) return;
      cube = new Cube(false);
      cube.init();
      while (active) {
        window.pollEvents();
      }
      cube.uninit();
      cube.cleanup();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void swap() {
    window.swap();
  }

  public void windowResize(int x, int y) {
  }

  public void windowClosing() {
    active = false;
  }

  public static class QueueFamilyIndices {
    Integer graphicsFamily = null;
    Integer presentFamily = null;

    boolean isComplete() {
      return graphicsFamily != null && presentFamily != null;
    }
  };

  public static class SwapChainSupportDetails {
    VkSurfaceCapabilitiesKHR capabilities = new VkSurfaceCapabilitiesKHR();
    VkSurfaceFormatKHR[] formats;
    VkPresentModeKHR[] presentModes;
  };

  public static String[] desiredExtensions = {
    VK_KHR_swapchain,
  };

  public static String[] fullExtensions;

  public static class Vertex {
    public Vector2 pos;
    public Vector3 color;

    public Vertex(float[] pos, float[] color) {
      this.pos = new Vector2(pos);
      this.color = new Vector3(color);
    }

    public static VkVertexInputBindingDescription[] getBindingDescriptions() {
      VkVertexInputBindingDescription bindingDescription = new VkVertexInputBindingDescription();
      bindingDescription.binding = 0;
      bindingDescription.stride = 4 * 5;  //sizeof(float) * (2 + 3)
      bindingDescription.inputRate.setValue(VK_VERTEX_INPUT_RATE_VERTEX);

      return new VkVertexInputBindingDescription[] {bindingDescription};
    }

    public static VkVertexInputAttributeDescription[] getAttributeDescriptions() {
      VkVertexInputAttributeDescription[] attributeDescriptions = new VkVertexInputAttributeDescription[2];

      for(int i=0;i<2;i++) {
        attributeDescriptions[i] = new VkVertexInputAttributeDescription();
      }

      attributeDescriptions[0].binding = 0;
      attributeDescriptions[0].location = 0;
      attributeDescriptions[0].format.setValue(VK_FORMAT_R32G32_SFLOAT);
      attributeDescriptions[0].offset =  0;

      attributeDescriptions[1].binding = 0;
      attributeDescriptions[1].location = 1;
      attributeDescriptions[1].format.setValue(VK_FORMAT_R32G32B32_SFLOAT);
      attributeDescriptions[1].offset = 4 * 2;

      return attributeDescriptions;
    }

  }

  public static class Cube {
    Object renderLock = new Object();
    java.util.Timer glTimer, fpsTimer;
    Object fpsLock = new Object();
    int fpsCounter;

    final int FPS = 65;
    final int max_frames = 2;  //# of concurrent frames to keep in the render pipeline (in flight)
    int current_frame = 0;

    //vulkan data
    VkInstance instance;
    VkSurfaceKHR surface;
    VkPhysicalDevice physicalDevice;
    VkDevice device;  //logical
    VkQueue graphicsQueue;
    VkQueue presentQueue;
    VkSwapchainKHR swapChain;
    VkImage[] swapChainImages;
    VkFormat swapChainImageFormat;
    VkExtent2D swapChainExtent;
    VkImageView[] swapChainImageViews;
    VkFramebuffer[] swapChainFramebuffers;
    VkRenderPass renderPass;

    VkPipelineLayout pipelineLayout;
    VkPipeline graphicsPipeline;

    VkCommandPool commandPool;
    VkCommandBuffer[] commandBuffers;

    VkBuffer vertexBuffer;
    VkDeviceMemory vertexBufferMemory;
    VkBuffer indexBuffer;
    VkDeviceMemory indexBufferMemory;

    VkSemaphore[] imageAvailableSemaphores;
    VkSemaphore[] renderFinishedSemaphores;
    VkFence[] inFlightFences;

    boolean keys[] = new boolean[1024];

    final float speedMove = 2.0f;
    final float speedRotate = 5.0f;

    float alpha = 0.5f, alphadir = -0.01f;

    boolean doResize = false;
    boolean doSwap = false;

    Vertex[] vertices = new Vertex[] {
      new Vertex(new float[] {-0.5f, -0.5f}, new float[] {1.0f, 0.0f, 0.0f}),
      new Vertex(new float[] { 0.5f, -0.5f}, new float[] {0.0f, 1.0f, 0.0f}),
      new Vertex(new float[] { 0.5f,  0.5f}, new float[] {0.0f, 0.0f, 1.0f}),
      new Vertex(new float[] {-0.5f,  0.5f}, new float[] {1.0f, 1.0f, 1.0f}),
    };

    int[] indices = new int[] {0, 1, 2, 2, 3, 0};

    public static float[] toArray(Vertex[] vs) {
      int cnt = vs.length;
      float[] fs = new float[cnt * 5];
      int p = 0;
      for(int i=0;i<cnt;i++) {
        fs[p++] = vs[i].pos.v[0];
        fs[p++] = vs[i].pos.v[1];
        fs[p++] = vs[i].color.v[0];
        fs[p++] = vs[i].color.v[1];
        fs[p++] = vs[i].color.v[2];
      }
      return fs;
    }

    public Cube(boolean doSwap) {
      this.doSwap = doSwap;
    }

    public void initVulkan() {
      if (debug) JFLog.log("createInstance");
      createInstance();
      if (debug) JFLog.log("createSurface");
      createSurface();
      if (debug) JFLog.log("pickPhysicalDevice");
      pickPhysicalDevice();
      if (debug) JFLog.log("createLogicalDevice");
      createLogicalDevice();
      if (debug) JFLog.log("createSwapChain");
      createSwapChain();
      if (debug) JFLog.log("createImageViews");
      createImageViews();
      if (debug) JFLog.log("createRenderPass");
      createRenderPass();
      if (debug) JFLog.log("createGraphicsPipeline");
      createGraphicsPipeline();
      if (debug) JFLog.log("createFramebuffers");
      createFramebuffers();
      if (debug) JFLog.log("createCommandPool");
      createCommandPool();
      if (debug) JFLog.log("createVertexBuffer");
      createVertexBuffer();
      if (debug) JFLog.log("createIndexBuffer");
      createIndexBuffer();
      if (debug) JFLog.log("createCommandBuffer");
      createCommandBuffer();
      if (debug) JFLog.log("createSyncObjects");
      createSyncObjects();
    }

    public void createInstance() {
      VkInstanceCreateInfo createInfo = new VkInstanceCreateInfo();
      VkInstance[] handle = new VkInstance[1];
      handle[0] = new VkInstance();
      ArrayList<String> fullExts = new ArrayList<>();
      String[] exts = window.getRequiredExtensions();
      for(int a=0;a<exts.length;a++) {
        if (debug) JFLog.log("ext=" + exts[a]);
        fullExts.add(exts[a]);
      }
      for(int a=0;a<desiredExtensions.length;a++) {
        fullExts.add(desiredExtensions[a]);
      }
      fullExtensions = fullExts.toArray(JF.StringArrayType);
      if (debug) {
        //this is critical for debugging
        createInfo.enabledLayerCount = 1;
        createInfo.ptr_ppEnabledLayerNames = new String[] {VK.VK_LAYER_KHRONOS_validation};
      }
      createInfo.enabledExtensionCount = exts.length;
      createInfo.ptr_ppEnabledExtensionNames = exts;
      createInfo.ptr_pApplicationInfo.applicationVersion = VK_MAKE_VERSION(1,0,0);
      createInfo.ptr_pApplicationInfo.engineVersion = VK_MAKE_VERSION(1,0,0);
      createInfo.ptr_pApplicationInfo.apiVersion = VK_MAKE_VERSION(1,1,0);
      vk.vkCreateInstance(createInfo, null, handle);
      if (debug) JFLog.log("handle=" + handle[0]);
      instance = handle[0];
      if (debug) JFLog.log("instance=0x" + Long.toHexString(instance.getValue()));
    }

    public void createSurface() {
      surface = new VkSurfaceKHR();
      surface.setValue(window.createSurface(instance.getValue()));
      if (debug) JFLog.log("surface=0x" + Long.toHexString(surface.getValue()));
      if (surface.getValue() == 0) {
        JFLog.log("Unable to create a surface");
        System.exit(1);
      }
    }

    void createLogicalDevice() {
      QueueFamilyIndices indices = findQueueFamilies(physicalDevice);

      ArrayList<VkDeviceQueueCreateInfo> queueCreateInfos = new ArrayList<>();
      int[] uniqueQueueFamilies = null;
      if (indices.graphicsFamily != indices.presentFamily) {
        uniqueQueueFamilies = new int[] {indices.graphicsFamily, indices.presentFamily};
      } else {
        uniqueQueueFamilies = new int[] {indices.graphicsFamily};
      }

      float[] queuePriority = new float[] {1.0f};
      for (int queueFamily : uniqueQueueFamilies) {
        VkDeviceQueueCreateInfo queueCreateInfo = new VkDeviceQueueCreateInfo();
        queueCreateInfo.queueFamilyIndex = queueFamily;
        queueCreateInfo.queueCount = 1;
        queueCreateInfo.ptr_pQueuePriorities = queuePriority;
        queueCreateInfos.add(queueCreateInfo);
      }

      VkPhysicalDeviceFeatures deviceFeatures = new VkPhysicalDeviceFeatures();

      VkDeviceCreateInfo createInfo = new VkDeviceCreateInfo();

      createInfo.queueCreateInfoCount = queueCreateInfos.size();
      createInfo.ptr_pQueueCreateInfos = queueCreateInfos.toArray(new VkDeviceQueueCreateInfo[0]);

      createInfo.enabledLayerCount = 0;

      if (debug) JFLog.log("enabledExtensionCount = " + desiredExtensions.length);
      createInfo.enabledExtensionCount = desiredExtensions.length;
      createInfo.ptr_ppEnabledExtensionNames = desiredExtensions;

      createInfo.ptr_pEnabledFeatures = deviceFeatures;

      VkDevice[] devices = new VkDevice[1];
      devices[0] = new VkDevice();

      if (debug) JFLog.log("vkCreateDevice:physicalDevice=0x" + Long.toHexString(physicalDevice.getValue()));
      int res = vk.vkCreateDevice(physicalDevice, createInfo, null, devices);
      if (res != VK_SUCCESS) {
        JFLog.log("Failed to create logical device! VkResult = " + res);
        System.exit(1);
      }

      device = devices[0];
      if (debug) JFLog.log("device=0x" + Long.toHexString(device.getValue()));

      VkQueue[] graphicsQueues = new VkQueue[1];
      graphicsQueues[0] = new VkQueue();
      VkQueue[] presentQueues = new VkQueue[1];
      presentQueues[0] = new VkQueue();

      VkDeviceQueueInfo2 graphicsInfo = new VkDeviceQueueInfo2();

      graphicsInfo.queueFamilyIndex = indices.graphicsFamily;
      graphicsInfo.queueIndex = 0;

      if (debug) JFLog.log("vkGetDeviceQueue2");
      vk.vkGetDeviceQueue(device, indices.graphicsFamily, 0, graphicsQueues);

      VkDeviceQueueInfo2 presentInfo = new VkDeviceQueueInfo2();

      presentInfo.queueFamilyIndex = indices.presentFamily;
      presentInfo.queueIndex = 0;

      if (debug) JFLog.log("vkGetDeviceQueue2");
      vk.vkGetDeviceQueue(device, indices.presentFamily, 0, presentQueues);

      graphicsQueue = graphicsQueues[0];
      presentQueue = presentQueues[0];
    }

    void createSwapChain() {
      SwapChainSupportDetails swapChainSupport = querySwapChainSupport(physicalDevice);

      VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
      VkPresentModeKHR presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
      VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);

      int[] imageCount = new int[1];
      imageCount[0] = swapChainSupport.capabilities.minImageCount + 1;
      if (swapChainSupport.capabilities.maxImageCount > 0 && imageCount[0] > swapChainSupport.capabilities.maxImageCount) {
        imageCount[0] = swapChainSupport.capabilities.maxImageCount;
      }

      VkSwapchainCreateInfoKHR createInfo = new VkSwapchainCreateInfoKHR();
      createInfo.surface = surface;

      createInfo.minImageCount = imageCount[0];
      createInfo.imageFormat = surfaceFormat.format;
      createInfo.imageColorSpace = surfaceFormat.colorSpace;
      createInfo.imageExtent = extent;
      createInfo.imageArrayLayers = 1;
      createInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

      QueueFamilyIndices indices = findQueueFamilies(physicalDevice);
      int queueFamilyIndices[] = {indices.graphicsFamily, indices.presentFamily};

      if (indices.graphicsFamily != indices.presentFamily) {
        createInfo.imageSharingMode = VK_SHARING_MODE_CONCURRENT;
        createInfo.queueFamilyIndexCount = 2;
        createInfo.ptr_pQueueFamilyIndices = queueFamilyIndices;
      } else {
        createInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
      }

      createInfo.preTransform = swapChainSupport.capabilities.currentTransform;
      createInfo.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
      createInfo.presentMode = presentMode;
      createInfo.clipped = VK_TRUE;

      createInfo.oldSwapchain.set(VK_NULL_HANDLE);

      if (debug) JFLog.log("vkCreateSwapchainKHR");
      swapChain = new VkSwapchainKHR();
      int res = vk.vkCreateSwapchainKHR(device, createInfo, null, new VkSwapchainKHR[] {swapChain});
      if (res != VK_SUCCESS) {
        JFLog.log("Failed to create swap chain! VkResult=" + res);
        System.exit(1);
      }

      if (debug) JFLog.log("vkGetSwapchainImagesKHR");
      vk.vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);
      int count = imageCount[0];
      swapChainImages = new VkImage[count];
      for(int a=0;a<count;a++) {
        swapChainImages[a] = new VkImage();
      }
      if (debug) JFLog.log("vkGetSwapchainImagesKHR");
      vk.vkGetSwapchainImagesKHR(device, swapChain, imageCount, swapChainImages);

      swapChainImageFormat = surfaceFormat.format;
      swapChainExtent = extent;
    }

    VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR[] availableFormats) {
      for (VkSurfaceFormatKHR availableFormat : availableFormats) {
        if (availableFormat.format.getValue() == VK_FORMAT_B8G8R8A8_SRGB && availableFormat.colorSpace.getValue() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
          return availableFormat;
        }
      }

      return availableFormats[0];
    }

    VkPresentModeKHR chooseSwapPresentMode(VkPresentModeKHR[] availablePresentModes) {
      for (VkPresentModeKHR availablePresentMode : availablePresentModes) {
        if (availablePresentMode.getValue() == VK_PRESENT_MODE_MAILBOX_KHR) {
          return availablePresentMode;
        }
      }

      VkPresentModeKHR presentMode = new VkPresentModeKHR();
      presentMode.setValue(VK_PRESENT_MODE_FIFO_KHR);

      return presentMode;
    }

    private int clamp(int value, int min, int max) {
      if (value < min) value = min;
      if (value > max) value = max;
      return value;
    }

    VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
      if (capabilities.currentExtent.width != UInteger.MAX_VALUE) {
        return capabilities.currentExtent;
      } else {
        int width, height;
        int w_h[] = window.getFramebufferSize();
        width = w_h[0];
        height = w_h[1];

        VkExtent2D actualExtent = new VkExtent2D();
        actualExtent.width = width;
        actualExtent.height = height;

        actualExtent.width = clamp(actualExtent.width, capabilities.minImageExtent.width, capabilities.maxImageExtent.width);
        actualExtent.height = clamp(actualExtent.height, capabilities.minImageExtent.height, capabilities.maxImageExtent.height);

        return actualExtent;
      }
    }

    public void pickPhysicalDevice() {
      int[] deviceCount = new int[1];
      vk.vkEnumeratePhysicalDevices(instance, deviceCount, null);

      if (deviceCount[0] == 0) {
        JFLog.log("Failed to find GPUs with Vulkan support!");
        System.exit(1);
      }
      if (debug) JFLog.log("device count=" + deviceCount[0]);
      VkPhysicalDevice[] devices = new VkPhysicalDevice[deviceCount[0]];
      devices[0] = new VkPhysicalDevice();
      vk.vkEnumeratePhysicalDevices(instance, deviceCount, devices);

      for (VkPhysicalDevice device : devices) {
        if (isDeviceSuitable(device)) {
          physicalDevice = device;
          break;
        }
      }

      if (physicalDevice == null) {
        JFLog.log("Failed to find a suitable GPU!");
        System.exit(1);
      }

      if (debug) {
        JFLog.log("physicalDevice=0x" + Long.toHexString(physicalDevice.getValue()));
      }
    }

    public boolean isDeviceSuitable(VkPhysicalDevice device) {
      QueueFamilyIndices indices = findQueueFamilies(device);

      boolean extensionsSupported = checkDeviceExtensionSupport(device);

      boolean swapChainAdequate = false;
      if (extensionsSupported) {
        SwapChainSupportDetails swapChainSupport = querySwapChainSupport(device);
        swapChainAdequate = swapChainSupport.formats != null && swapChainSupport.presentModes != null;
        if (debug) JFLog.log("swapChainAdequte=" + swapChainAdequate);
      }

      return indices.isComplete() && extensionsSupported && swapChainAdequate;
    }

    QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
      if (debug) JFLog.log("findQueueFamilies:device=0x" + Long.toHexString(device.getValue()));
      QueueFamilyIndices indices = new QueueFamilyIndices();

      int[] queueFamilyCount = new int[1];
      vk.vkGetPhysicalDeviceQueueFamilyProperties2(device, queueFamilyCount, null);

      int count = queueFamilyCount[0];
      if (debug) JFLog.log("queueFamilyCount=" + count);

      VkQueueFamilyProperties2[] queueFamilies = new VkQueueFamilyProperties2[count];
      for(int i=0;i<count;i++) {
        queueFamilies[i] = new VkQueueFamilyProperties2();
      }
      vk.vkGetPhysicalDeviceQueueFamilyProperties2(device, queueFamilyCount, queueFamilies);

      int i = 0;
      for (VkQueueFamilyProperties2 queueFamily : queueFamilies) {
        if ((queueFamily.queueFamilyProperties.queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
          if (debug) JFLog.log("graphicsFamily=" + i);
          indices.graphicsFamily = i;
        }

        int[] presentSupport = new int[1];
        if (debug) JFLog.log("vkGetPhysicalDeviceSurfaceSupportKHR:device=0x" + Long.toHexString(device.getValue()));
        vk.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

        if (presentSupport[0] != 0) {
          if (debug) JFLog.log("presentFamily=" + i);
          indices.presentFamily = i;
        }

        if (indices.isComplete()) {
          break;
        }

        i++;
      }

      if (debug) JFLog.log("indices.isComplete()=" + indices.isComplete());
      return indices;
    }

    boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
      int[] extensionCount = new int[1];
      vk.vkEnumerateDeviceExtensionProperties(device, null, extensionCount, null);

      int count = extensionCount[0];
      if (debug) JFLog.log("extensionCount=" + count);

      VkExtensionProperties[] availableExtensions = new VkExtensionProperties[count];
      for(int i=0;i<count;i++) {
        availableExtensions[i] = new VkExtensionProperties();
      }
      if (debug) JFLog.log("vkEnumerateDeviceExtensionProperties");
      vk.vkEnumerateDeviceExtensionProperties(device, null, extensionCount, availableExtensions);

      ArrayList<String> requiredExtensions = new ArrayList<>();
      for(String extension : desiredExtensions) {
        requiredExtensions.add(extension);
      }

      for (VkExtensionProperties availableExtension : availableExtensions) {
        if (availableExtension == null) {
          if (debug) JFLog.log("extension==null");
          continue;
        }
        String extensionName = availableExtension.getExtensionName();
        if (debug_ext) JFLog.log("extensionName=" + extensionName);
        requiredExtensions.remove(extensionName);
      }

      if (debug) JFLog.log("checkDeviceExtensionSupport=" + requiredExtensions.isEmpty());
      if (!requiredExtensions.isEmpty()) {
        for(String ext : requiredExtensions) {
          JFLog.log("req ext=" + ext);
        }
      }
      return requiredExtensions.isEmpty();
    }

    SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device) {
      SwapChainSupportDetails details = new SwapChainSupportDetails();

      vk.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

      {
        int[] formatCount = new int[1];
        vk.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, formatCount, null);

        int count = formatCount[0];
        if (debug) JFLog.log("surface formats count=" + count);
        if (count != 0) {
          details.formats = new VkSurfaceFormatKHR[count];
          for(int i=0;i<count;i++) {
            details.formats[i] = new VkSurfaceFormatKHR();
          }
          vk.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, formatCount, details.formats);
        }
      }

      {
        int[] presentModeCount = new int[1];
        vk.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, presentModeCount, null);

        int count = presentModeCount[0];
        if (debug) JFLog.log("present mode count=" + count);
        if (count != 0) {
          details.presentModes = new VkPresentModeKHR[count];
          for(int i=0;i<count;i++) {
            details.presentModes[i] = new VkPresentModeKHR();
          }
          vk.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, presentModeCount, details.presentModes);
        }
      }

      return details;
    }

    void createImageViews() {
      if (debug) JFLog.log("imageviews=" + swapChainImages.length);
      swapChainImageViews = new VkImageView[swapChainImages.length];

      for (int i = 0; i < swapChainImages.length; i++) {
        VkImageViewCreateInfo createInfo = new VkImageViewCreateInfo();
        createInfo.image = swapChainImages[i];
        createInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        createInfo.format = swapChainImageFormat;
        createInfo.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
        createInfo.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
        createInfo.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
        createInfo.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
        createInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        createInfo.subresourceRange.baseMipLevel = 0;
        createInfo.subresourceRange.levelCount = 1;
        createInfo.subresourceRange.baseArrayLayer = 0;
        createInfo.subresourceRange.layerCount = 1;

        VkImageView[] imageView = new VkImageView[1];
        imageView[0] = new VkImageView();
        if (debug) JFLog.log("vkCreateImageView");
        if (vk.vkCreateImageView(device, createInfo, null, imageView) != VK_SUCCESS) {
          JFLog.log("Failed to create image views!");
          System.exit(1);
        }
        swapChainImageViews[i] = imageView[0];
      }
    }

    void createRenderPass() {
      VkAttachmentDescription colorAttachment = new VkAttachmentDescription();
      colorAttachment.format = swapChainImageFormat;
      colorAttachment.samples = VK_SAMPLE_COUNT_1_BIT;
      colorAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
      colorAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
      colorAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
      colorAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
      colorAttachment.initialLayout.setValue(VK_IMAGE_LAYOUT_UNDEFINED);
      colorAttachment.finalLayout.setValue(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

      VkAttachmentReference colorAttachmentRef = new VkAttachmentReference();
      colorAttachmentRef.attachment = 0;
      colorAttachmentRef.layout.setValue(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

      VkSubpassDescription subpass = new VkSubpassDescription();
      subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
      subpass.colorAttachmentCount = 1;
      subpass.ptr_pColorAttachments = new VkAttachmentReference[] {colorAttachmentRef};

      VkSubpassDependency dependency = new VkSubpassDependency();
      dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
      dependency.dstSubpass = 0;
      dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
      dependency.srcAccessMask = 0;
      dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
      dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;

      VkRenderPassCreateInfo renderPassInfo = new VkRenderPassCreateInfo();
      renderPassInfo.attachmentCount = 1;
      renderPassInfo.ptr_pAttachments = new VkAttachmentDescription[] {colorAttachment};
      renderPassInfo.subpassCount = 1;
      renderPassInfo.ptr_pSubpasses = new VkSubpassDescription[] {subpass};
      renderPassInfo.dependencyCount = 1;
      renderPassInfo.ptr_pDependencies = new VkSubpassDependency[] {dependency};

      renderPass = new VkRenderPass();
      if (debug) JFLog.log("vkCreateRenderPass2");
      if (vk.vkCreateRenderPass(device, renderPassInfo, null, new VkRenderPass[] {renderPass}) != VK_SUCCESS) {
        JFLog.log("Failed to create render pass!");
        System.exit(1);
      }
      if (debug) JFLog.log("renderPass=0x" + Long.toHexString(renderPass.getValue()));
    }

    void createGraphicsPipeline() {
      VkPipelineLayoutCreateInfo pipelineLayoutInfo = new VkPipelineLayoutCreateInfo();
      pipelineLayoutInfo.setLayoutCount = 0;
      pipelineLayoutInfo.pushConstantRangeCount = 0;

      pipelineLayout = new VkPipelineLayout();
      if (debug) JFLog.log("vkCreatePipelineLayout");
      if (vk.vkCreatePipelineLayout(device, pipelineLayoutInfo, null, new VkPipelineLayout[] {pipelineLayout}) != VK_SUCCESS) {
        JFLog.log("Failed to create pipeline layout!");
        System.exit(1);
      }
      if (debug) JFLog.log("pipelineLayout=0x" + Long.toHexString(pipelineLayout.getValue()));

      byte[] vertShaderCode = JF.readResource("/javaforce/vk/glsl/testvk-vert.spv");
      if (vertShaderCode == null) {
        JFLog.log("Failed to read vertex shader code!");
        System.exit(1);
      }
      byte[] fragShaderCode = JF.readResource("/javaforce/vk/glsl/testvk-frag.spv");
      if (fragShaderCode == null) {
        JFLog.log("Failed to read fragment shader code!");
        System.exit(1);
      }

      VkShaderModule vertShaderModule = createShaderModule(vertShaderCode);
      VkShaderModule fragShaderModule = createShaderModule(fragShaderCode);

      VkPipelineShaderStageCreateInfo vertShaderStageInfo = new VkPipelineShaderStageCreateInfo();
      vertShaderStageInfo.stage = VK_SHADER_STAGE_VERTEX_BIT;
      vertShaderStageInfo.module = vertShaderModule;
      vertShaderStageInfo.ptr_pName = "main";

      VkPipelineShaderStageCreateInfo fragShaderStageInfo = new VkPipelineShaderStageCreateInfo();
      fragShaderStageInfo.stage = VK_SHADER_STAGE_FRAGMENT_BIT;
      fragShaderStageInfo.module = fragShaderModule;
      fragShaderStageInfo.ptr_pName = "main";

      VkPipelineShaderStageCreateInfo shaderStages[] = new VkPipelineShaderStageCreateInfo[] {vertShaderStageInfo, fragShaderStageInfo};

      VkPipelineVertexInputStateCreateInfo vertexInputInfo = new VkPipelineVertexInputStateCreateInfo();

      VkVertexInputBindingDescription[] bindDescs = Vertex.getBindingDescriptions();
      VkVertexInputAttributeDescription[] attrDescs = Vertex.getAttributeDescriptions();

      vertexInputInfo.vertexBindingDescriptionCount = 1;
      vertexInputInfo.vertexAttributeDescriptionCount = attrDescs.length;
      vertexInputInfo.ptr_pVertexBindingDescriptions = bindDescs;
      vertexInputInfo.ptr_pVertexAttributeDescriptions = attrDescs;

      VkPipelineInputAssemblyStateCreateInfo inputAssembly = new VkPipelineInputAssemblyStateCreateInfo();
      inputAssembly.topology = new VkPrimitiveTopology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
      inputAssembly.primitiveRestartEnable = VK_FALSE;

      VkPipelineViewportStateCreateInfo viewportState = new VkPipelineViewportStateCreateInfo();
      viewportState.viewportCount = 1;  //dynamic
      viewportState.scissorCount = 1;  //dynamic

      VkPipelineRasterizationStateCreateInfo rasterizer = new VkPipelineRasterizationStateCreateInfo();
      rasterizer.depthClampEnable = VK_FALSE;
      rasterizer.rasterizerDiscardEnable = VK_FALSE;
      rasterizer.polygonMode = new VkPolygonMode(VK_POLYGON_MODE_FILL);
      rasterizer.lineWidth = 1.0f;
      rasterizer.cullMode = VK_CULL_MODE_BACK_BIT;
      rasterizer.frontFace = new VkFrontFace(VK_FRONT_FACE_CLOCKWISE);
      rasterizer.depthBiasEnable = VK_FALSE;

      VkPipelineMultisampleStateCreateInfo multisampling = new VkPipelineMultisampleStateCreateInfo();
      multisampling.sampleShadingEnable = VK_FALSE;
      multisampling.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

      VkPipelineColorBlendAttachmentState colorBlendAttachment = new VkPipelineColorBlendAttachmentState();
      colorBlendAttachment.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
      colorBlendAttachment.blendEnable = VK_FALSE;

      VkPipelineColorBlendStateCreateInfo colorBlending = new VkPipelineColorBlendStateCreateInfo();
      colorBlending.logicOpEnable = VK_FALSE;
      colorBlending.logicOp = new VkLogicOp(VK_LOGIC_OP_COPY);
      colorBlending.attachmentCount = 1;
      colorBlending.ptr_pAttachments = colorBlendAttachment;
      colorBlending.blendConstants[0] = 0.0f;
      colorBlending.blendConstants[1] = 0.0f;
      colorBlending.blendConstants[2] = 0.0f;
      colorBlending.blendConstants[3] = 0.0f;

      VkDynamicState[] dynamicStates = new VkDynamicState[] {
        new VkDynamicState(VK_DYNAMIC_STATE_VIEWPORT),
        new VkDynamicState(VK_DYNAMIC_STATE_SCISSOR)
      };
      VkPipelineDynamicStateCreateInfo dynamicState = new VkPipelineDynamicStateCreateInfo();
      dynamicState.dynamicStateCount = dynamicStates.length;
      dynamicState.ptr_pDynamicStates = dynamicStates;

      VkGraphicsPipelineCreateInfo[] pipelineInfos = new VkGraphicsPipelineCreateInfo[1];
      VkGraphicsPipelineCreateInfo pipelineInfo = new VkGraphicsPipelineCreateInfo();
      pipelineInfos[0] = pipelineInfo;
      pipelineInfo.stageCount = 2;
      pipelineInfo.ptr_pStages = shaderStages;
      pipelineInfo.ptr_pVertexInputState = vertexInputInfo;
      pipelineInfo.ptr_pInputAssemblyState = inputAssembly;
      pipelineInfo.ptr_pViewportState = viewportState;
      pipelineInfo.ptr_pRasterizationState = rasterizer;
      pipelineInfo.ptr_pMultisampleState = multisampling;
      pipelineInfo.ptr_pColorBlendState = colorBlending;
      pipelineInfo.ptr_pDynamicState = dynamicState;
      pipelineInfo.layout = pipelineLayout;
      pipelineInfo.renderPass = renderPass;
      pipelineInfo.subpass = 0;
      pipelineInfo.basePipelineHandle.set(VK_NULL_HANDLE);

      graphicsPipeline = new VkPipeline();
      if (debug) JFLog.log("vkCreateGraphicsPipelines");
      if (vk.vkCreateGraphicsPipelines(device, null, 1, pipelineInfos, null, new VkPipeline[] {graphicsPipeline}) != VK_SUCCESS) {
        JFLog.log("Failed to create graphics pipeline!");
        System.exit(1);
      }
      if (debug) JFLog.log("pipeline=0x" + Long.toHexString(graphicsPipeline.getValue()));
      if (debug) JFLog.log("vkDestroyShaderModule");
      vk.vkDestroyShaderModule(device, fragShaderModule, null);
      if (debug) JFLog.log("vkDestroyShaderModule");
      vk.vkDestroyShaderModule(device, vertShaderModule, null);
    }

    VkShaderModule createShaderModule(byte[] code) {
      if (debug) JFLog.log("code.length=" + code.length);
      VkShaderModuleCreateInfo createInfo = new VkShaderModuleCreateInfo();
      createInfo.codeSize = code.length;  //must be multiple of 4
      createInfo.ptr_pCode = code;

      VkShaderModule[] shaderModule = new VkShaderModule[1];
      shaderModule[0] = new VkShaderModule();
      if (debug) JFLog.log("vkCreateShaderModule");
      if (vk.vkCreateShaderModule(device, createInfo, null, shaderModule) != VK_SUCCESS) {
        JFLog.log("Failed to create shader module!");
        System.exit(10);
      }

      return shaderModule[0];
    }

    void createFramebuffers() {
      swapChainFramebuffers = new VkFramebuffer[swapChainImageViews.length];

      for (int i = 0; i < swapChainImageViews.length; i++) {
        VkImageView attachments[] = {
          swapChainImageViews[i]
        };

        VkFramebufferCreateInfo framebufferInfo = new VkFramebufferCreateInfo();
        framebufferInfo.renderPass = renderPass;
        framebufferInfo.attachmentCount = 1;
        framebufferInfo.ptr_pAttachments = attachments;
        framebufferInfo.width = swapChainExtent.width;
        framebufferInfo.height = swapChainExtent.height;
        framebufferInfo.layers = 1;

        swapChainFramebuffers[i] = new VkFramebuffer();
        if (debug) JFLog.log("vkCreateFramebuffer");
        if (vk.vkCreateFramebuffer(device, framebufferInfo, null, new VkFramebuffer[] {swapChainFramebuffers[i]}) != VK_SUCCESS) {
          JFLog.log("Failed to create framebuffer!");
          System.exit(1);
        }
        if (debug) JFLog.log("framebuffer=0x" + Long.toHexString(swapChainFramebuffers[i].getValue()));
      }
    }

    void createCommandPool() {
      QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice);

      VkCommandPoolCreateInfo poolInfo = new VkCommandPoolCreateInfo();
      poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
      poolInfo.queueFamilyIndex = queueFamilyIndices.graphicsFamily;

      commandPool = new VkCommandPool();
      if (debug) JFLog.log("vkCreateCommandPool");
      if (vk.vkCreateCommandPool(device, poolInfo, null, new VkCommandPool[] {commandPool}) != VK_SUCCESS) {
        JFLog.log("Failed to create command pool!");
        System.exit(1);
      }
    }

    void createVertexBuffer() {
      VkDeviceSize bufferSize = new VkDeviceSize(4 * 7 * vertices.length);

      float[] vs = toArray(vertices);

      VkBuffer stagingBuffer = new VkBuffer();
      VkDeviceMemory stagingBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingBufferMemory);

      long[] addr = new long[1];
      vk.vkMapMemory(device, stagingBufferMemory, 0, vs.length * 4, 0, addr);
      MemorySegment memory = MemorySegment.ofAddress(addr[0]).reinterpret(vs.length * 4);
      for(int i=0;i<vs.length;i++) {
        memory.setAtIndex(ValueLayout.JAVA_FLOAT, i, vs[i]);
      }
      vk.vkUnmapMemory(device, stagingBufferMemory);

      vertexBuffer = new VkBuffer();
      vertexBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, vertexBuffer, vertexBufferMemory);

      copyBuffer(stagingBuffer, vertexBuffer, bufferSize);

      vk.vkDestroyBuffer(device, stagingBuffer, null);
      vk.vkFreeMemory(device, stagingBufferMemory, null);
    }

    void createBuffer(VkDeviceSize size, int usage, int properties, VkBuffer buffer, VkDeviceMemory bufferMemory) {
      VkBufferCreateInfo bufferInfo = new VkBufferCreateInfo();
      bufferInfo.size.set(size);
      bufferInfo.usage = usage;
      bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

      if (vk.vkCreateBuffer(device, bufferInfo, null, new VkBuffer[] {buffer}) != VK_SUCCESS) {
        JFLog.log("Failed to create vertex buffer!");
        System.exit(1);
      }
      if (debug) JFLog.log("vertexBuffer=" + buffer.toString());

      VkMemoryRequirements memRequirements = new VkMemoryRequirements();
      vk.vkGetBufferMemoryRequirements(device, buffer, memRequirements);

      VkMemoryAllocateInfo allocInfo = new VkMemoryAllocateInfo();
      allocInfo.allocationSize = memRequirements.size;
      allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

      if (vk.vkAllocateMemory(device, allocInfo, null, new VkDeviceMemory[] {bufferMemory}) != VK_SUCCESS) {
        JFLog.log("Failed to allocate vertex buffer memory!");
        System.exit(1);
      }

      vk.vkBindBufferMemory(device, buffer, bufferMemory, new VkDeviceSize(0));
    }

    void copyBuffer(VkBuffer srcBuffer, VkBuffer dstBuffer, VkDeviceSize size) {
      VkCommandBufferAllocateInfo allocInfo = new VkCommandBufferAllocateInfo();
      allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
      allocInfo.commandPool = commandPool;
      allocInfo.commandBufferCount = 1;

      VkCommandBuffer commandBuffer = new VkCommandBuffer();
      vk.vkAllocateCommandBuffers(device, allocInfo, new VkCommandBuffer[] {commandBuffer});

      VkCommandBufferBeginInfo beginInfo = new VkCommandBufferBeginInfo();
      beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

      vk.vkBeginCommandBuffer(commandBuffer, beginInfo);

      VkBufferCopy copyRegion = new VkBufferCopy();
      copyRegion.size = size;
      vk.vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, 1, new VkBufferCopy[] {copyRegion});

      vk.vkEndCommandBuffer(commandBuffer);

      VkSubmitInfo submitInfo = new VkSubmitInfo();
      submitInfo.commandBufferCount = 1;
      submitInfo.ptr_pCommandBuffers = new VkCommandBuffer[] {commandBuffer};

      vk.vkQueueSubmit(graphicsQueue, 1, new VkSubmitInfo[] {submitInfo}, null);
      vk.vkQueueWaitIdle(graphicsQueue);

      vk.vkFreeCommandBuffers(device, commandPool, 1, new VkCommandBuffer[] {commandBuffer});
    }

    void createIndexBuffer() {
      VkDeviceSize bufferSize = new VkDeviceSize(4 * indices.length);

      VkBuffer stagingBuffer = new VkBuffer();
      VkDeviceMemory stagingBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingBufferMemory);

      long[] addr = new long[1];
      vk.vkMapMemory(device, stagingBufferMemory, 0, indices.length * 4, 0, addr);
      MemorySegment memory = MemorySegment.ofAddress(addr[0]).reinterpret(indices.length * 4);
      for(int i=0;i<indices.length;i++) {
        memory.setAtIndex(ValueLayout.JAVA_INT, i, indices[i]);
      }
      vk.vkUnmapMemory(device, stagingBufferMemory);

      indexBuffer = new VkBuffer();
      indexBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, indexBuffer, indexBufferMemory);

      copyBuffer(stagingBuffer, indexBuffer, bufferSize);

      vk.vkDestroyBuffer(device, stagingBuffer, null);
      vk.vkFreeMemory(device, stagingBufferMemory, null);
    }

    int findMemoryType(int typeFilter, int properties) {
      if (debug) JFLog.log("findMemoryType:" + typeFilter + "," + properties);
      VkPhysicalDeviceMemoryProperties2 memProperties = new VkPhysicalDeviceMemoryProperties2();
      vk.vkGetPhysicalDeviceMemoryProperties2(physicalDevice, memProperties);

      for (int i = 0; i < memProperties.memoryProperties.memoryTypeCount; i++) {
        if (debug) JFLog.log("findMemoryType:memory=" + i + "," + memProperties.memoryProperties.memoryTypes[i].propertyFlags);
        if (((typeFilter & (1 << i)) != 0) && (memProperties.memoryProperties.memoryTypes[i].propertyFlags & properties) == properties) {
          return i;
        }
      }

      JFLog.log("Failed to find suitable memory type!");
      System.exit(1);
      return -1;
    }

    void createCommandBuffer() {
      VkCommandBufferAllocateInfo allocInfo = new VkCommandBufferAllocateInfo();
      allocInfo.commandPool = commandPool;
      allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
      allocInfo.commandBufferCount = max_frames;

      commandBuffers = new VkCommandBuffer[max_frames];
      for(int i=0;i<max_frames;i++) {
        commandBuffers[i] = new VkCommandBuffer();
      }
      if (vk.vkAllocateCommandBuffers(device, allocInfo, commandBuffers) != VK_SUCCESS) {
        JFLog.log("Failed to allocate command buffers!");
        System.exit(1);
      }
    }

    void createSyncObjects() {
      VkSemaphoreCreateInfo semaphoreInfo = new VkSemaphoreCreateInfo();
      VkSemaphore[] semaphore = new VkSemaphore[] {new VkSemaphore()};
      VkFence[] fence = new VkFence[] {new VkFence()};

      VkFenceCreateInfo fenceInfo = new VkFenceCreateInfo();
      fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;

      imageAvailableSemaphores = new VkSemaphore[max_frames];
      renderFinishedSemaphores = new VkSemaphore[max_frames];
      inFlightFences = new VkFence[max_frames];

      for(int i=0;i<max_frames;i++) {
        imageAvailableSemaphores[i] = new VkSemaphore();
        renderFinishedSemaphores[i] = new VkSemaphore();
        inFlightFences[i] = new VkFence();

        if (debug) JFLog.log("vkCreateSemaphore");
        if (vk.vkCreateSemaphore(device, semaphoreInfo, null, semaphore) != VK_SUCCESS)
        {
          JFLog.log("Failed to create synchronization objects for a frame!");
          System.exit(1);
        }
        imageAvailableSemaphores[i].set(semaphore[0]);
        if (debug) JFLog.log("vkCreateSemaphore=0x" + Long.toHexString(semaphore[0].getValue()));

        if (debug) JFLog.log("vkCreateSemaphore");
        if (vk.vkCreateSemaphore(device, semaphoreInfo, null, semaphore) != VK_SUCCESS)
        {
            JFLog.log("Failed to create synchronization objects for a frame!");
            System.exit(1);
        }
        renderFinishedSemaphores[i].set(semaphore[0]);
        if (debug) JFLog.log("vkCreateSemaphore=0x" + Long.toHexString(semaphore[0].getValue()));

        if (debug) JFLog.log("vkCreateFence");
        if (vk.vkCreateFence(device, fenceInfo, null, fence) != VK_SUCCESS)
        {
            JFLog.log("Failed to create synchronization objects for a frame!");
            System.exit(1);
        }
        inFlightFences[i].set(fence[0]);
        if (debug) JFLog.log("vkCreateFence=0x" + Long.toHexString(fence[0].getValue()));
      }
    }

    void recreateSwapChain() {
      int[] w_h = window.getFramebufferSize();

      vk.vkDeviceWaitIdle(device);

      cleanupSwapChain();

      createSwapChain();
      createImageViews();
      createFramebuffers();
    }

    public void init() {
      initVulkan();

      //setup timers
      glTimer = new java.util.Timer();
      int delay = 1000 / FPS;
      glTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          render();
        }
      }, delay, delay);
      fpsTimer = new java.util.Timer();
      fpsTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          int cnt;
          synchronized(fpsLock) {
            cnt = fpsCounter;
            fpsCounter = 0;
          }
          JFLog.log("fps=" + cnt);
  //        JFLog.log("camera=" + scene.m_camera.toString());
  //        JFLog.log("model=" + scene.m_model.toString());
        }
      }, 1000, 1000);

    }

    public void uninit() {
      synchronized (renderLock) {
        if (glTimer != null) {
          glTimer.cancel();
          glTimer = null;
        }
        if (fpsTimer != null) {
          fpsTimer.cancel();
          fpsTimer = null;
        }
      }
    }

    public void render() {
      if (!active) return;
      synchronized(fpsLock) {
        fpsCounter++;
      }
      synchronized (renderLock) {
        if (window.getFramebufferResized()) {
          doResize = true;
        }
        drawFrame();
        if (doSwap) TestVK.swap();
      }
    }

    public Object3 makeWall(float x,float y,float z,int side,Object3 obj) {
      //use counter clock wise triangles
      float vp[];  //vertex coords (positions)
      vp = new float[] {
        (x     ) * 10.0f, (y     ) * 10.0f,  (z     ) * 10.0f,
        (x+1.0f) * 10.0f, (y     ) * 10.0f,  (z     ) * 10.0f,
        (x     ) * 10.0f, (y+1.0f) * 10.0f,  (z     ) * 10.0f,
        (x+1.0f) * 10.0f, (y+1.0f) * 10.0f,  (z     ) * 10.0f,
        (x     ) * 10.0f, (y     ) * 10.0f,  (z+1.0f) * 10.0f,
        (x+1.0f) * 10.0f, (y     ) * 10.0f,  (z+1.0f) * 10.0f,
        (x     ) * 10.0f, (y+1.0f) * 10.0f,  (z+1.0f) * 10.0f,
        (x+1.0f) * 10.0f, (y+1.0f) * 10.0f,  (z+1.0f) * 10.0f
      };
      int off = obj.vpl.size() / 3;  //current vertex count
      int pts[] = null;
      float uv[] = new float[8 * 2];
      switch (side) {
        case 1:  //top
          pts = new int[] {2,6,7,3};
          break;
        case 2:  //bottom
          pts = new int[] {5,4,0,1};
          break;
        case 3:  //left
          pts = new int[] {2,0,4,6};
          break;
        case 4:  //right
          pts = new int[] {7,5,1,3};
          break;
        case 5:  //front
          pts = new int[] {6,4,5,7};
          break;
        case 6:  //back
          pts = new int[] {3,1,0,2};
          break;
      }
      float u = 0.0f;
      float v = 0.0f;
      for(int a=0;a<4;a++) {
        uv[pts[a] * 2 + 0] = u;
        uv[pts[a] * 2 + 1] = v;
        if (u == 0.0f && v == 0.0f) v = 1.0f;
        else if (u == 0.0f && v == 1.0f) u = 1.0f;
        else if (u == 1.0f && v == 1.0f) v = 0.0f;
  //      else if (tx == 0.0f && ty == 0.0f) tx = 0.0f;  //not needed - end of loop
      }
      obj.addVertex(vp, uv);
      obj.addPoly(new int[] {off + pts[0], off + pts[1], off + pts[2]});
      obj.addPoly(new int[] {off + pts[0], off + pts[2], off + pts[3]});
      return obj;
    }

    public void cleanup() {
      if (debug) JFLog.log("cleanup");
      vk.vkDeviceWaitIdle(device);

      vk.vkDestroyBuffer(device, vertexBuffer, null);
      vk.vkFreeMemory(device, vertexBufferMemory, null);

      vk.vkDestroyBuffer(device, indexBuffer, null);
      vk.vkFreeMemory(device, indexBufferMemory, null);

      if (debug) JFLog.log("cleanup:semaphores + fences");
      for(int i=0;i<max_frames;i++) {
        if (debug) JFLog.log("cleanup:semaphore[]" + i);
        vk.vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
        if (debug) JFLog.log("cleanup:semaphore[]" + i);
        vk.vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
        if (debug) JFLog.log("cleanup:fence[]" + i);
        vk.vkDestroyFence(device, inFlightFences[i], null);
      }

      if (debug) JFLog.log("cleanup:command pool");
      vk.vkDestroyCommandPool(device, commandPool, null);

      if (debug) JFLog.log("cleanup:framebuffers");
      for (VkFramebuffer framebuffer : swapChainFramebuffers) {
        vk.vkDestroyFramebuffer(device, framebuffer, null);
      }

      if (debug) JFLog.log("cleanup:pipelines");
      vk.vkDestroyPipeline(device, graphicsPipeline, null);
      vk.vkDestroyPipelineLayout(device, pipelineLayout, null);
      vk.vkDestroyRenderPass(device, renderPass, null);

      if (debug) JFLog.log("cleanup:images");
      for (VkImageView imageView : swapChainImageViews) {
        vk.vkDestroyImageView(device, imageView, null);
      }

      if (debug) JFLog.log("cleanup:swapchain");
      vk.vkDestroySwapchainKHR(device, swapChain, null);
      vk.vkDestroyDevice(device, null);

      if (debug) JFLog.log("cleanup:surface");
      vk.vkDestroySurfaceKHR(instance, surface, null);
      if (debug) JFLog.log("cleanup:instance");
      vk.vkDestroyInstance(instance, null);
    }

    void cleanupSwapChain() {
      for (VkFramebuffer framebuffer : swapChainFramebuffers) {
        vk.vkDestroyFramebuffer(device, framebuffer, null);
      }

      for (VkImageView imageView : swapChainImageViews) {
        vk.vkDestroyImageView(device, imageView, null);
      }

      vk.vkDestroySwapchainKHR(device, swapChain, null);
    }

    void drawFrame() {
      if (debug) JFLog.log("frame=" + current_frame);
      VkFence[] fence = new VkFence[] {new VkFence()};

      if (debug) JFLog.log("vkWaitForFences");
      fence[0] = inFlightFences[current_frame];
      vk.vkWaitForFences(device, 1, fence, VK_TRUE, ULong.MAX_VALUE);
      if (debug) JFLog.log("vkResetFences");
      fence[0] = inFlightFences[current_frame];
      vk.vkResetFences(device, 1, fence);

      int[] imageIndex = new int[1];
      if (debug) JFLog.log("vkAcquireNextImageKHR");
      int result = vk.vkAcquireNextImageKHR(device, swapChain, ULong.MAX_VALUE, imageAvailableSemaphores[current_frame], null, imageIndex);

      if (result == VK_ERROR_OUT_OF_DATE_KHR) {
        recreateSwapChain();
        return;
      } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
        JFLog.log("Failed to acquire swap chain image!");
        System.exit(1);
      }

      if (debug) JFLog.log("vkResetCommandBuffer");
      vk.vkResetCommandBuffer(commandBuffers[current_frame], /*VkCommandBufferResetFlagBits*/ 0);
      recordCommandBuffer(commandBuffers[current_frame], imageIndex[0]);

      int[] waitStages = {VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT};

      VkSemaphore[] signalSemaphores = new VkSemaphore[] {renderFinishedSemaphores[current_frame]};

      VkSubmitInfo submitInfo = new VkSubmitInfo();
      submitInfo.waitSemaphoreCount = 1;
      submitInfo.ptr_pWaitSemaphores = new VkSemaphore[] {imageAvailableSemaphores[current_frame]};
      submitInfo.ptr_pWaitDstStageMask = waitStages;
      submitInfo.commandBufferCount = 1;
      submitInfo.ptr_pCommandBuffers = new VkCommandBuffer[] {commandBuffers[current_frame]};
      submitInfo.signalSemaphoreCount = 1;
      submitInfo.ptr_pSignalSemaphores = signalSemaphores;

      if (debug) JFLog.log("vkQueueSubmit");
      if (vk.vkQueueSubmit(graphicsQueue, 1, new VkSubmitInfo[] {submitInfo}, inFlightFences[current_frame]) != VK_SUCCESS) {
        JFLog.log("Failed to submit draw command buffer!");
        System.exit(1);
      }

      VkPresentInfoKHR presentInfo = new VkPresentInfoKHR();
      presentInfo.waitSemaphoreCount = 1;
      presentInfo.ptr_pWaitSemaphores = signalSemaphores;
      presentInfo.swapchainCount = 1;
      presentInfo.ptr_pSwapchains = new VkSwapchainKHR[] {swapChain};
      presentInfo.ptr_pImageIndices = imageIndex;

      if (debug) JFLog.log("vkQueuePresentKHR");
      result = vk.vkQueuePresentKHR(presentQueue, presentInfo);

      if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || doResize) {
        doResize = false;
        recreateSwapChain();
      } else if (result != VK_SUCCESS) {
        JFLog.log("Failed to present swap chain image!");
        System.exit(1);
      }

      current_frame = (current_frame + 1) % max_frames;
    }

    void recordCommandBuffer(VkCommandBuffer commandBuffer, int imageIndex) {
      if (debug) JFLog.log("recordCommandBuffer:imageIndex=" + imageIndex);
      VkCommandBufferBeginInfo beginInfo = new VkCommandBufferBeginInfo();

      if (debug) JFLog.log("vkBeginCommandBuffer");
      if (vk.vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
        JFLog.log("Failed to begin recording command buffer!");
        System.exit(1);
      }

      VkColor clearColor = new VkColor();
      clearColor.color[3] = 1.0f;

      VkRenderPassBeginInfo renderPassInfo = new VkRenderPassBeginInfo();
      renderPassInfo.renderPass = renderPass;
      renderPassInfo.framebuffer = swapChainFramebuffers[imageIndex];
      renderPassInfo.renderArea.extent = swapChainExtent;
      renderPassInfo.clearValueCount = 1;
      renderPassInfo.ptr_pClearValues = new VkColor[] {clearColor};

      if (debug) JFLog.log("vkCmdBeginRenderPass");
      vk.vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

      if (debug) JFLog.log("vkCmdBindPipeline");
      vk.vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

      VkViewport viewport = new VkViewport();
      viewport.x = 0.0f;
      viewport.y = 0.0f;
      viewport.width = (float)(swapChainExtent.width);
      viewport.height = (float)(swapChainExtent.height);
      viewport.minDepth = 0.0f;
      viewport.maxDepth = 1.0f;

      if (debug) JFLog.log("vkCmdSetViewport");
      vk.vkCmdSetViewport(commandBuffer, 0, 1, new VkViewport[] {viewport});

      VkRect2D scissor = new VkRect2D();
      scissor.extent = swapChainExtent;

      if (debug) JFLog.log("vkCmdSetScissor");
      vk.vkCmdSetScissor(commandBuffer, 0, 1, new VkRect2D[] {scissor});

      if (debug) JFLog.log("vkCmdBindVertexBuffers");
      VkBuffer vertexBuffers[] = {vertexBuffer};
      VkDeviceSize offsets[] = new VkDeviceSize[1];
      offsets[0] = new VkDeviceSize();
      vk.vkCmdBindVertexBuffers(commandBuffer, 0, 1, vertexBuffers, offsets);

      if (debug) JFLog.log("vkCmdBindIndexBuffer");
      vk.vkCmdBindIndexBuffer(commandBuffer, indexBuffer, new VkDeviceSize(0), VK_INDEX_TYPE_UINT32);

      if (debug) JFLog.log("vkCmdDrawIndexed");
      vk.vkCmdDrawIndexed(commandBuffer, indices.length, 1, 0, 0, 0);

      if (debug) JFLog.log("vkCmdEndRenderPass");
      vk.vkCmdEndRenderPass(commandBuffer);

      if (debug) JFLog.log("vkEndCommandBuffer");
      if (vk.vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
        JFLog.log("Failed to record command buffer!");
        System.exit(1);
      }
    }
  }
}
