#include "../glfw/include/GLFW/glfw3.h"

//see javaforce.ui.Window
int KEY_TYPED = 1;
int KEY_PRESS = 2;
int KEY_RELEASE = 3;
int MOUSE_MOVE = 4;
int MOUSE_DOWN = 5;
int MOUSE_UP = 6;
int MOUSE_SCROLL = 7;
int WIN_RESIZE = 8;
int WIN_CLOSING = 9;

int STYLE_VISIBLE = 1;
int STYLE_RESIZABLE = 2;
int STYLE_TITLEBAR = 4;
int STYLE_FULLSCREEN = 8;

struct GLFWContextFFM {
  GLFWwindow *window;
  void (*dispatchEvent)(int,int,int);
};

static void key_callback_ffm(GLFWwindow* window, int key, int scancode, int action, int mods)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  int type;
  if (action > 0)
    type = KEY_PRESS;
  else
    type = KEY_RELEASE;
  ctx->dispatchEvent(type, key, 0);
}

static void character_callback_ffm(GLFWwindow* window, unsigned int codepoint)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  ctx->dispatchEvent(KEY_TYPED, codepoint, 0);
}

static void cursor_position_callback_ffm(GLFWwindow* window, double xpos, double ypos)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  ctx->dispatchEvent(MOUSE_MOVE, (int)xpos, (int)ypos);
}

static void mouse_button_callback_ffm(GLFWwindow* window, int button, int action, int mods)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  int type;
  if (action > 0)
    type = MOUSE_DOWN;
  else
    type = MOUSE_UP;
  ctx->dispatchEvent(type, ++button, 0);
}

static void scroll_callback_ffm(GLFWwindow* window, double xoffset, double yoffset)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  ctx->dispatchEvent(MOUSE_SCROLL, (int)-xoffset, (int)-yoffset);
}

static void window_close_callback_ffm(GLFWwindow* window)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  ctx->dispatchEvent(WIN_CLOSING, 0, 0);
}

static void window_size_callback_ffm(GLFWwindow* window, int width, int height)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)glfwGetWindowUserPointer(window);
  ctx->dispatchEvent(WIN_RESIZE, width, height);
}

jboolean uiInit()
{
  return glfwInit() ? JNI_TRUE : JNI_FALSE;
}

GLFWContextFFM* uiWindowCreate(jint style, const char* title, jint x, jint y, GLFWContextFFM* shared)
{
  GLFWContextFFM *ctx = (GLFWContextFFM*)malloc(sizeof(GLFWContextFFM));
  memset(ctx, 0, sizeof(GLFWContextFFM));

  GLFWmonitor *monitor = NULL;
  if (style & STYLE_FULLSCREEN) monitor = glfwGetPrimaryMonitor();

  glfwDefaultWindowHints();
  glfwWindowHint(GLFW_VISIBLE, (style & STYLE_VISIBLE ? GL_TRUE : GL_FALSE));
  glfwWindowHint(GLFW_RESIZABLE, (style & STYLE_RESIZABLE ? GL_TRUE : GL_FALSE));
  glfwWindowHint(GLFW_DECORATED, (style & STYLE_TITLEBAR ? GL_TRUE : GL_FALSE));

  GLFWwindow *sharedWin = NULL;
  if (shared != NULL) {
    sharedWin = shared->window;
  }

  int px, py;
  const GLFWvidmode *vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

  if (!(style & STYLE_FULLSCREEN)) {
    px = (vidmode->width - x) / 2;
    py = (vidmode->height - y) / 2;
  }

  ctx->window = glfwCreateWindow(x, y, title, monitor, sharedWin);

  if (!(style & STYLE_FULLSCREEN)) {
    glfwSetWindowPos(ctx->window, px, py);
  }

  glfwSetWindowUserPointer(ctx->window, (void*)ctx);

  glfwSetKeyCallback(ctx->window, key_callback_ffm);
  glfwSetCharCallback(ctx->window, character_callback_ffm);
  glfwSetCursorPosCallback(ctx->window, cursor_position_callback_ffm);
  glfwSetMouseButtonCallback(ctx->window, mouse_button_callback_ffm);
  glfwSetScrollCallback(ctx->window, scroll_callback_ffm);
  glfwSetWindowCloseCallback(ctx->window, window_close_callback_ffm);
  glfwSetWindowSizeCallback(ctx->window, window_size_callback_ffm);

  glfwMakeContextCurrent(ctx->window);

  return ctx;
}

