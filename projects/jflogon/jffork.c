#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

int main(int argc, char**argv) {
  static char *newargv[] = { NULL, NULL, NULL };
  static char *newenviron[] = { NULL };

  if (argc < 3) {
    printf("usage:jffork uid gid app\n");
    return 1;
  }
  int uid = atoi(argv[1]);
  int gid = atoi(argv[2]);
  char* app = argv[3];
  int pid = fork();
  if (pid == 0) {
    setsid();
    setuid(uid);
    setgid(gid);
    newargv[0] = "/usr/bin/dbus-run-session";
    newargv[1] = app;
    execv("/usr/bin/dbus-run-session", newargv);
  } else {
    int status;
    waitpid(pid, &status, 0);
  }
  return 0;
}
