//Console

jint* getConsoleSize()
{
  CONSOLE_SCREEN_BUFFER_INFO info;
  jint* xy = ffm->newIntArray(2);
  GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
  xy[0] = info.srWindow.Right - info.srWindow.Left + 1;
  xy[1] = info.srWindow.Bottom - info.srWindow.Top + 1;
  return xy;
}

jint* getConsolePos()
{
  CONSOLE_SCREEN_BUFFER_INFO info;
  jint* xy = ffm->newIntArray(2);
  GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info);
  xy[0] = info.dwCursorPosition.X - info.srWindow.Left + 1;
  xy[1] = info.dwCursorPosition.Y - info.srWindow.Top + 1;
  return xy;
}

static DWORD input_console_mode;
static DWORD output_console_mode;
static char console_buffer[8];

#ifndef ENABLE_PROCESSED_INPUT
#define ENABLE_PROCESSED_INPUT 0x0001
#endif

#ifndef ENABLE_VIRTUAL_TERMINAL_INPUT
#define ENABLE_VIRTUAL_TERMINAL_INPUT 0x0200
#endif

#ifndef ENABLE_PROCESSED_OUTPUT
#define ENABLE_PROCESSED_OUTPUT 0x0001
#endif

#ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
#define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
#endif

#ifndef DISABLE_NEWLINE_AUTO_RETURN
#define DISABLE_NEWLINE_AUTO_RETURN 0x0008
#endif

#ifndef KEY_EVENT
#define KEY_EVENT 0x0001
#endif

static void StringCopy(char *dest, const char *src) {
  while (*src != 0) {
    *(dest++) = (*src++);
  }
  *dest = *src;
}

void enableConsoleMode()
{
  GetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), &input_console_mode);
  GetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), &output_console_mode);

  if (!SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), ENABLE_VIRTUAL_TERMINAL_INPUT)) {
    printf("Error:Unable to set stdin mode\n");
    exit(1);
  }
  if (!SetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), ENABLE_PROCESSED_OUTPUT | ENABLE_VIRTUAL_TERMINAL_PROCESSING | DISABLE_NEWLINE_AUTO_RETURN)) {
    printf("Error:Unable to set stdout mode\n");
    exit(1);
  }
  console_buffer[0] = 0;
}

void disableConsoleMode()
{
  SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), input_console_mode);
  SetConsoleMode(GetStdHandle(STD_OUTPUT_HANDLE), output_console_mode);
}

