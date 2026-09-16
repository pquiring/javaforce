int usOpen() {
  return socket(AF_UNIX, SOCK_STREAM, 0);
}

jboolean usBind(int fd, const char* name) {
  struct sockaddr_un addr;
  memset(&addr, 0, sizeof(addr));
  addr.sun_family = AF_UNIX;
  strncpy(addr.sun_path, name, sizeof(addr.sun_path) - 1);
  return bind(fd, (struct sockaddr *)&addr, sizeof(addr)) != -1;
}

jboolean usListen(int fd) {
  return listen(fd, 5) != -1;
}

int usAccept(int fd) {
  struct sockaddr_un addr;
  socklen_t client_len = sizeof(addr);
  return accept(fd, (struct sockaddr *)&addr, &client_len);
}

jboolean usConnect(int fd, const char* name) {
  struct sockaddr_un addr;
  memset(&addr, 0, sizeof(addr));
  addr.sun_family = AF_UNIX;
  strncpy(addr.sun_path, name, sizeof(addr.sun_path) - 1);
  return connect(fd, (struct sockaddr *)&addr, sizeof(addr)) != -1;
}

jboolean usRead(int fd, int* len_data, char* data, int* len_fds, int* fds) {
  int max_fds = len_fds[0];

  struct iovec iov;
  iov.iov_base = data;
  iov.iov_len = len_data[0];

  char cmsg_buf[CMSG_SPACE(sizeof(int) * max_fds)];
  struct msghdr msg = {};
  msg.msg_iov = &iov;
  msg.msg_iovlen = 1;
  msg.msg_control = cmsg_buf;
  msg.msg_controllen = sizeof(cmsg_buf);

  ssize_t n = recvmsg(fd, &msg, 0);
  if (n < 0) return JNI_FALSE;

  len_data[0] = n;

  len_fds[0] = 0;
  struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
  while (cmsg && len_fds[0] < max_fds) {
    if (cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS) {
      int num_fds = (cmsg->cmsg_len - CMSG_LEN(0)) / sizeof(int);
      int *cmsg_fds = (int *)CMSG_DATA(cmsg);
      for (int i = 0; i < num_fds && len_fds[0] < max_fds; i++) {
        fds[len_fds[0]] = cmsg_fds[i];
        len_fds[0]++;
      }
    }
    cmsg = CMSG_NXTHDR(&msg, cmsg);
  }
  return JNI_TRUE;
}

jboolean usWrite(int fd, int* len_data, char* data, int* len_fds, int* fds) {
  int max_fds = len_fds[0];

  struct iovec iov;
  iov.iov_base = (void *)data;
  iov.iov_len = len_data[0];

  char cmsg_buf[CMSG_SPACE(sizeof(int) * max_fds)];
  struct msghdr msg = {};
  msg.msg_iov = &iov;
  msg.msg_iovlen = 1;

  if (len_fds[0] > 0) {
    msg.msg_control = cmsg_buf;
    msg.msg_controllen = CMSG_SPACE(sizeof(int) * len_fds[0]);

    struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = CMSG_LEN(sizeof(int) * len_fds[0]);

    int *cmsg_fds = (int *)CMSG_DATA(cmsg);
    for (int i = 0; i < len_fds[0]; i++) {
      cmsg_fds[i] = fds[i];
    }
  }

  int n = sendmsg(fd, &msg, 0);
  if (n < 0) {
    return JNI_FALSE;
  }
  len_data[0] = n;
  return JNI_TRUE;
}

jboolean usClose(int fd) {
  close(fd);
  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT int (*_usOpen)() = &usOpen;
  JNIEXPORT jboolean (*_usBind)(int, const char*) = &usBind;
  JNIEXPORT jboolean (*_usListen)(int) = &usListen;
  JNIEXPORT int (*_usAccept)(int) = &usAccept;
  JNIEXPORT jboolean (*_usConnect)(int, const char*) = &usConnect;
  JNIEXPORT jboolean (*_usRead)(int, int*, char*, int*, int*) = &usRead;
  JNIEXPORT jboolean (*_usWrite)(int, int*, char*, int*, int*) = &usWrite;
  JNIEXPORT jboolean (*_usClose)(int) = &usClose;


  JNIEXPORT jboolean JNICALL UnixSocketAPIinit() {return JNI_TRUE;}
}

