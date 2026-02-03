//Com Port JNI API

jlong comOpen(const char* str, jint baud)
{
  int baudcode = -1;
  switch (baud) {
    case 9600: baudcode = 015; break;
    case 19200: baudcode = 016; break;
    case 38400: baudcode = 017; break;
    case 57600: baudcode = 010001; break;
    case 115200: baudcode = 010002; break;
  }
  if (baudcode == -1) {
    printf("LnxCom:Unknown baud rate\n");
    return 0;
  }
  int fd = open(str, O_RDWR | O_NOCTTY);
  if (fd == -1) {
    printf("LnxCom:invalid handle\n");
    return 0;
  }

  tcgetattr(fd, &orgattrs);
  termios attrs;
  memset(&attrs, 0, sizeof(termios));
  attrs.c_cflag = baudcode | CS8 | CLOCAL | CREAD;

  attrs.c_cc[VMIN]  =  1;          // block until at least 1 char
  attrs.c_cc[VTIME] =  5;          // 0.5 seconds read timeout

  tcflush(fd, TCIFLUSH);
  tcsetattr(fd, TCSANOW, &attrs);

  int* handle = (int*)malloc(sizeof(int*));
  *handle = fd;

  return (jlong)handle;
}

void comClose(jlong handleptr)
{
  if (handleptr == 0) return;
  int* handle = (int*)handleptr;
  if (*handle == 0) return;
  tcsetattr(*handle, TCSANOW, &orgattrs);
  close(*handle);
  *handle = 0;
  free(handle);
}

jint comRead(jlong handleptr, jbyte* ba, jint size)
{
  if (handleptr == 0) return -1;
  int* handle = (int*)handleptr;
  if (*handle == 0) return -1;
  int readAmt = read(*handle, ba, size);
  return readAmt;
}

jint comWrite(jlong handleptr, jbyte* ba, jint size)
{
  if (handleptr == 0) return -1;
  int* handle = (int*)handleptr;
  if (*handle == 0) return -1;
  int writeAmt = write(*handle, ba, size);
  return writeAmt;
}

extern "C" {
  JNIEXPORT jlong (*_comOpen)(const char* str, jint baud) = &comOpen;
  JNIEXPORT void (*_comClose)(jlong handle) = &comClose;
  JNIEXPORT jint (*_comRead)(jlong handle, jbyte* ba, jint size) = &comRead;
  JNIEXPORT jint (*_comWrite)(jlong handle, jbyte* ba, jint size) = &comWrite;

  JNIEXPORT jboolean JNICALL ComPortAPIinit() {return JNI_TRUE;}
}