void uiWindowDestroy(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwDestroyWindow(ctx->window);
  ctx->window = NULL;
  free(ctx);
}

void uiWindowSetCurrent(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwMakeContextCurrent(ctx->window);
}

void uiPollEvents(GLFWContextFFM* ctx, jint wait, void* events)
{
  ctx->dispatchEvent = (void (*)(int,int,int))events;
  switch (wait) {
    case -1: {
      glfwWaitEvents();
      break;
    }
    case 0: {
      glfwPollEvents();
      break;
    }
    default: {
      double wait_d = wait;
      glfwWaitEventsTimeout(wait_d / 1000.0);
      break;
    }
  }
}

void uiPostEvent()
{
  glfwPostEmptyEvent();
}

void uiWindowShow(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwShowWindow(ctx->window);
}

void uiWindowHide(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwHideWindow(ctx->window);
}

void uiWindowSwap(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwSwapBuffers(ctx->window);
}

void uiWindowHideCursor(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwSetInputMode(ctx->window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
}

void uiWindowShowCursor(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwSetInputMode(ctx->window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
}

void uiWindowLockCursor(GLFWContextFFM* ctx)
{
  if (ctx == NULL) return;
  glfwSetInputMode(ctx->window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
}

void uiWindowGetPos(GLFWContextFFM* ctx, jint* pos)
{
  if (ctx == NULL) return;
  glfwGetWindowPos(ctx->window, &pos[0], &pos[1]);
}

void uiWindowSetPos(GLFWContextFFM* ctx, jint x, jint y)
{
  if (ctx == NULL) return;
  glfwSetWindowPos(ctx->window, x, y);
}

extern "C" {
  JNIEXPORT jboolean (*_uiInit)() = &uiInit;
  JNIEXPORT GLFWContextFFM* (*_uiWindowCreate)(jint,const char*,jint,jint,GLFWContextFFM*) = &uiWindowCreate;
  JNIEXPORT void (*_uiWindowDestroy)(GLFWContextFFM*) = &uiWindowDestroy;
  JNIEXPORT void (*_uiWindowSetCurrent)(GLFWContextFFM*) = &uiWindowSetCurrent;
//  JNIEXPORT void (*_uiWindowSetIcon)(GLFWContextFFM*, const char*,jint,jint) = &uiWindowSetIcon;
  JNIEXPORT void (*_uiPollEvents)(GLFWContextFFM*,jint,void*) = &uiPollEvents;
  JNIEXPORT void (*_uiPostEvent)() = &uiPostEvent;
  JNIEXPORT void (*_uiWindowShow)(GLFWContextFFM*) = &uiWindowShow;
  JNIEXPORT void (*_uiWindowHide)(GLFWContextFFM*) = &uiWindowHide;
  JNIEXPORT void (*_uiWindowSwap)(GLFWContextFFM*) = &uiWindowSwap;
  JNIEXPORT void (*_uiWindowHideCursor)(GLFWContextFFM*) = &uiWindowHideCursor;
  JNIEXPORT void (*_uiWindowShowCursor)(GLFWContextFFM*) = &uiWindowShowCursor;
  JNIEXPORT void (*_uiWindowLockCursor)(GLFWContextFFM*) = &uiWindowLockCursor;
  JNIEXPORT void (*_uiWindowGetPos)(GLFWContextFFM*,jint*) = &uiWindowGetPos;
  JNIEXPORT void (*_uiWindowSetPos)(GLFWContextFFM*,jint,jint) = &uiWindowSetPos;

  JNIEXPORT jboolean UIAPIinit() {return JNI_TRUE;}
}
