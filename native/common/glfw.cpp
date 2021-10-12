#include "../glfw/include/GLFW/glfw3.h"

struct GLFWContext {
  GLFWwindow *window;
  jobject obj;
  jmethodID mid_dispatch;
};

static JNIEnv *ge;

static void key_callback(GLFWwindow* window, int key, int scancode, int action, int mods)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  int type;
  if (action > 0)
    type = javaforce_ui_Window_KEY_PRESS;
  else
    type = javaforce_ui_Window_KEY_RELEASE;
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, type, key, 0);
}

static void character_callback(GLFWwindow* window, unsigned int codepoint)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, javaforce_ui_Window_KEY_TYPED, codepoint, 0);
}

static void cursor_position_callback(GLFWwindow* window, double xpos, double ypos)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, javaforce_ui_Window_MOUSE_MOVE, (int)xpos, (int)ypos);
}

static void mouse_button_callback(GLFWwindow* window, int button, int action, int mods)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  int type;
  if (action > 0)
    type = javaforce_ui_Window_MOUSE_DOWN;
  else
    type = javaforce_ui_Window_MOUSE_UP;
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, type, ++button, 0);
}

static void scroll_callback(GLFWwindow* window, double xoffset, double yoffset)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, javaforce_ui_Window_MOUSE_SCROLL, (int)-xoffset, (int)-yoffset);
}

static void window_close_callback(GLFWwindow* window)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, javaforce_ui_Window_WIN_CLOSING, 0, 0);
}

static void window_size_callback(GLFWwindow* window, int width, int height)
{
  GLFWContext *ctx = (GLFWContext*)glfwGetWindowUserPointer(window);
  ge->CallVoidMethod(ctx->obj, ctx->mid_dispatch, javaforce_ui_Window_WIN_RESIZE, width, height);
}

JNIEXPORT jboolean JNICALL Java_javaforce_ui_Window_ninit
  (JNIEnv *e, jclass c)
{
  return glfwInit() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_javaforce_ui_Window_ncreate
  (JNIEnv *e, jclass c, jint style, jstring title, jint x, jint y, jobject events, jlong shared)
{
  GLFWContext *ctx = new GLFWContext();
  ge = e;
  memset(ctx, 0, sizeof(GLFWContext));
  GLFWmonitor *monitor = NULL;
  if (style & javaforce_ui_Window_STYLE_FULLSCREEN) monitor = glfwGetPrimaryMonitor();

  glfwDefaultWindowHints();
  glfwWindowHint(GLFW_VISIBLE, (style & javaforce_ui_Window_STYLE_VISIBLE ? GL_TRUE : GL_FALSE));
  glfwWindowHint(GLFW_RESIZABLE, (style & javaforce_ui_Window_STYLE_RESIZABLE ? GL_TRUE : GL_FALSE));
  glfwWindowHint(GLFW_DECORATED, (style & javaforce_ui_Window_STYLE_TITLEBAR ? GL_TRUE : GL_FALSE));

  GLFWwindow *sharedWin = NULL;
  if (shared != 0) {
    GLFWContext *sharedCtx = (GLFWContext*)shared;
    sharedWin = sharedCtx->window;
  }

  int px, py;
  const GLFWvidmode *vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

  if (!(style & javaforce_ui_Window_STYLE_FULLSCREEN)) {
    px = (vidmode->width - x) / 2;
    py = (vidmode->height - y) / 2;
  }

  const char *ctitle = e->GetStringUTFChars(title,NULL);
  ctx->window = glfwCreateWindow(x,y,ctitle, monitor, sharedWin);
  e->ReleaseStringUTFChars(title, ctitle);

  if (!(style & javaforce_ui_Window_STYLE_FULLSCREEN)) {
    glfwSetWindowPos(ctx->window, px, py);
  }

  glfwSetWindowUserPointer(ctx->window, (void*)ctx);

  ctx->obj = e->NewGlobalRef(events);
  jclass cls = e->GetObjectClass(events);
  ctx->mid_dispatch = e->GetMethodID(cls, "dispatchEvent", "(III)V");

  glfwSetKeyCallback(ctx->window, key_callback);
  glfwSetCharCallback(ctx->window, character_callback);
  glfwSetCursorPosCallback(ctx->window, cursor_position_callback);
  glfwSetMouseButtonCallback(ctx->window, mouse_button_callback);
  glfwSetScrollCallback(ctx->window, scroll_callback);
  glfwSetWindowCloseCallback(ctx->window, window_close_callback);
  glfwSetWindowSizeCallback(ctx->window, window_size_callback);

  glfwMakeContextCurrent(ctx->window);

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_ndestroy
  (JNIEnv *e, jclass c, jlong id)
{
  if (id == 0L) return;
  GLFWContext *ctx = (GLFWContext*)id;
  glfwDestroyWindow(ctx->window);
  ctx->window = NULL;
  e->DeleteGlobalRef(ctx->obj);
  delete ctx;
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nsetcurrent
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwMakeContextCurrent(ctx->window);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_pollEvents
  (JNIEnv *e, jclass c, jint wait)
{
  ge = e;
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

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nshow
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwShowWindow(ctx->window);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nhide
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwHideWindow(ctx->window);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nswap
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  ge = e;
  glfwSwapBuffers(ctx->window);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nhidecursor
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwSetInputMode(ctx->window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nshowcursor
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwSetInputMode(ctx->window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nlockcursor
  (JNIEnv *e, jclass c, jlong id)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwSetInputMode(ctx->window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_ngetpos
  (JNIEnv *e, jclass c, jlong id, jintArray ia)
{
  GLFWContext *ctx = (GLFWContext*)id;
  int pos[2];
  glfwGetWindowPos(ctx->window, &pos[0], &pos[1]);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)&pos);
}

JNIEXPORT void JNICALL Java_javaforce_ui_Window_nsetpos
  (JNIEnv *e, jclass c, jlong id, jint x, jint y)
{
  GLFWContext *ctx = (GLFWContext*)id;
  glfwSetWindowPos(ctx->window, x, y);
}
