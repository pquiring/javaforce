/**
 *
 * @author pquiring
 */

import org.lwjgl.Sys;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import javaforce.JF;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import javaforce.gl.GL;

public class Main {
  // We need to strongly reference callback instances.
  private GLFWErrorCallback errorCallback;
  private GLFWKeyCallback keyCallback;
  private GLFWCursorPosCallback mousePosCallback;
  private GLFWMouseButtonCallback mouseButtonCallback;
  private GLFWScrollCallback scrollCallback;
  private GLFWWindowSizeCallback windowSizeCallback;
  private GLFWWindowCloseCallback windowCloseCallback;

  private float mx, my;
  private int mb;

  private boolean fullscreen;
  private boolean recreate;
  private boolean createOnce = true;

  private static final int GL_TRUE = 1;
  private static final int GL_FALSE = 0;

  private GLCode code;

  // The window handle
  private long window;

  public void run() {
    main = this;
    try {
      init();
      create();
      loop();
      // Release window and window callbacks
      close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // Terminate GLFW and release the GLFWerrorfun
      glfwTerminate();
      errorCallback.release();
    }
  }

  private void init() {
    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (glfwInit() != GL11.GL_TRUE) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }
  }

  private void create() {
    // Configure our window
    glfwDefaultWindowHints(); // optional, the current window hints are already the default
    glfwWindowHint(GLFW_VISIBLE, GL_TRUE); // the window will stay hidden after creation
    glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable

    int width = 512;
    int height = 512;

    // Create the window
    window = glfwCreateWindow(width, height, "Test", fullscreen ?  glfwGetPrimaryMonitor() : NULL, NULL);
    if (window == NULL) {
      throw new RuntimeException("Failed to create the GLFW window");
    }

    if (!fullscreen) {
      // Get the resolution of the primary monitor
      ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
      // Center our window
      glfwSetWindowPos(
        window,
        (GLFWvidmode.width(vidmode) - width) / 2,
        (GLFWvidmode.height(vidmode) - height) / 2
      );
    }

    // Make the OpenGL context current
    glfwMakeContextCurrent(window);
    // Enable v-sync
//    glfwSwapInterval(1);

    // Make the window visible
    glfwShowWindow(window);

    if (createOnce) {
      // Load GL functions via JavaForce
      GL.glInit();

      // Init the game rendering engine
      code = new GLCode(true);
      code.init();

      createOnce = false;
    }

    // Setup a key callback. It will be called every time a key is pressed, repeated or released.
    glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
      public void invoke(long window, int key, int scancode, int action, int mods) {
//        Static.log("     key:" + key + "," + scancode + "," + action + "," + mods);
        //convert to VK
        boolean press = action > 0;
        if (press && key > 13 && key < 128) {
//          code.keyTyped((char)key);
        }
        if (press) {
//          code.keyPressed(key);
        } else {
//          code.keyReleased(key);
        }
      }
    });

    glfwSetCallback(window, mouseButtonCallback = new GLFWMouseButtonCallback() {
      public void invoke(long window, int button, int action, int mods) {
//        Static.log("mouseBut:" + button + "," + action + "," + mods);
        switch (button) {
          case 0: button = 1; break;
          case 1: button = 3; break;
          default: return;
        }
        if (action == 1) {
          mb = button;
//          code.mouseDown(mx, my, button);
        } else {
          mb = 0;
//          code.mouseUp(mx, my, button);
        }
      }
    });

    glfwSetCallback(window, mousePosCallback = new GLFWCursorPosCallback() {
      public void invoke(long window, double x, double y) {
//        Static.log("mousePos:" + x + "," + y);
        mx = (float) x;
        my = (float) y;
        //code.mouseMove(mx, my, mb);
      }
    });

    glfwSetCallback(window, windowSizeCallback = new GLFWWindowSizeCallback() {
      public void invoke(long window, int x, int y) {
        code.resize(x, y);
      }
    });

    glfwSetCallback(window, windowCloseCallback = new GLFWWindowCloseCallback() {
      public void invoke(long window) {
        //code.windowClosed();
        System.exit(0);
      }
    });

    glfwSetCallback(window, scrollCallback = new GLFWScrollCallback() {
      public void invoke(long window, double x, double y) {
        if (JF.isMac()) {
          //for Mac users
          //code.mouseScrolled((int)y);
        } else {
          //for everyone else
          //code.mouseScrolled((int)-y);
        }
      }
    });
  }

  private void close() {
    glfwDestroyWindow(window);
    keyCallback.release();
    mousePosCallback.release();
    mouseButtonCallback.release();
    scrollCallback.release();
    windowSizeCallback.release();
    windowCloseCallback.release();
  }

  private void loop() {
    // This line is critical for LWJGL's interoperation with GLFW's
    // OpenGL context, or any context that is managed externally.
    // LWJGL detects the context that is current in the current thread,
    // creates the ContextCapabilities instance and makes the OpenGL
    // bindings available for use.
    GLContext.createFromCurrent();

    // Run the rendering loop until the user has attempted to close
    // the window or has pressed the ESCAPE key.
    while (glfwWindowShouldClose(window) == GL_FALSE) {
      code.render();
      // Poll for window events.
      glfwPollEvents();
      if (recreate) {
        recreate = false;
        close();
        create();
//        //code.reload();  //TODO
      }
    }
  }

  public static void main(String[] args) {
    new Main().run();
  }

  public static Main main;

  private void _swap() {
    glfwSwapBuffers(window); // swap the color buffers
  }

  public static void swap() {
    main._swap();
  }

  private void _setCursor(boolean state) {
    if (state) {
      glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    } else {
      glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }
  }

  public static void setCursor(boolean state) {
    main._setCursor(state);
  }

  private void _fullscreen() {
/*
    fullscreen = !fullscreen;
    recreate = true;
*/
    //TODO : need to reload all textures???
  }

  public static void toggleFullscreen() {
    main._fullscreen();
  }
}
