JNIEXPORT jintArray JNICALL Java_javaforce_jni_LnxNative_getConsoleSize
  (JNIEnv *e, jclass c)
{
  int xy[2];
  struct winsize w;
  ioctl(STDOUT_FILENO, TIOCGWINSZ, &w);
  xy[0] = w.ws_col;
  xy[1] = w.ws_row;
  jintArray ia = e->NewIntArray(2);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)xy);
  return ia;
}

JNIEXPORT jintArray JNICALL Java_javaforce_jni_LnxNative_getConsolePos
  (JNIEnv *e, jclass c)
{
  int xy[2];
  //print ESC[6n
  printf("\x1b[6n");
  int x = 1;
  int y = 1;
  char t;
  int val = 0;
  //reply = ESC[row;colR
  while (1) {
    t = fgetc(stdin);
    if (t == '\x1b') continue;
    if (t == '[') continue;
    if (t == 'R') {
      x = val;
      break;
    }
    if (t == ';') {
      y = val;
      val = 0;
    } else {
      val *= 10;
      val += (t - '0');
    }
  }
  xy[0] = x;
  xy[1] = y;
  jintArray ia = e->NewIntArray(2);
  e->SetIntArrayRegion(ia, 0, 2, (const jint*)xy);
  return ia;
}

static char console_buffer[8];

static void StringCopy(char *dest, const char *src) {
  while (*src != 0) {
    *(dest++) = (*src++);
  }
  *dest = *src;
}

static struct termios oldt, newt;

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_enableConsoleMode
  (JNIEnv *e, jclass c)
{
  console_buffer[0] = 0;
  (*_initscr)();
  (*_raw)();
  (*_noecho)();
  (*_wtimeout)(*_stdscr, 0);
  (*_wgetch)(*_stdscr);  //first call to wgetch() clears the screen
  (*_wtimeout)(*_stdscr, -1);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_disableConsoleMode
  (JNIEnv *e, jclass c)
{
  (*_endwin)();
}

JNIEXPORT jchar JNICALL Java_javaforce_jni_LnxNative_readConsole
  (JNIEnv *e, jclass c)
{
  if (console_buffer[0] != 0) {
    char ret = console_buffer[0];
    StringCopy(console_buffer, console_buffer+1);
    return (jchar)ret;
  }
  (*_wtimeout)(*_stdscr, -1);
  char ch = (*_wgetch)(*_stdscr);
  if (ch == 0x1b) {
    //is it Escape key or ANSI code???
    (*_wtimeout)(*_stdscr, 100);
    char ch2 = (*_wgetch)(*_stdscr);  //waits 100ms max
    (*_wtimeout)(*_stdscr, -1);
    if (ch2 == ERR) {
      StringCopy(console_buffer, "[1~");  //custom ansi code for esc
    } else {
      if (ch2 == 0x1b) {
        (*_ungetch)(ch2);
        StringCopy(console_buffer, "[1~");  //custom ansi code for esc
      } else {
        console_buffer[0] = ch2;
        console_buffer[1] = 0;
      }
    }
  }
  return (jchar)ch;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_peekConsole
  (JNIEnv *e, jclass c)
{
  if (console_buffer[0] != 0) return JNI_TRUE;
  (*_wtimeout)(*_stdscr, 0);
  char ch = (*_wgetch)(*_stdscr);
  if (ch == 0x1b) {
    console_buffer[0] = 0x1b;
    //is it Escape key or ANSI code???
    (*_wtimeout)(*_stdscr, 100);
    char ch2 = (*_wgetch)(*_stdscr);  //waits 100ms max
    (*_wtimeout)(*_stdscr, -1);
    if (ch2 == ERR) {
      StringCopy(console_buffer+1, "[1~");  //custom ansi code for esc
    } else {
      if (ch2 == 0x1b) {
        (*_ungetch)(ch2);
        StringCopy(console_buffer+1, "[1~");  //custom ansi code for esc
      } else {
        console_buffer[1] = ch2;
        console_buffer[2] = 0;
        return JNI_TRUE;
      }
    }
  }
  if (ch == ERR) {
    return JNI_FALSE;
  } else {
    console_buffer[0] = ch;
    console_buffer[1] = 0;
    return JNI_TRUE;
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_writeConsole
  (JNIEnv *e, jclass c, jint ch)
{
  printf("%c", ch);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_writeConsoleArray
  (JNIEnv *e, jclass c, jbyteArray ba, jint off, jint len)
{
  jbyte tmp[128];
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int length = len;
  int pos = off;
  while (length > 0) {
    if (length > 127) {
      memcpy(tmp, baptr+pos, 127);
      tmp[127] = 0;
      length -= 127;
      pos += 127;
    } else {
      memcpy(tmp, baptr+pos, length);
      tmp[length] = 0;
      length = 0;
    }
    printf("%s", tmp);
  }
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
}
