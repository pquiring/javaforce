#define _GLFW_COCOA
#define _GLFW_NSGL
#define _GLFW_USE_OPENGL

//this is no longer maintained or tested

//common
#include "../glfw/src/platform.c"
#include "../glfw/src/context.c"
#include "../glfw/src/init.c"
#include "../glfw/src/input.c"
#include "../glfw/src/monitor.c"
#include "../glfw/src/window.c"
#include "../glfw/src/egl_context.c"
#include "../glfw/src/vulkan.c"
#include "../glfw/src/osmesa_context.c"

//mac
#include "../glfw/src/nsgl_context.m"
#include "../glfw/src/cocoa_init.m"
#include "../glfw/src/cocoa_monitor.m"
#include "../glfw/src/cocoa_time.c"
#include "../glfw/src/cocoa_window.m"
#include "../glfw/src/cocoa_joystick.m"

//posix
#undef _POSIX_C_SOURCE
#include "../glfw/src/posix_module.c"
#include "../glfw/src/posix_thread.c"
