package javaforce.tests;

import java.lang.foreign.*;
import java.io.*;
import java.util.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.ui.*;
import javaforce.ffm.*;
import javaforce.gl.*;
import javaforce.vk.*;
import static javaforce.vk.VK.*;

/** Vulkan Test.
 *
 * See https://vulkan-tutorial.com/
 *
 * @author pquiring
 */

public class TestVK implements WindowEvents {
  public static boolean debug = true;
  public static boolean debug_mem = false;
  public static boolean debug_ext = false;

  public static Window window;
  public static Cube cube;
  public static VK vk;
  public static boolean active = true;

  public static void main(String args[]) {
    try {
      if (args.length == 1 && args[0].equals("sizes")) {
        getSizes();
        System.exit(1);
      }
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

  public static void getSizes() {
    //to generate structs.txt : cd src\javaforce\vk : grep FFMStruct -l > structs.txt : remove .java from structs.txt
    String _name = null;
    try {
      String[] names = null;
      FileInputStream fis = JF.fileopen("structs.txt");
      names = new String(JF.readAll(fis)).split("\n");
      JF.fileclose(fis);
      for(String name : names) {
        if (name.length() == 0) continue;
        _name = name;
        Class<?> cls = Class.forName("javaforce.vk." + name);
        FFMStruct struct = (FFMStruct)cls.getConstructors()[0].newInstance();
        int size = struct.getSize();
        System.out.println("sizeof(" + name + ")=" + size);
      }
    } catch (Exception e) {
      JFLog.log("struct=" + _name);
      JFLog.log(e);
    }
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
    public Vector3 pos;
    public Vector3 color;
    public Vector2 texCoord;

    public static int getSize() {
      return 4 * 8;
    }

    public Vertex(float[] pos, float[] color) {
      this.pos = new Vector3(pos);
      this.color = new Vector3(color);
    }

    public Vertex(float[] pos, float[] color, float[] uv) {
      this.pos = new Vector3(pos);
      this.color = new Vector3(color);
      this.texCoord = new Vector2(uv);
    }

    public static VkVertexInputBindingDescription[] getBindingDescriptions() {
      VkVertexInputBindingDescription bindingDescription = new VkVertexInputBindingDescription();
      bindingDescription.binding = 0;
      bindingDescription.stride = getSize();
      bindingDescription.inputRate.setValue(VK_VERTEX_INPUT_RATE_VERTEX);

      return new VkVertexInputBindingDescription[] {bindingDescription};
    }

    public static VkVertexInputAttributeDescription[] getAttributeDescriptions() {
      VkVertexInputAttributeDescription[] attributeDescriptions = new VkVertexInputAttributeDescription[3];

      for(int i=0;i<3;i++) {
        attributeDescriptions[i] = new VkVertexInputAttributeDescription();
      }

      attributeDescriptions[0].binding = 0;
      attributeDescriptions[0].location = 0;
      attributeDescriptions[0].format.setValue(VK_FORMAT_R32G32B32_SFLOAT);
      attributeDescriptions[0].offset =  0;  //offset of pos

      attributeDescriptions[1].binding = 0;
      attributeDescriptions[1].location = 1;
      attributeDescriptions[1].format.setValue(VK_FORMAT_R32G32B32_SFLOAT);
      attributeDescriptions[1].offset = 4 * 3;  //offset of color

      attributeDescriptions[2].binding = 0;
      attributeDescriptions[2].location = 2;
      attributeDescriptions[2].format.setValue(VK_FORMAT_R32G32_SFLOAT);
      attributeDescriptions[2].offset = 4 * 6;  //offset of texCoord

      return attributeDescriptions;
    }

  }

  public static class Cube {
    Object renderLock = new Object();
    java.util.Timer glTimer, fpsTimer;
    Object fpsLock = new Object();
    int fpsCounter;

    final int FPS = 60;
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
    VkDescriptorSetLayout descriptorSetLayout;
    VkDescriptorPool descriptorPool;
    VkDescriptorSet[] descriptorSets;

    VkPipelineLayout pipelineLayout;
    VkPipeline graphicsPipeline;

    VkCommandPool commandPool;
    VkCommandBuffer[] commandBuffers;

    VkBuffer[] uniformBuffers;
    VkDeviceMemory[] uniformBuffersMemory;
    MemorySegment[] uniformBuffersMapped;

    VkBuffer vertexBuffer;
    VkDeviceMemory vertexBufferMemory;
    VkBuffer indexBuffer;
    VkDeviceMemory indexBufferMemory;

    VkSemaphore[] imageAvailableSemaphores;
    VkSemaphore[] renderFinishedSemaphores;
    VkFence[] inFlightFences;

    VkImage textureImage;
    VkDeviceMemory textureImageMemory;
    VkImageView textureImageView;
    VkSampler textureSampler;

    VkImage depthImage;
    VkDeviceMemory depthImageMemory;
    VkImageView depthImageView;

    VkImage colorImage;
    VkDeviceMemory colorImageMemory;
    VkImageView colorImageView;

    int mipLevels;
    int msaaSamples = VK_SAMPLE_COUNT_1_BIT;

    boolean doResize = false;
    boolean doSwap = false;

    Vertex[] vertices = new Vertex[] {
      new Vertex(new float[] {-0.5f, -0.5f,  0.0f}, new float[] {1.0f, 0.0f, 0.0f}, new float[] {0.0f,1.0f}),
      new Vertex(new float[] { 0.5f, -0.5f,  0.0f}, new float[] {0.0f, 1.0f, 0.0f}, new float[] {0.0f,0.0f}),
      new Vertex(new float[] { 0.5f,  0.5f,  0.0f}, new float[] {0.0f, 0.0f, 1.0f}, new float[] {1.0f,0.0f}),
      new Vertex(new float[] {-0.5f,  0.5f,  0.0f}, new float[] {1.0f, 1.0f, 1.0f}, new float[] {1.0f,1.0f}),

      new Vertex(new float[] {-0.5f, -0.5f, -0.5f}, new float[] {1.0f, 0.0f, 0.0f}, new float[] {0.0f,1.0f}),
      new Vertex(new float[] { 0.5f, -0.5f, -0.5f}, new float[] {0.0f, 1.0f, 0.0f}, new float[] {0.0f,0.0f}),
      new Vertex(new float[] { 0.5f,  0.5f, -0.5f}, new float[] {0.0f, 0.0f, 1.0f}, new float[] {1.0f,0.0f}),
      new Vertex(new float[] {-0.5f,  0.5f, -0.5f}, new float[] {1.0f, 1.0f, 1.0f}, new float[] {1.0f,1.0f}),
    };

    //sets of triangles (3)
    int[] indices = new int[] {
      0, 1, 2, 2, 3, 0,
      4, 5, 6, 6, 7, 4,
    };

    public static class UniformBufferObject {
      public Matrix model = new Matrix();
      public Matrix view = new Matrix();
      public Matrix proj = new Matrix();

      public void copy_row_major(float[] s, int sp, float[] d, int dp, int cnt) {
        for(int row=0;row<4;row++) {
          for(int col=0;col<4;col++) {
            d[dp++] = s[col * 4 + row];
          }
        }
      }

      public float[] toArray() {
        float[] out = new float[16 * 3];
        System.arraycopy(model.m, 0, out, 0, 16);
        System.arraycopy(view.m, 0, out, 16, 16);
        System.arraycopy(proj.m, 0, out, 32, 16);
        return out;
      }

      public static int sizeof() {
        return 4 * 16 * 3;  //sizeof(float) * matrix.length * 3
      }
    };

    public static float[] toArray(Vertex[] vs) {
      int cnt = vs.length;
      float[] fs = new float[cnt * 8];
      int p = 0;
      for(int i=0;i<cnt;i++) {
        fs[p++] = vs[i].pos.v[0];
        fs[p++] = vs[i].pos.v[1];
        fs[p++] = vs[i].pos.v[2];
        fs[p++] = vs[i].color.v[0];
        fs[p++] = vs[i].color.v[1];
        fs[p++] = vs[i].color.v[2];
        fs[p++] = vs[i].texCoord.v[0];
        fs[p++] = vs[i].texCoord.v[1];
      }
      return fs;
    }

    public Cube(boolean doSwap) {
      this.doSwap = doSwap;
    }

    public void initVulkan() {
      if (debug) JFLog.log("createInstance");
      createInstance();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createSurface");
      createSurface();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("pickPhysicalDevice");
      pickPhysicalDevice();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createLogicalDevice");
      createLogicalDevice();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createSwapChain");
      createSwapChain();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createImageViews");
      createImageViews();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createRenderPass");
      createRenderPass();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createDescriptorSetLayout");
      createDescriptorSetLayout();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createGraphicsPipeline");
      createGraphicsPipeline();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createCommandPool");
      createCommandPool();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createColorResources");
      createColorResources();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createDepthResources");
      createDepthResources();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createFramebuffers");
      createFramebuffers();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createTextureImage");
      createTextureImage();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createTextureImageView");
      createTextureImageView();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createTextureSampler");
      createTextureSampler();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createVertexBuffer");
      createVertexBuffer();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createIndexBuffer");
      createIndexBuffer();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createUniformBuffers");
      createUniformBuffers();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createDescriptorPool");
      createDescriptorPool();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createDescriptorSets");
      createDescriptorSets();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createCommandBuffers");
      createCommandBuffers();
      if (debug_mem) System.gc();
      if (debug) JFLog.log("createSyncObjects");
      createSyncObjects();
      if (debug_mem) System.gc();
    }

    public void createInstance() {
      VkInstanceCreateInfo createInfo = new VkInstanceCreateInfo();
      VkInstance[] handle = new VkInstance[1];
      handle[0] = new VkInstance();
      ArrayList<String> fullExts = new ArrayList<>();
      String[] exts = window.getRequiredExtensions();
      for(int a=0;a<exts.length;a++) {
        if (debug_ext) JFLog.log("ext=" + exts[a]);
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
      deviceFeatures.samplerAnisotropy = VK_TRUE;

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
          msaaSamples = getMaxUsableSampleCount();
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

      VkPhysicalDeviceFeatures2 supportedFeatures = new VkPhysicalDeviceFeatures2();
      vk.vkGetPhysicalDeviceFeatures2(device, supportedFeatures);

      return indices.isComplete() && extensionsSupported && swapChainAdequate && supportedFeatures.features.samplerAnisotropy == VK_TRUE;
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
        swapChainImageViews[i] = createImageView(swapChainImages[i], swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1);
      }
    }

    void createRenderPass() {
      VkAttachmentDescription colorAttachment = new VkAttachmentDescription();
      colorAttachment.format = swapChainImageFormat;
      colorAttachment.samples = msaaSamples;
      colorAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
      colorAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
      colorAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
      colorAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
      colorAttachment.initialLayout = new VkImageLayout(VK_IMAGE_LAYOUT_UNDEFINED);
      colorAttachment.finalLayout = new VkImageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

      VkAttachmentDescription depthAttachment = new VkAttachmentDescription();
      depthAttachment.format = findDepthFormat();
      depthAttachment.samples = msaaSamples;
      depthAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
      depthAttachment.storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
      depthAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
      depthAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
      depthAttachment.initialLayout = new VkImageLayout(VK_IMAGE_LAYOUT_UNDEFINED);
      depthAttachment.finalLayout = new VkImageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

      VkAttachmentDescription colorAttachmentResolve = new VkAttachmentDescription();
      colorAttachmentResolve.format = swapChainImageFormat;
      colorAttachmentResolve.samples = VK_SAMPLE_COUNT_1_BIT;
      colorAttachmentResolve.loadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
      colorAttachmentResolve.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
      colorAttachmentResolve.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
      colorAttachmentResolve.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
      colorAttachmentResolve.initialLayout = new VkImageLayout(VK_IMAGE_LAYOUT_UNDEFINED);
      colorAttachmentResolve.finalLayout = new VkImageLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

      VkAttachmentReference colorAttachmentRef = new VkAttachmentReference();
      colorAttachmentRef.attachment = 0;
      colorAttachmentRef.layout.setValue(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

      VkAttachmentReference depthAttachmentRef = new VkAttachmentReference();
      depthAttachmentRef.attachment = 1;
      depthAttachmentRef.layout = new VkImageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

      VkAttachmentReference colorAttachmentResolveRef = new VkAttachmentReference();
      colorAttachmentResolveRef.attachment = 2;
      colorAttachmentResolveRef.layout = new VkImageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

      VkSubpassDescription subpass = new VkSubpassDescription();
      subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
      subpass.colorAttachmentCount = 1;
      subpass.ptr_pColorAttachments = new VkAttachmentReference[] {colorAttachmentRef};
      subpass.ptr_pDepthStencilAttachment = new VkAttachmentReference[] {depthAttachmentRef};
      subpass.ptr_pResolveAttachments = new VkAttachmentReference[] {colorAttachmentResolveRef};

      VkSubpassDependency dependency = new VkSubpassDependency();
      dependency.srcSubpass = VK_SUBPASS_EXTERNAL;
      dependency.dstSubpass = 0;
      dependency.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
      dependency.srcAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
      dependency.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
      dependency.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;

      VkRenderPassCreateInfo renderPassInfo = new VkRenderPassCreateInfo();
      renderPassInfo.attachmentCount = 3;
      renderPassInfo.ptr_pAttachments = new VkAttachmentDescription[] {colorAttachment, depthAttachment, colorAttachmentResolve};
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

    void createDescriptorSetLayout() {
      VkDescriptorSetLayoutBinding uboLayoutBinding = new VkDescriptorSetLayoutBinding();
      uboLayoutBinding.binding = 0;
      uboLayoutBinding.descriptorCount = 1;
      uboLayoutBinding.descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
      uboLayoutBinding.stageFlags = VK_SHADER_STAGE_VERTEX_BIT;

      VkDescriptorSetLayoutBinding samplerLayoutBinding = new VkDescriptorSetLayoutBinding();
      samplerLayoutBinding.binding = 1;
      samplerLayoutBinding.descriptorCount = 1;
      samplerLayoutBinding.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
      samplerLayoutBinding.ptr_pImmutableSamplers = null;
      samplerLayoutBinding.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;

      VkDescriptorSetLayoutCreateInfo layoutInfo = new VkDescriptorSetLayoutCreateInfo();
      layoutInfo.bindingCount = 2;
      layoutInfo.ptr_pBindings = new VkDescriptorSetLayoutBinding[] {uboLayoutBinding, samplerLayoutBinding};

      descriptorSetLayout = new VkDescriptorSetLayout();

      if (vk.vkCreateDescriptorSetLayout(device, layoutInfo, null, new VkDescriptorSetLayout[] {descriptorSetLayout}) != VK_SUCCESS) {
        JFLog.log("Failed to create descriptor set layout!");
        System.exit(1);
      }
      if (debug) JFLog.log("descriptorSetLayout=" + descriptorSetLayout.toString());
    }

    void createGraphicsPipeline() {
      VkPipelineLayoutCreateInfo pipelineLayoutInfo = new VkPipelineLayoutCreateInfo();
      pipelineLayoutInfo.setLayoutCount = 0;
      pipelineLayoutInfo.pushConstantRangeCount = 0;
      pipelineLayoutInfo.setLayoutCount = 1;
      pipelineLayoutInfo.ptr_pSetLayouts = new VkDescriptorSetLayout[] {descriptorSetLayout};

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
      vertexInputInfo.ptr_pVertexBindingDescriptions = bindDescs;
      vertexInputInfo.vertexAttributeDescriptionCount = attrDescs.length;
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
      multisampling.rasterizationSamples = msaaSamples;

      VkPipelineDepthStencilStateCreateInfo depthStencil = new VkPipelineDepthStencilStateCreateInfo();
      depthStencil.sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
      depthStencil.depthTestEnable = VK_TRUE;
      depthStencil.depthWriteEnable = VK_TRUE;
      depthStencil.depthCompareOp = new VkCompareOp(VK_COMPARE_OP_LESS);
      depthStencil.depthBoundsTestEnable = VK_FALSE;
      depthStencil.stencilTestEnable = VK_FALSE;

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
      pipelineInfo.ptr_pDepthStencilState = depthStencil;
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
          colorImageView,
          depthImageView,
          swapChainImageViews[i],
        };

        VkFramebufferCreateInfo framebufferInfo = new VkFramebufferCreateInfo();
        framebufferInfo.renderPass = renderPass;
        framebufferInfo.attachmentCount = attachments.length;
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
      if (debug) JFLog.log("vkCreateCommandPool=" + commandPool.toString());
    }

    void createVertexBuffer() {
      float[] vs = toArray(vertices);

      VkDeviceSize bufferSize = new VkDeviceSize(4 * vs.length);

      VkBuffer stagingBuffer = new VkBuffer();
      VkDeviceMemory stagingBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingBufferMemory);

      long[] addr = new long[1];
      if (debug) JFLog.log("vkMapMemory:" + stagingBufferMemory.toString());
      vk.vkMapMemory(device, stagingBufferMemory, 0, 4 * vs.length, 0, addr);
      if (debug) JFLog.log("addr=0x" + Long.toHexString(addr[0]));
      MemorySegment memory = MemorySegment.ofAddress(addr[0]).reinterpret(4 * vs.length);
      for(int i=0;i<vs.length;i++) {
        memory.setAtIndex(JAVA_FLOAT, i, vs[i]);
      }
      vk.vkUnmapMemory(device, stagingBufferMemory);

      vertexBuffer = new VkBuffer();
      vertexBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, vertexBuffer, vertexBufferMemory);

      copyBuffer(stagingBuffer, vertexBuffer, bufferSize);

      if (debug) JFLog.log("free stagingBuffers");
      vk.vkDestroyBuffer(device, stagingBuffer, null);
      vk.vkFreeMemory(device, stagingBufferMemory, null);
    }

    void createBuffer(VkDeviceSize size, int usage, int properties, VkBuffer buffer, VkDeviceMemory bufferMemory) {
      if (debug) JFLog.log("createBuffer:size=" + size.value);
      VkBufferCreateInfo bufferInfo = new VkBufferCreateInfo();
      bufferInfo.size.setValue(size.value);
      bufferInfo.usage = usage;
      bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

      if (debug) JFLog.log("vkCreateBuffer:size=" + size.value);
      if (vk.vkCreateBuffer(device, bufferInfo, null, new VkBuffer[] {buffer}) != VK_SUCCESS) {
        JFLog.log("Failed to create vertex buffer!");
        System.exit(1);
      }
      if (debug) JFLog.log("buffer=" + buffer.toString());

      VkMemoryRequirements memRequirements = new VkMemoryRequirements();
      if (debug) JFLog.log("vkGetBufferMemoryRequirements");
      vk.vkGetBufferMemoryRequirements(device, buffer, memRequirements);

      VkMemoryAllocateInfo allocInfo = new VkMemoryAllocateInfo();
      allocInfo.allocationSize = memRequirements.size;
      allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

      if (debug) JFLog.log("vkAllocateMemory:size=" + memRequirements.size);
      if (vk.vkAllocateMemory(device, allocInfo, null, new VkDeviceMemory[] {bufferMemory}) != VK_SUCCESS) {
        JFLog.log("Failed to allocate vertex buffer memory!");
        System.exit(1);
      }
      if (debug) JFLog.log("vkAllocateMemory:memory=" + bufferMemory.toString());

      if (debug) JFLog.log("vkBindBufferMemory");
      vk.vkBindBufferMemory(device, buffer, bufferMemory, new VkDeviceSize(0) /* offset */);
    }

    VkCommandBuffer beginSingleTimeCommands() {
      if (debug) JFLog.log("copyBuffer");
      VkCommandBufferAllocateInfo allocInfo = new VkCommandBufferAllocateInfo();
      allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
      allocInfo.commandPool = commandPool;
      allocInfo.commandBufferCount = 1;

      VkCommandBuffer commandBuffer = new VkCommandBuffer();
      vk.vkAllocateCommandBuffers(device, allocInfo, new VkCommandBuffer[] {commandBuffer});

      VkCommandBufferBeginInfo beginInfo = new VkCommandBufferBeginInfo();
      beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

      vk.vkBeginCommandBuffer(commandBuffer, beginInfo);

      return commandBuffer;
    }

    void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
      vk.vkEndCommandBuffer(commandBuffer);

      VkSubmitInfo submitInfo = new VkSubmitInfo();
      submitInfo.commandBufferCount = 1;
      submitInfo.ptr_pCommandBuffers = new VkCommandBuffer[] {commandBuffer};

      vk.vkQueueSubmit(graphicsQueue, 1, new VkSubmitInfo[] {submitInfo}, null);
      vk.vkQueueWaitIdle(graphicsQueue);

      vk.vkFreeCommandBuffers(device, commandPool, 1, new VkCommandBuffer[] {commandBuffer});
    }

    void copyBuffer(VkBuffer srcBuffer, VkBuffer dstBuffer, VkDeviceSize size) {
        VkCommandBuffer commandBuffer = beginSingleTimeCommands();

        VkBufferCopy copyRegion = new VkBufferCopy();
        copyRegion.size = size;
        vk.vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, 1, new VkBufferCopy[] {copyRegion});

        endSingleTimeCommands(commandBuffer);
    }

