//Com Port JNI API

static termios orgattrs;

JNIEXPORT jlong JNICALL Java_javaforce_jni_ComPortJNI_comOpen
  (JNIEnv *e, jclass c, jstring str, jint baud)
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
  const char *cstr = e->GetStringUTFChars(str,NULL);
  int fd = open(cstr, O_RDWR | O_NOCTTY);
  e->ReleaseStringUTFChars(str, cstr);
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

JNIEXPORT void JNICALL Java_javaforce_jni_ComPortJNI_comClose
  (JNIEnv *e, jclass c, jlong handleptr)
{
  if (handleptr == 0) return;
  int* handle = (int*)handleptr;
  if (*handle == 0) return;
  tcsetattr(*handle, TCSANOW, &orgattrs);
  close(*handle);
  *handle = 0;
  free(handle);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_ComPortJNI_comRead
  (JNIEnv *e, jclass c, jlong handleptr, jbyteArray ba, jint size)
{
  if (handleptr == 0) return -1;
  int* handle = (int*)handleptr;
  if (*handle == 0) return -1;
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int readAmt = read(*handle, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return readAmt;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_ComPortJNI_comWrite
  (JNIEnv *e, jclass c, jlong handleptr, jbyteArray ba, jint size)
{
  if (handleptr == 0) return -1;
  int* handle = (int*)handleptr;
  if (*handle == 0) return -1;
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int writeAmt = write(*handle, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return writeAmt;
}
