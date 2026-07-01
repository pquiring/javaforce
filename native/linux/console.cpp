jint* getConsoleSize()
{
  jint* xy = ffm->newIntArray(2);
  struct winsize w;
  ioctl(STDOUT_FILENO, TIOCGWINSZ, &w);
  xy[0] = w.ws_col;
  xy[1] = w.ws_row;
  return xy;
}

jint* getConsolePos()
{
  jint* xy = ffm->newIntArray(2);
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
  return xy;
}

static char console_buffer[8];

static void StringCopy(char *dest, const char *src) {
  while (*src != 0) {
    *(dest++) = (*src++);
  }
  *dest = *src;
}

static struct termios oldt, newt;

void enableConsoleMode()
{
  console_buffer[0] = 0;
  (*_initscr)();
  (*_raw)();
  (*_noecho)();
  (*_wtimeout)(*_stdscr, 0);
  (*_wgetch)(*_stdscr);  //first call to wgetch() clears the screen
  (*_wtimeout)(*_stdscr, -1);
}

void disableConsoleMode()
{
  (*_endwin)();
}

jchar readConsole()
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

jboolean peekConsole()
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

void writeConsole(jint ch)
{
  printf("%c", ch);
}

void writeConsoleArray(jbyte* baptr, jint off, jint len)
{
  jbyte tmp[128];
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
}

extern "C" {
  JNIEXPORT jint* (*_getConsoleSize)() = &getConsoleSize;
  JNIEXPORT jint* (*_getConsolePos)() = &getConsolePos;
  JNIEXPORT void (*_enableConsoleMode)() = &enableConsoleMode;
  JNIEXPORT void (*_disableConsoleMode)() = &disableConsoleMode;
  JNIEXPORT jchar (*_readConsole)() = &readConsole;
  JNIEXPORT jboolean (*_peekConsole)() = &peekConsole;
  JNIEXPORT void (*_writeConsole)(jint) = &writeConsole;
  JNIEXPORT void (*_writeConsoleArray)(jbyte*,jint,jint) = &writeConsoleArray;
}
