#define _GLFW_WIN32
#define _GLFW_WGL
#define _GLFW_USE_OPENGL

//common
#include "../glfw/src/context.c"
#include "../glfw/src/init.c"
#include "../glfw/src/input.c"
#include "../glfw/src/monitor.c"
#include "../glfw/src/window.c"

//windows
#include "../glfw/src/win32_init.c"
#include "../glfw/src/win32_monitor.c"
#include "../glfw/src/win32_time.c"
#include "../glfw/src/win32_tls.c"
#include "../glfw/src/win32_window.c"
#include "../glfw/src/winmm_joystick.c"
#include "../glfw/src/wgl_context.c"
