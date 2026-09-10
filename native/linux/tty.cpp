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
  setsid();
  return JNI_TRUE;
}

jboolean ttyFreeVT(int number) {
  int fd = open(CONSOLE, O_RDONLY | O_NOCTTY, 0);
  if (fd < 0) {
    printf("ttyFreeVT:open(tty) failed\n");
    return JNI_FALSE;
  }
  if (ioctl(fd, VT_DISALLOCATE, number) < 0) {
    printf("ttyFreeVT:ioctl(VT_DISALLOCATE) failed\n");
    return JNI_FALSE;
  }
  close(fd);
  return JNI_TRUE;
}

jboolean ttyTakeOwnership() {
  int fd = open(CONSOLE, O_RDONLY | O_NOCTTY, 0);
  if (fd < 0) {
    printf("ttyTakeOwnership:open(tty) failed\n");
    return JNI_FALSE;
  }
  if (ioctl(fd, TIOCSCTTY, 0) < 0) {
    printf("ttyTakeOwnership:ioctl(TIOCSCTTY) failed\n");
    return JNI_FALSE;
  }
  close(fd);
  return JNI_TRUE;
}

jboolean ttyReleaseOwnership() {
  int fd = open(CONSOLE, O_RDONLY | O_NOCTTY, 0);
  if (fd < 0) {
    printf("ttyReleaseOwnership:open(tty) failed\n");
    return JNI_FALSE;
  }
  if (ioctl(fd, TIOCNOTTY) < 0) {
    printf("ttyReleaseOwnership:ioctl(TIOCNOTTY) failed\n");
    return JNI_FALSE;
  }
  close(fd);
  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jboolean (*_ttySetActiveVT)(int) = &ttySetActiveVT;
  JNIEXPORT jboolean (*_ttyFreeVT)(int) = &ttyFreeVT;
  JNIEXPORT jboolean (*_ttyTakeOwnership)() = &ttyTakeOwnership;
  JNIEXPORT jboolean (*_ttyReleaseOwnership)() = &ttyReleaseOwnership;
}
