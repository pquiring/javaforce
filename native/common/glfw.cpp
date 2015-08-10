#include "../glfw/include/GLFW/glfw3.h"

struct GLFWContext {
  GLFWwindow *window;
  JNIEnv *env;
  jobject obj;
  jclass cls_glwindow;
  jmethodID mid_dispatch;
};

static void key_callback(GLFWwindow* window, int key, int scancode, int action, int mods)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  int type;
  if (action > 0)
    type = javaforce_gl_GLWindow_KEY_PRESS;
  else
    type = javaforce_gl_GLWindow_KEY_RELEASE;
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, type, key, 0);
}

static void character_callback(GLFWwindow* window, unsigned int codepoint)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, javaforce_gl_GLWindow_KEY_TYPED, codepoint, 0);
}

static void cursor_position_callback(GLFWwindow* window, double xpos, double ypos)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, javaforce_gl_GLWindow_MOUSE_MOVE, (int)xpos, (int)ypos);
}

static void mouse_button_callback(GLFWwindow* window, int button, int action, int mods)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  int type;
  if (action > 0)
    type = javaforce_gl_GLWindow_MOUSE_DOWN;
  else
    type = javaforce_gl_GLWindow_MOUSE_UP;
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, type, button, 0);
}

static void scroll_callback(GLFWwindow* window, double xoffset, double yoffset)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, javaforce_gl_GLWindow_MOUSE_SCROLL, (int)xoffset, (int)yoffset);
}

static void window_close_callback(GLFWwindow* window)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, javaforce_gl_GLWindow_WIN_CLOSING, 0, 0);
}

static void window_size_callback(GLFWwindow* window, int width, int height)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ctx->env->CallVoidMethod(ctx->cls_glwindow, ctx->mid_dispatch, ctx->obj, javaforce_gl_GLWindow_WIN_RESIZE, width, height);
}

JNIEXPORT jboolean JNICALL Java_javaforce_gl_GLWindow_ninit
  (JNIEnv *e, jclass c)
{
  return glfwInit() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_javaforce_gl_GLWindow_ncreate
  (JNIEnv *e, jclass c, jint style, jstring title, jint x, jint y, jobject events, jlong shared)
{
  GLFWContext *ctx = new GLFWContext();
  GLFWmonitor *monitor = NULL;
  if (style & javaforce_gl_GLWindow_STYLE_FULLSCREEN) monitor = glfwGetPrimaryMonitor();

  glfwDefaultWindowHints();
  glfwWindowHint(GLFW_VISIBLE, (style & javaforce_gl_GLWindow_STYLE_VISIBLE ? GL_TRUE : GL_FALSE));
  glfwWindowHint(GLFW_RESIZABLE, (style & javaforce_gl_GLWindow_STYLE_RESIZABLE ? GL_TRUE : GL_FALSE));
  glfwWindowHint(GLFW_DECORATED, (style & javaforce_gl_GLWindow_STYLE_TITLEBAR ? GL_TRUE : GL_FALSE));

  GLFWwindow *sharedWin = NULL;
  if (shared != 0) {
    GLFWContext *sharedCtx = (GLFWContext*)shared;
    sharedWin = sharedCtx->window;
  }

  const char *ctitle = e->GetStringUTFChars(title,NULL);
  ctx->window = glfwCreateWindow(x,y,ctitle, monitor, sharedWin);
  e->ReleaseStringUTFChars(title, ctitle);

  glfwSetWindowUserPointer(ctx->window, (void*)ctx);

  glfwSetKeyCallback(ctx->window, key_callback);
  glfwSetCharCallback(ctx->window, character_callback);
  glfwSetCursorPosCallback(ctx->window, cursor_position_callback);
  glfwSetMouseButtonCallback(ctx->window, mouse_button_callback);
  glfwSetScrollCallback(ctx->window, scroll_callback);
  glfwSetWindowCloseCallback(ctx->window, window_close_callback);
  glfwSetWindowSizeCallback(ctx->window, window_size_callback);

  ctx->cls_glwindow = e->GetObjectClass(events);
  ctx->mid_dispatch = e->GetMethodID(ctx->cls_glwindow, "dispatchEvent", "(III)V");
  ctx->env = e;
  ctx->obj = events;

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_gl_GLWindow_npoll
  (JNIEnv *e, jclass c)
{
  glfwPollEvents();
}

JNIEXPORT void JNICALL Java_javaforce_gl_GLWindow_nshow
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwShowWindow(ctx->window);
}

JNIEXPORT void JNICALL Java_javaforce_gl_GLWindow_nhide
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwHideWindow(ctx->window);
}
