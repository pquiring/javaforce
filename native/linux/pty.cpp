struct Pty {
  int master;
  char *slaveName;
  jboolean closed;
};

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_ptyAlloc
  (JNIEnv *e, jclass c)
{
  Pty *pty;
  pty = (Pty*)malloc(sizeof(Pty));
  memset(pty, 0, sizeof(Pty));
  return (jlong)pty;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyFree
  (JNIEnv *e, jclass c, jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  free(pty);
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_LnxNative_ptyOpen
  (JNIEnv *e, jclass c, jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  pty->master = posix_openpt(O_RDWR | O_NOCTTY);
  if (pty->master == -1) {
    printf("LnxPty:failed to alloc pty\n");
    pty->master = 0;
    return NULL;
  }
  pty->slaveName = ptsname(pty->master);
  if (pty->slaveName == NULL) {
    printf("LnxPty:slaveName == NULL\n");
    return NULL;
  }
  if (grantpt(pty->master) != 0) {
    printf("LnxPty:grantpt() failed\n");
    return NULL;
  }
  if (unlockpt(pty->master) != 0) {
    printf("LnxPty:unlockpt() failed\n");
    return NULL;
  }
  return e->NewStringUTF(pty->slaveName);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyClose
  (JNIEnv *e, jclass c, jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  if (pty->master != 0) close(pty->master);
  pty->closed = JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_LnxNative_ptyRead
  (JNIEnv *e, jclass c, jlong ctx, jbyteArray ba)
{
  Pty *pty = (Pty*)ctx;

  timeval timeout;
  timeout.tv_sec = 0;
  timeout.tv_usec = 100 * 1000;  //100ms
  fd_set read_set;
  FD_SET(pty->master, &read_set);

  fd_set error_set;
  FD_SET(pty->master, &error_set);

  int res = select(pty->master+1, &read_set, NULL, &error_set, &timeout);
  if (res == -1) {
    printf("LnxPty:select() : unknown error:%d:%d\n", res ,errno);
    return -1;
  }
  if (res == 0) {
    //timeout
    sleep_ms(100);  //sometimes select() will timeout instantly
    return 0;
  }

  if (pty->closed) {
    printf("LnxPty:select() : closed\n");
    return -1;
  }

  if (FD_ISSET(pty->master, &error_set)) {
    printf("LnxPty:select() : error_set\n");
    return -1;
  }
  if (FD_ISSET(pty->master, &read_set)) {
    jbyte *baptr = e->GetByteArrayElements(ba,NULL);
    int readAmt = read(pty->master, baptr, e->GetArrayLength(ba));
    e->ReleaseByteArrayElements(ba, baptr, 0);
    if (readAmt < 0) {
      printf("LnxPty:read() failed:%d:%d\n", readAmt, errno);
      return -1;
    }
    return readAmt;
  }
  //Warning:this does happen until the child process opens the pty
  //printf("LnxPty:select() : unknown reason:%d:%d\n", res, errno);
  sleep_ms(100);  //avoid 100% CPU usage
  return 0;
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptyWrite
  (JNIEnv *e, jclass c, jlong ctx, jbyteArray ba)
{
  Pty *pty = (Pty*)ctx;
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int res = write(pty->master, baptr, e->GetArrayLength(ba));
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_javaforce_jni_LnxNative_ptySetSize
  (JNIEnv *e, jclass c, jlong ctx, jint x, jint y)
{
  Pty *pty = (Pty*)ctx;
  winsize size;
  memset(&size, 0, sizeof(winsize));
  size.ws_row = (short)y;
  size.ws_col = (short)x;
  size.ws_xpixel = (short)(x*8);
  size.ws_ypixel = (short)(y*8);
  ioctl(pty->master, TIOCSWINSZ, &size);
}

#ifdef __FreeBSD__
#define execvpe exect  //different name
#ifndef IUTF8
#define IUTF8 0  //not supported
#endif
#endif

JNIEXPORT jlong JNICALL Java_javaforce_jni_LnxNative_ptyChildExec
  (JNIEnv *e, jclass c, jstring slaveName, jstring cmd, jobjectArray args, jobjectArray env)
{
  const char *cslaveName = e->GetStringUTFChars(slaveName,NULL);
  int slave = open(cslaveName, O_RDWR);
  e->ReleaseStringUTFChars(slaveName, cslaveName);
  if (slave == -1) {
    printf("LnxPty:unable to open slave pty\n");
    exit(0);
  }
  if (setsid() == -1) {
    printf("LnxPty:unable to setsid\n");
    exit(0);
  }
  termios attrs;
  memset(&attrs, 0, sizeof(termios));
  tcgetattr(slave, &attrs);
  // Assume input is UTF-8; this allows character-erase to be correctly performed in cooked mode.
  attrs.c_iflag |= IUTF8;
  // Humans don't need XON/XOFF flow control of output, and it only serves to confuse those who accidentally hit ^S or ^Q, so turn it off.
  attrs.c_iflag &= ~IXON;
  // ???
  attrs.c_cc[VERASE] = 127;
  tcsetattr(slave, TCSANOW, &attrs);
  dup2(slave, STDIN_FILENO);
  dup2(slave, STDOUT_FILENO);
  dup2(slave, STDERR_FILENO);
  signal(SIGINT, SIG_DFL);
  signal(SIGQUIT, SIG_DFL);
  signal(SIGCHLD, SIG_DFL);

  //build args
  int nargs = e->GetArrayLength(args);
  char **cargs = (char **)malloc((nargs+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nargs;a++) {
    jstring jstr = (jstring)e->GetObjectArrayElement(args, a);
    const char *cstr = e->GetStringUTFChars(jstr,NULL);
    int sl = strlen(cstr);
    cargs[a] = (char*)malloc(sl+1);
    strcpy(cargs[a], cstr);
    e->ReleaseStringUTFChars(jstr, cstr);
  }
  cargs[nargs] = NULL;

  //build env
  int nenv = e->GetArrayLength(env);
  char **cenv = (char **)malloc((nenv+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nenv;a++) {
    jstring jstr = (jstring)e->GetObjectArrayElement(env, a);
    const char *cstr = e->GetStringUTFChars(jstr,NULL);
    int sl = strlen(cstr);
    cenv[a] = (char*)malloc(sl+1);
    strcpy(cenv[a], cstr);
    e->ReleaseStringUTFChars(jstr, cstr);
  }
  cenv[nenv] = NULL;

  const char *ccmd = e->GetStringUTFChars(cmd, NULL);
  execvpe(ccmd, cargs, cenv);
  e->ReleaseStringUTFChars(cmd, ccmd);
  return 0;
}
