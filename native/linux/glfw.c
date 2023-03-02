#define _GLFW_X11
#define _GLFW_GLX
#define _GLFW_USE_OPENGL

#define _GLFW_HAS_XINPUT

//common
#include "../glfw/src/context.c"
#include "../glfw/src/init.c"
#include "../glfw/src/input.c"
#include "../glfw/src/monitor.c"
#include "../glfw/src/window.c"
#include "../glfw/src/egl_context.c"
#include "../glfw/src/vulkan.c"
#include "../glfw/src/osmesa_context.c"

//linux
#include "../glfw/src/x11_init.c"
#include "../glfw/src/x11_monitor.c"
#undef _POSIX_C_SOURCE
#include "../glfw/src/posix_time.c"
#include "../glfw/src/posix_thread.c"
#include "../glfw/src/x11_window.c"
#include "../glfw/src/xkb_unicode.c"
#include "../glfw/src/glx_context.c"

#ifndef __FreeBSD__
#include "../glfw/src/linux_joystick.c"
#else
int _glfwPlatformPollJoystick(_GLFWjoystick* ptr, int mode) {return 0;}
void _glfwPlatformUpdateGamepadGUID(char* guid) {}
#endif