    void createIndexBuffer() {
      VkDeviceSize bufferSize = new VkDeviceSize(4 * indices.length);

      VkBuffer stagingBuffer = new VkBuffer();
      VkDeviceMemory stagingBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingBufferMemory);

      long[] addr = new long[1];
      if (debug) JFLog.log("vkMapMemory:" + stagingBufferMemory.toString());
      vk.vkMapMemory(device, stagingBufferMemory, 0, 4 * indices.length, 0, addr);
      if (debug) JFLog.log("addr=0x" + Long.toHexString(addr[0]));
      MemorySegment memory = MemorySegment.ofAddress(addr[0]).reinterpret(4 * indices.length);
      for(int i=0;i<indices.length;i++) {
        memory.setAtIndex(JAVA_INT, i, indices[i]);
      }
      vk.vkUnmapMemory(device, stagingBufferMemory);

      indexBuffer = new VkBuffer();
      indexBufferMemory = new VkDeviceMemory();
      createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, indexBuffer, indexBufferMemory);

      copyBuffer(stagingBuffer, indexBuffer, bufferSize);

      vk.vkDestroyBuffer(device, stagingBuffer, null);
      vk.vkFreeMemory(device, stagingBufferMemory, null);
    }

    void createUniformBuffers() {
      VkDeviceSize bufferSize = new VkDeviceSize(UniformBufferObject.sizeof());
      if (debug) JFLog.log("createUniformBuffers:size=" + bufferSize.value);

      uniformBuffers = new VkBuffer[max_frames];
      uniformBuffersMemory = new VkDeviceMemory[max_frames];
      uniformBuffersMapped = new MemorySegment[max_frames];

      for (int i = 0; i < max_frames; i++) {
        uniformBuffers[i] = new VkBuffer();
        uniformBuffersMemory[i] = new VkDeviceMemory();
        createBuffer(bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, uniformBuffers[i], uniformBuffersMemory[i]);
        if (debug) JFLog.log("uniformBuffers[]=" + uniformBuffers[i].toString());
        if (debug) JFLog.log("uniformBuffersMemory[]=" + uniformBuffersMemory[i].toString());

        long[] addr = new long[1];
        if (debug) JFLog.log("vkMapMemory:" + uniformBuffersMemory[i].toString());
        vk.vkMapMemory(device, uniformBuffersMemory[i], 0, bufferSize.value, 0, addr);
        if (debug) JFLog.log("addr=0x" + Long.toHexString(addr[0]));
        uniformBuffersMapped[i] = MemorySegment.ofAddress(addr[0]).reinterpret(bufferSize.value);
        if (debug) JFLog.log("uniformBuffersMapped[]=" + uniformBuffersMapped[i].toString());
      }
    }

    int findMemoryType(int typeFilter, int properties) {
      if (debug) JFLog.log("findMemoryType:" + typeFilter + "," + properties);
      VkPhysicalDeviceMemoryProperties2 memProperties = new VkPhysicalDeviceMemoryProperties2();
      if (debug) JFLog.log("vkGetPhysicalDeviceMemoryProperties2");
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

    void createCommandBuffers() {
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
      if (debug) JFLog.log("createSyncObjects:1");
      VkSemaphoreCreateInfo semaphoreInfo = new VkSemaphoreCreateInfo();

      if (debug) JFLog.log("createSyncObjects:2");
      VkFenceCreateInfo fenceInfo = new VkFenceCreateInfo();
      fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;

      if (debug) JFLog.log("createSyncObjects:3");
      imageAvailableSemaphores = new VkSemaphore[max_frames];
      renderFinishedSemaphores = new VkSemaphore[max_frames];
      inFlightFences = new VkFence[max_frames];

      if (debug) JFLog.log("createSyncObjects:4");
      for(int i=0;i<max_frames;i++) {
        imageAvailableSemaphores[i] = new VkSemaphore();
        renderFinishedSemaphores[i] = new VkSemaphore();
        inFlightFences[i] = new VkFence();

        if (debug) JFLog.log("vkCreateSemaphore");
        if (vk.vkCreateSemaphore(device, semaphoreInfo, null, new VkSemaphore[] {imageAvailableSemaphores[i]}) != VK_SUCCESS)
        {
          JFLog.log("Failed to create synchronization objects for a frame!");
          System.exit(1);
        }
        if (debug) JFLog.log("vkCreateSemaphore=0x" + Long.toHexString(imageAvailableSemaphores[i].getValue()));

        if (debug) JFLog.log("vkCreateSemaphore");
        if (vk.vkCreateSemaphore(device, semaphoreInfo, null, new VkSemaphore[] {renderFinishedSemaphores[i]}) != VK_SUCCESS)
        {
            JFLog.log("Failed to create synchronization objects for a frame!");
            System.exit(1);
        }
        if (debug) JFLog.log("vkCreateSemaphore=0x" + Long.toHexString(renderFinishedSemaphores[i].getValue()));

        if (debug) JFLog.log("vkCreateFence");
        if (vk.vkCreateFence(device, fenceInfo, null, new VkFence[] {inFlightFences[i]}) != VK_SUCCESS)
        {
            JFLog.log("Failed to create synchronization objects for a frame!");
            System.exit(1);
        }
        if (debug) JFLog.log("vkCreateFence=0x" + Long.toHexString(inFlightFences[i].getValue()));
      }
    }

    void createDescriptorPool() {
      VkDescriptorPoolSize[] poolSizes = new VkDescriptorPoolSize[2];

      poolSizes[0] = new VkDescriptorPoolSize();
      poolSizes[0].type = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
      poolSizes[0].descriptorCount = max_frames;

      poolSizes[1] = new VkDescriptorPoolSize();
      poolSizes[1].type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
      poolSizes[1].descriptorCount = max_frames;

      VkDescriptorPoolCreateInfo poolInfo = new VkDescriptorPoolCreateInfo();
      poolInfo.poolSizeCount = 2;
      poolInfo.ptr_pPoolSizes = poolSizes;
      poolInfo.maxSets = max_frames;

      descriptorPool = new VkDescriptorPool();

      if (debug) JFLog.log("vkCreateDescriptorPool");
      if (vk.vkCreateDescriptorPool(device, poolInfo, null, new VkDescriptorPool[] {descriptorPool}) != VK_SUCCESS) {
        JFLog.log("Failed to create descriptor pool!");
        System.exit(1);
      }
      if (debug) JFLog.log("vkCreateDescriptorPool=" + descriptorPool.toString());
    }

    void createDescriptorSets() {
      VkDescriptorSetLayout[] layouts = new VkDescriptorSetLayout[max_frames];
      for(int i = 0; i < max_frames; i++) {
        layouts[i] = new VkDescriptorSetLayout();
        layouts[i].set(descriptorSetLayout);
      }

      VkDescriptorSetAllocateInfo allocInfo = new VkDescriptorSetAllocateInfo();
      allocInfo.descriptorPool = descriptorPool;
      allocInfo.descriptorSetCount = max_frames;
      allocInfo.ptr_pSetLayouts = layouts;

      descriptorSets = new VkDescriptorSet[max_frames];
      for(int i = 0; i < max_frames; i++) {
        descriptorSets[i] = new VkDescriptorSet();
      }

      if (debug) JFLog.log("vkAllocateDescriptorSets");
      if (vk.vkAllocateDescriptorSets(device, allocInfo, descriptorSets) != VK_SUCCESS) {
        JFLog.log("Failed to allocate descriptor sets!");
        System.exit(1);
      }

      if (debug) {
        for(int a=0;a<max_frames;a++) {
          JFLog.log("descriptorSets[]=" + descriptorSets[a].toString());
        }
      }

      for (int i = 0; i < max_frames; i++) {
        if (debug) JFLog.log("bufferInfo");
        VkDescriptorBufferInfo bufferInfo = new VkDescriptorBufferInfo();
        bufferInfo.buffer = uniformBuffers[i];
        bufferInfo.offset = new VkDeviceSize(0);
        bufferInfo.range = new VkDeviceSize(UniformBufferObject.sizeof());

        VkDescriptorImageInfo imageInfo = new VkDescriptorImageInfo();
        imageInfo.imageLayout = new VkImageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        imageInfo.imageView = textureImageView;
        imageInfo.sampler = textureSampler;

        VkWriteDescriptorSet[] descriptorWrites = new VkWriteDescriptorSet[2];
        descriptorWrites[0] = new VkWriteDescriptorSet();
        descriptorWrites[0].dstSet = descriptorSets[i];
        descriptorWrites[0].dstBinding = 0;
        descriptorWrites[0].dstArrayElement = 0;
        descriptorWrites[0].descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        descriptorWrites[0].descriptorCount = 1;
        descriptorWrites[0].ptr_pBufferInfo = bufferInfo;

        descriptorWrites[1] = new VkWriteDescriptorSet();
        descriptorWrites[1].dstSet = descriptorSets[i];
        descriptorWrites[1].dstBinding = 1;
        descriptorWrites[1].dstArrayElement = 0;
        descriptorWrites[1].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        descriptorWrites[1].descriptorCount = 1;
        descriptorWrites[1].ptr_pImageInfo = imageInfo;

        if (debug) JFLog.log("vkUpdateDescriptorSets");
        vk.vkUpdateDescriptorSets(device, 2, descriptorWrites, 0, null);
      }
    }

    public static double log2(double n) {
      return Math.log(n) / Math.log(2);
    }

    void createTextureImage() {
      int texWidth, texHeight, texChannels;
      JFImage image = new JFImage();
      if (!image.loadPNG("javaforce.png")) {
        JFLog.log("Failed to load javaforce.png");
        System.exit(1);
      }
      int[] pixels = image.getBuffer();
      texWidth = image.getWidth();
      texHeight = image.getHeight();

      VkDeviceSize imageSize = new VkDeviceSize(texWidth * texHeight * 4);
      mipLevels = (int)(Math.floor(log2(Math.max(texWidth, texHeight))) + 1);

      VkBuffer stagingBuffer = new VkBuffer();
      VkDeviceMemory stagingBufferMemory = new VkDeviceMemory();
      createBuffer(imageSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingBufferMemory);

      long[] addr = new long[1];
      vk.vkMapMemory(device, stagingBufferMemory, 0, imageSize.value, 0, addr);
      MemorySegment memory = MemorySegment.ofAddress(addr[0]).reinterpret(imageSize.value);
      for(int i=0;i<pixels.length;i++) {
        memory.setAtIndex(JAVA_INT, i, pixels[i]);
      }
      vk.vkUnmapMemory(device, stagingBufferMemory);

      textureImage = new VkImage();
      textureImageMemory = new VkDeviceMemory();

      createImage(texWidth, texHeight, mipLevels, VK_SAMPLE_COUNT_1_BIT, new VkFormat(VK_FORMAT_R8G8B8A8_SRGB), VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, textureImage, textureImageMemory);

      transitionImageLayout(textureImage, new VkFormat(VK_FORMAT_R8G8B8A8_SRGB), new VkImageLayout(VK_IMAGE_LAYOUT_UNDEFINED), new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL), mipLevels);
      copyBufferToImage(stagingBuffer, textureImage, texWidth, texHeight);
      //transitioned to VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL while generating mipmaps

      vk.vkDestroyBuffer(device, stagingBuffer, null);
      vk.vkFreeMemory(device, stagingBufferMemory, null);

      generateMipmaps(textureImage, new VkFormat(VK_FORMAT_R8G8B8A8_SRGB), texWidth, texHeight, mipLevels);
    }

    /*VkSampleCountFlagBits*/
    int getMaxUsableSampleCount() {
        VkPhysicalDeviceProperties2 physicalDeviceProperties = new VkPhysicalDeviceProperties2();
        vk.vkGetPhysicalDeviceProperties2(physicalDevice, physicalDeviceProperties);

        int counts = physicalDeviceProperties.properties.limits.framebufferColorSampleCounts & physicalDeviceProperties.properties.limits.framebufferDepthSampleCounts;
        if ((counts & VK_SAMPLE_COUNT_64_BIT) != 0) { return VK_SAMPLE_COUNT_64_BIT; }
        if ((counts & VK_SAMPLE_COUNT_32_BIT) != 0) { return VK_SAMPLE_COUNT_32_BIT; }
        if ((counts & VK_SAMPLE_COUNT_16_BIT) != 0) { return VK_SAMPLE_COUNT_16_BIT; }
        if ((counts & VK_SAMPLE_COUNT_8_BIT) != 0) { return VK_SAMPLE_COUNT_8_BIT; }
        if ((counts & VK_SAMPLE_COUNT_4_BIT) != 0) { return VK_SAMPLE_COUNT_4_BIT; }
        if ((counts & VK_SAMPLE_COUNT_2_BIT) != 0) { return VK_SAMPLE_COUNT_2_BIT; }

        return VK_SAMPLE_COUNT_1_BIT;
    }

    void createTextureImageView() {
      textureImageView = createImageView(textureImage, new VkFormat(VK_FORMAT_R8G8B8A8_SRGB), VK_IMAGE_ASPECT_COLOR_BIT, mipLevels);
    }

    void createTextureSampler() {
      VkPhysicalDeviceProperties2 properties = new VkPhysicalDeviceProperties2();
      vk.vkGetPhysicalDeviceProperties2(physicalDevice, properties);

      VkSamplerCreateInfo samplerInfo = new VkSamplerCreateInfo();
      samplerInfo.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
      samplerInfo.magFilter = VK_FILTER_LINEAR;
      samplerInfo.minFilter = VK_FILTER_LINEAR;
      samplerInfo.addressModeU = VK_SAMPLER_ADDRESS_MODE_REPEAT;
      samplerInfo.addressModeV = VK_SAMPLER_ADDRESS_MODE_REPEAT;
      samplerInfo.addressModeW = VK_SAMPLER_ADDRESS_MODE_REPEAT;
      samplerInfo.anisotropyEnable = VK_TRUE;
      samplerInfo.maxAnisotropy = properties.properties.limits.maxSamplerAnisotropy;
      samplerInfo.borderColor = VK_BORDER_COLOR_INT_OPAQUE_BLACK;
      samplerInfo.unnormalizedCoordinates = VK_FALSE;
      samplerInfo.compareEnable = VK_FALSE;
      samplerInfo.compareOp = new VkCompareOp(VK_COMPARE_OP_ALWAYS);
      samplerInfo.mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR;
      samplerInfo.minLod = 0.0f;
      samplerInfo.maxLod = VK_LOD_CLAMP_NONE;
      samplerInfo.mipLodBias = 0.0f;

      textureSampler = new VkSampler();
      if (vk.vkCreateSampler(device, samplerInfo, null, new VkSampler[] {textureSampler}) != VK_SUCCESS) {
        JFLog.log("Failed to create texture sampler!");
        System.exit(1);
      }
    }

    VkImageView createImageView(VkImage image, VkFormat format, int aspectFlags, int mipLevels) {
      VkImageViewCreateInfo viewInfo = new VkImageViewCreateInfo();
      viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
      viewInfo.image = image;
      viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
      viewInfo.format = format;
      viewInfo.subresourceRange.aspectMask = aspectFlags;
      viewInfo.subresourceRange.baseMipLevel = 0;
      viewInfo.subresourceRange.levelCount = mipLevels;
      viewInfo.subresourceRange.baseArrayLayer = 0;
      viewInfo.subresourceRange.layerCount = 1;

      VkImageView imageView = new VkImageView();
      if (vk.vkCreateImageView(device, viewInfo, null, new VkImageView[] {imageView}) != VK_SUCCESS) {
        JFLog.log("Failed to create image view!");
        System.exit(1);
      }

      return imageView;
    }

    void createImage(int width, int height, int mipLevels, int numSamples, VkFormat format, int tiling, int usage, int properties, VkImage image, VkDeviceMemory imageMemory) {
      if (debug) JFLog.log("createImage");
      VkImageCreateInfo imageInfo = new VkImageCreateInfo();
      imageInfo.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
      imageInfo.imageType = VK_IMAGE_TYPE_2D;
      imageInfo.extent.width = width;
      imageInfo.extent.height = height;
      imageInfo.extent.depth = 1;
      imageInfo.mipLevels = mipLevels;
      imageInfo.arrayLayers = 1;
      imageInfo.format = format;
      imageInfo.tiling = tiling;
      imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
      imageInfo.usage = usage;
      imageInfo.samples = numSamples;
      imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

      if (debug) JFLog.log("vkCreateImage");
      if (vk.vkCreateImage(device, imageInfo, null, new VkImage[] {image}) != VK_SUCCESS) {
        JFLog.log("Failed to create image!");
        System.exit(1);
      }
      if (debug) JFLog.log("image=" + image.toString());

      VkMemoryRequirements memRequirements = new VkMemoryRequirements();
      vk.vkGetImageMemoryRequirements(device, image, memRequirements);

      VkMemoryAllocateInfo allocInfo = new VkMemoryAllocateInfo();
      allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
      allocInfo.allocationSize = memRequirements.size;
      allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);

      if (vk.vkAllocateMemory(device, allocInfo, null, new VkDeviceMemory[] {imageMemory}) != VK_SUCCESS) {
        JFLog.log("failed to allocate image memory!");
        System.exit(1);
      }

      vk.vkBindImageMemory(device, image, imageMemory, 0);
    }

    void transitionImageLayout(VkImage image, VkFormat format, VkImageLayout oldLayout, VkImageLayout newLayout, int mipLevels) {
      VkCommandBuffer commandBuffer = beginSingleTimeCommands();

      VkImageMemoryBarrier barrier = new VkImageMemoryBarrier();
      barrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
      barrier.oldLayout = oldLayout;
      barrier.newLayout = newLayout;
      barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
      barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
      barrier.image = image;
      barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
      barrier.subresourceRange.baseMipLevel = 0;
      barrier.subresourceRange.levelCount = mipLevels;
      barrier.subresourceRange.baseArrayLayer = 0;
      barrier.subresourceRange.layerCount = 1;

      int sourceStage = -1;
      int destinationStage = -1;

      if (oldLayout.value == VK_IMAGE_LAYOUT_UNDEFINED && newLayout.value == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
        barrier.srcAccessMask = 0;
        barrier.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;

        sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
        destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
      } else if (oldLayout.value == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout.value == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
        barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;

        sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
      } else {
        JFLog.log("unsupported layout transition!");
        System.exit(1);
      }

      vk.vkCmdPipelineBarrier(
        commandBuffer,
        sourceStage, destinationStage,
        0,
        0, null,
        0, null,
        1, barrier
      );

      endSingleTimeCommands(commandBuffer);
    }

    void copyBufferToImage(VkBuffer buffer, VkImage image, int width, int height) {
        VkCommandBuffer commandBuffer = beginSingleTimeCommands();

        VkBufferImageCopy region = new VkBufferImageCopy();
        region.bufferOffset = new VkDeviceSize(0);
        region.bufferRowLength = 0;
        region.bufferImageHeight = 0;
        region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        region.imageSubresource.mipLevel = 0;
        region.imageSubresource.baseArrayLayer = 0;
        region.imageSubresource.layerCount = 1;
        region.imageOffset = new VkOffset3D(0,0,0);
        region.imageExtent = new VkExtent3D(width, height, 1);

        if (debug) JFLog.log("vkCmdCopyBufferToImage");
        vk.vkCmdCopyBufferToImage(commandBuffer, buffer, image, new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL), 1, new VkBufferImageCopy[] {region});

        endSingleTimeCommands(commandBuffer);
    }

    void createDepthResources() {
      VkFormat depthFormat = findDepthFormat();

      depthImage = new VkImage();
      depthImageMemory = new VkDeviceMemory();

      createImage(swapChainExtent.width, swapChainExtent.height, 1, msaaSamples, depthFormat, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, depthImage, depthImageMemory);
      depthImageView = createImageView(depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1);
    }

    VkFormat findSupportedFormat(VkFormat[] candidates, VkImageTiling tiling, int features) {
      for (VkFormat format : candidates) {
        VkFormatProperties props = new VkFormatProperties();
        vk.vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);

        if (tiling.value == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures & features) == features) {
          return format;
        } else if (tiling.value == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures & features) == features) {
          return format;
        }
      }

      JFLog.log("Failed to find supported format!");
      System.exit(1);
      return null;
    }

    VkFormat findDepthFormat() {
      return findSupportedFormat(
        new VkFormat[] {new VkFormat(VK_FORMAT_D32_SFLOAT), new VkFormat(VK_FORMAT_D32_SFLOAT_S8_UINT), new VkFormat(VK_FORMAT_D24_UNORM_S8_UINT)},
        new VkImageTiling(VK_IMAGE_TILING_OPTIMAL),
        VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT
      );
    }

    boolean hasStencilComponent(VkFormat format) {
      return format.value == VK_FORMAT_D32_SFLOAT_S8_UINT || format.value == VK_FORMAT_D24_UNORM_S8_UINT;
    }

    void generateMipmaps(VkImage image, VkFormat imageFormat, int texWidth, int texHeight, int mipLevels) {
      // Check if image format supports linear blitting
      VkFormatProperties formatProperties = new VkFormatProperties();
      vk.vkGetPhysicalDeviceFormatProperties(physicalDevice, imageFormat, formatProperties);

      if ((formatProperties.optimalTilingFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0) {
        JFLog.log("Texture image format does not support linear blitting!");
        System.exit(1);
      }

      VkCommandBuffer commandBuffer = beginSingleTimeCommands();

      VkImageMemoryBarrier barrier = new VkImageMemoryBarrier();
      barrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
      barrier.image = image;
      barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
      barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
      barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
      barrier.subresourceRange.baseArrayLayer = 0;
      barrier.subresourceRange.layerCount = 1;
      barrier.subresourceRange.levelCount = 1;

      int mipWidth = texWidth;
      int mipHeight = texHeight;

      for (int i = 1; i < mipLevels; i++) {
          barrier.subresourceRange.baseMipLevel = i - 1;
          barrier.oldLayout = new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
          barrier.newLayout = new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
          barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
          barrier.dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT;

          vk.vkCmdPipelineBarrier(commandBuffer,
              VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
              0, null,
              0, null,
              1, barrier);

          VkImageBlit blit = new VkImageBlit();
          blit.srcOffsets[0] = new VkOffset3D(0, 0, 0);
          blit.srcOffsets[1] = new VkOffset3D(mipWidth, mipHeight, 1);
          blit.srcSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
          blit.srcSubresource.mipLevel = i - 1;
          blit.srcSubresource.baseArrayLayer = 0;
          blit.srcSubresource.layerCount = 1;
          blit.dstOffsets[0] = new VkOffset3D(0, 0, 0);
          blit.dstOffsets[1] = new VkOffset3D(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1 );
          blit.dstSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
          blit.dstSubresource.mipLevel = i;
          blit.dstSubresource.baseArrayLayer = 0;
          blit.dstSubresource.layerCount = 1;

          vk.vkCmdBlitImage(commandBuffer,
              image, new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL),
              image, new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL),
              1, blit,
              new VkFilter(VK_FILTER_LINEAR));

          barrier.oldLayout = new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
          barrier.newLayout = new VkImageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
          barrier.srcAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
          barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;

          vk.vkCmdPipelineBarrier(commandBuffer,
              VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
              0, null,
              0, null,
              1, barrier);

          if (mipWidth > 1) mipWidth /= 2;
          if (mipHeight > 1) mipHeight /= 2;
      }

      barrier.subresourceRange.baseMipLevel = mipLevels - 1;
      barrier.oldLayout = new VkImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
      barrier.newLayout = new VkImageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
      barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
      barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;

      vk.vkCmdPipelineBarrier(commandBuffer,
          VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
          0, null,
          0, null,
          1, barrier);

      endSingleTimeCommands(commandBuffer);
    }

    void createColorResources() {
      VkFormat colorFormat = swapChainImageFormat;

      colorImage = new VkImage();
      colorImageMemory = new VkDeviceMemory();
      createImage(swapChainExtent.width, swapChainExtent.height, 1, msaaSamples, colorFormat, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, colorImage, colorImageMemory);
      colorImageView = createImageView(colorImage, colorFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1);
    }

    void recreateSwapChain() {
      int[] w_h = window.getFramebufferSize();

      vk.vkDeviceWaitIdle(device);

      cleanupSwapChain();

      createSwapChain();
      createImageViews();
      createColorResources();
      createDepthResources();
      createFramebuffers();
    }

    public void init() {
      initVulkan();

      vk.vkQueueWaitIdle(graphicsQueue);
      vk.vkDeviceWaitIdle(device);

      //setup timers
      glTimer = new java.util.Timer();
      int frame_rate = 1000 / FPS;
      glTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          render();
        }
      }, 100, frame_rate);
      fpsTimer = new java.util.Timer();
      fpsTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          int cnt;
          synchronized(fpsLock) {
            cnt = fpsCounter;
            fpsCounter = 0;
          }
          JFLog.log("fps=" + cnt);
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
      vk.vkQueueWaitIdle(graphicsQueue);
      vk.vkDeviceWaitIdle(device);

      cleanupSwapChain();

      vk.vkDestroyBuffer(device, vertexBuffer, null);
      vk.vkFreeMemory(device, vertexBufferMemory, null);

      vk.vkDestroyBuffer(device, indexBuffer, null);
      vk.vkFreeMemory(device, indexBufferMemory, null);

      vk.vkDestroyDescriptorPool(device, descriptorPool, null);

      vk.vkDestroySampler(device, textureSampler, null);
      vk.vkDestroyImageView(device, textureImageView, null);

      vk.vkDestroyImage(device, textureImage, null);
      vk.vkFreeMemory(device, textureImageMemory, null);

      for (int i = 0; i < max_frames; i++) {
        vk.vkDestroyBuffer(device, uniformBuffers[i], null);
        vk.vkFreeMemory(device, uniformBuffersMemory[i], null);
      }
      vk.vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);

      if (debug) JFLog.log("cleanup:semaphores + fences");
      for(int i=0;i<max_frames;i++) {
        if (debug) JFLog.log("cleanup:semaphore[" + i + "]=" + renderFinishedSemaphores[i]);
        vk.vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
        if (debug) JFLog.log("cleanup:semaphore[" + i + "]=" + imageAvailableSemaphores[i]);
        vk.vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
        if (debug) JFLog.log("cleanup:fence[" + i + "]=" + inFlightFences[i]);
        vk.vkDestroyFence(device, inFlightFences[i], null);
      }

      if (debug) JFLog.log("cleanup:command pool");
      vk.vkDestroyCommandPool(device, commandPool, null);

      if (debug) JFLog.log("cleanup:pipelines");
      vk.vkDestroyPipeline(device, graphicsPipeline, null);
      vk.vkDestroyPipelineLayout(device, pipelineLayout, null);
      vk.vkDestroyRenderPass(device, renderPass, null);

      vk.vkDestroyDevice(device, null);

      if (debug) JFLog.log("cleanup:surface");
      vk.vkDestroySurfaceKHR(instance, surface, null);
      if (debug) JFLog.log("cleanup:instance");
      vk.vkDestroyInstance(instance, null);
    }

    void cleanupSwapChain() {
      vk.vkDestroyImageView(device, depthImageView, null);
      vk.vkDestroyImage(device, depthImage, null);
      vk.vkFreeMemory(device, depthImageMemory, null);

      vk.vkDestroyImageView(device, colorImageView, null);
      vk.vkDestroyImage(device, colorImage, null);
      vk.vkFreeMemory(device, colorImageMemory, null);

      if (debug) JFLog.log("cleanup:framebuffers");
      for (VkFramebuffer framebuffer : swapChainFramebuffers) {
        vk.vkDestroyFramebuffer(device, framebuffer, null);
      }

      if (debug) JFLog.log("cleanup:images");
      for (VkImageView imageView : swapChainImageViews) {
        vk.vkDestroyImageView(device, imageView, null);
      }

      if (debug) JFLog.log("cleanup:swapchain");
      vk.vkDestroySwapchainKHR(device, swapChain, null);
    }

    float angle = 1.0f;
    UniformBufferObject ubo = new UniformBufferObject();

    void updateUniformBuffer() {
      //angle += 1.0f;
      if (angle > 90.0f) angle = 1.0f;
      if (debug) JFLog.log("angle=" + angle);
      float ratio = ((float)swapChainExtent.width) / ((float)swapChainExtent.height);
      if (debug) JFLog.log("ratio=" + ratio);

      ubo.model.addRotate(angle, 0,0,1);
      //void lookAt(Vector3 eye, Vector3 at, Vector3 up)
      ubo.view.lookAt(new Vector3(0,0,-3), new Vector3(0,0,0), new Vector3(0,1,0));
      //void perspective(float fovyInDegrees, float aspectRatio, float znear, float zfar)
      ubo.proj.perspective(45f, ratio, 0.1f, 10.0f);
      ubo.proj.m[4*1 + 1] *= -1f;
      float[] mats = ubo.toArray();
      for(int i=0;i<mats.length;i++) {
        uniformBuffersMapped[current_frame].setAtIndex(JAVA_FLOAT, i, mats[i]);
      }
    }

    void drawFrame() {
      if (debug) JFLog.log("============ drawFrame ============");
      if (debug_mem) System.gc();
      if (debug) JFLog.log("frame=" + current_frame);

      if (debug) JFLog.log("vkWaitForFences");
      vk.vkWaitForFences(device, 1, new VkFence[] {inFlightFences[current_frame]}, VK_TRUE, ULong.MAX_VALUE);

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

      updateUniformBuffer();

      if (debug) JFLog.log("vkResetFences");
      vk.vkResetFences(device, 1, new VkFence[] {inFlightFences[current_frame]});

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

      VkClearValue[] clearValues = new VkClearValue[2];
      clearValues[0] = new VkClearValue();
      clearValues[0].color.floats = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
      clearValues[1] = new VkClearValue();
      clearValues[1].depthStencil.depth = 1.0f;
      clearValues[1].depthStencil.stencil = 0;

      VkRenderPassBeginInfo renderPassInfo = new VkRenderPassBeginInfo();
      renderPassInfo.renderPass = renderPass;
      renderPassInfo.framebuffer = swapChainFramebuffers[imageIndex];
      renderPassInfo.renderArea.extent = swapChainExtent;
      renderPassInfo.clearValueCount = clearValues.length;
      renderPassInfo.ptr_pClearValues = clearValues;

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

      if (debug) JFLog.log("vkCmdBindDescriptorSets");
      vk.vkCmdBindDescriptorSets(commandBuffer, new VkPipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS), pipelineLayout, 0, 1, new VkDescriptorSet[]{descriptorSets[current_frame]}, 0, null);

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
