struct Pty {
  int master;
  char *slaveName;
  jboolean closed;
};

jlong ptyAlloc()
{
  Pty *pty;
  pty = (Pty*)malloc(sizeof(Pty));
  memset(pty, 0, sizeof(Pty));
  return (jlong)pty;
}

void ptyFree(jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  free(pty);
}

const char* ptyOpen(jlong ctx)
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
  return pty->slaveName;
}

void ptyClose(jlong ctx)
{
  Pty *pty = (Pty*)ctx;
  if (pty->master != 0) close(pty->master);
  pty->closed = JNI_TRUE;
}

jint ptyRead(jlong ctx, jbyte* baptr, int offset, int length)
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
    int readAmt = read(pty->master, baptr + offset, length);
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

void ptyWrite(jlong ctx, jbyte* baptr, int offset, int length)
{
  Pty *pty = (Pty*)ctx;
  int res = write(pty->master, baptr + offset, length);
}

void ptySetSize(jlong ctx, jint x, jint y)
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

int stringArrayLength(const char**arr) {
  int cnt = 0;
  while (*arr != NULL) {
    cnt++;
    arr++;
  }
  return cnt;
}

jlong ptyChildExec(const char* slaveName, const char* cmd, const char** args, const char** env)
{
  int slave = open(slaveName, O_RDWR);
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
  int nargs = stringArrayLength(args);
  char **cargs = (char **)malloc((nargs+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nargs;a++) {
    const char* cstr = args[a];
    int sl = strlen(cstr);
    cargs[a] = (char*)malloc(sl+1);
    strcpy(cargs[a], cstr);
  }
  cargs[nargs] = NULL;

  //build env
  int nenv = stringArrayLength(env);
  char **cenv = (char **)malloc((nenv+1) * sizeof(char*));  //+1 NULL terminator
  for(int a=0;a<nenv;a++) {
    const char* cstr = env[a];
    int sl = strlen(cstr);
    cenv[a] = (char*)malloc(sl+1);
    strcpy(cenv[a], cstr);
  }
  cenv[nenv] = NULL;

  execvpe(cmd, cargs, cenv);
  return 0;
}

extern "C" {
  JNIEXPORT jlong (*_ptyAlloc)() = &ptyAlloc;
  JNIEXPORT void (*_ptyFree)(jlong) = &ptyFree;
  JNIEXPORT const char* (*_ptyOpen)(jlong) = &ptyOpen;
  JNIEXPORT void (*_ptyClose)(jlong) = &ptyClose;
  JNIEXPORT jint (*_ptyRead)(jlong,jbyte*,int,int) = &ptyRead;
  JNIEXPORT void (*_ptyWrite)(jlong,jbyte*,int,int) = &ptyWrite;
  JNIEXPORT void (*_ptySetSize)(jlong,jint,jint) = &ptySetSize;
  JNIEXPORT jlong (*_ptyChildExec)(const char*,const char*,const char**,const char**) = &ptyChildExec;
}