jchar readConsole()
{
  INPUT_RECORD input;
  DWORD read;
  if (console_buffer[0] != 0) {
    char ret = console_buffer[0];
    StringCopy(console_buffer, console_buffer+1);
    return (jchar)ret;
  }
  ReadConsoleInput(GetStdHandle(STD_INPUT_HANDLE), &input, 1, &read);
  if (input.EventType != KEY_EVENT) return 0;
  if (!input.Event.KeyEvent.bKeyDown) return 0;
  if (input.Event.KeyEvent.uChar.AsciiChar != 0) {
    char ch = input.Event.KeyEvent.uChar.AsciiChar;
    if (ch == 0x1b) {
      //is it Escape key or ANSI code???
      for(int a=0;a<10;a++) {
        read = 0;
        PeekConsoleInput(GetStdHandle(STD_INPUT_HANDLE), &input, 1, &read);
        if (read == 1) {
          char ch2 = input.Event.KeyEvent.uChar.AsciiChar;
          if (input.EventType == KEY_EVENT && input.Event.KeyEvent.bKeyDown && ch2 != 0) {
            if (ch2 != 0x1b) {
              //must be an ANSI code
              return (jchar)ch;
            } else {
              //multiple esc chars - prev must be esc key
              break;
            }
          } else {
            //ignore non-ascii events
            ReadConsoleInput(GetStdHandle(STD_INPUT_HANDLE), &input, 1, &read);
            continue;
          }
        }
        Sleep(10);
      }
      //it must be Escape key
      StringCopy(console_buffer, "[1~");  //custom code
    }
    if (ch == 13) ch = 10;  //linux style
    return (jchar)ch;
  }
  bool shift = input.Event.KeyEvent.dwControlKeyState & SHIFT_PRESSED;
  bool ctrl = input.Event.KeyEvent.dwControlKeyState & (LEFT_CTRL_PRESSED | RIGHT_CTRL_PRESSED);
  bool alt = input.Event.KeyEvent.dwControlKeyState & (LEFT_ALT_PRESSED | RIGHT_ALT_PRESSED);
  char code = 0;
  if (shift && ctrl && alt) {
    code = '8';
  } else if (ctrl && alt) {
    code = '7';
  } else if (ctrl && shift) {
    code = '6';
  } else if (ctrl) {
    code = '5';
  } else if (alt && shift) {
    code = '4';
  } else if (alt) {
    code = '3';
  } else if (shift) {
    code = '2';
  }
  switch (input.Event.KeyEvent.wVirtualKeyCode) {
    case VK_ESCAPE: StringCopy(console_buffer, "\x1b[1~"); break;  //custom
    case VK_INSERT: StringCopy(console_buffer, "\x1b[2~"); break;
    case VK_DELETE: StringCopy(console_buffer, "\x1b[3~"); break;
    case VK_UP: StringCopy(console_buffer, "\x1b[1;0A"); break;
    case VK_DOWN: StringCopy(console_buffer, "\x1b[1;0B"); break;
    case VK_RIGHT: StringCopy(console_buffer, "\x1b[1;0C"); break;
    case VK_LEFT: StringCopy(console_buffer, "\x1b[1;0D"); break;
    case VK_HOME: StringCopy(console_buffer, "\x1b[1;0H"); break;
    case VK_END: StringCopy(console_buffer, "\x1b[1;0F"); break;
  }
  if (console_buffer[0] != 0) {
    if (code > 0 && console_buffer[3] != '~') {
      console_buffer[4] = code;
    }
    char ret = console_buffer[0];
    StringCopy(console_buffer, console_buffer+1);
    return (jchar)ret;
  }
  return 0;
}

jboolean peekConsole()
{
  DWORD count;
  GetNumberOfConsoleInputEvents(GetStdHandle(STD_INPUT_HANDLE), &count);
  return count != 0;
}

/**
  UTF8 Format:
    1st bytes:
      0xxxxxxx = 7bit (1 byte)
      110xxxxx = 5bit (2 byte) 11bit total (import supported) (not exported)
      1110xxxx = 4bit (3 byte) 16bit total (import/export supported)
      11110xxx = 3bit (4 byte) 21bit total (import truncated to 16bit) (not exported)
    2nd,3rd,4th bytes only:
      10xxxxxx = 6bit
*/

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
      std::memcpy(tmp, baptr+pos, 127);
      tmp[127] = 0;
      length -= 127;
      pos += 127;
    } else {
      std::memcpy(tmp, baptr+pos, length);
      tmp[length] = 0;
      length = 0;
    }
    printf("%s", tmp);
  }
}

extern "C" {
  JNIEXPORT void (*_enableConsoleMode)() = &enableConsoleMode;
  JNIEXPORT void (*_disableConsoleMode)() = &disableConsoleMode;
  JNIEXPORT jint* (*_getConsoleSize)() = &getConsoleSize;
  JNIEXPORT jint* (*_getConsolePos)() = &getConsolePos;
  JNIEXPORT jchar (*_readConsole)() = &readConsole;
  JNIEXPORT jboolean (*_peekConsole)() = &peekConsole;
  JNIEXPORT void (*_writeConsole)(jint) = &writeConsole;
  JNIEXPORT void (*_writeConsoleArray)(jbyte*, int, int) = &writeConsoleArray;
}
