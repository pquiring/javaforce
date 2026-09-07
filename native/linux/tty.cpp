#if defined(__FreeBSD__)
#define CONSOLE "/dev/console"
#else
#define CONSOLE "/dev/tty0"
#endif

jboolean ttySetActiveVT(int number) {
  int fd = open(CONSOLE, O_RDONLY | O_NOCTTY, 0);
  if (fd < 0) {
    printf("ttySetActiveVT:open(tty) failed\n");
    return JNI_FALSE;
  }
  if (ioctl(fd, VT_ACTIVATE, number) < 0) {
    printf("ttySetActiveVT:ioctl(VT_ACTIVATE) failed\n");
    return JNI_FALSE;
  }
  //wait for VT to become active
  while (true) {
    if (ioctl(fd, VT_WAITACTIVE, number) < 0) {
      if (errno == EINTR) continue;
      printf("ttySetActiveVT:ioctl(VT_WAITACTIVE) failed\n");
      return JNI_FALSE;
    }
    break;
  }
  close(fd);
  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jboolean (*_ttySetActiveVT)(int) = &ttySetActiveVT;
}
