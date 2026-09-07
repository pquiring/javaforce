#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <security/pam_appl.h>

extern char **environ;

static int debug = 1;

static const char *pam_user, *pam_pass;
static struct pam_response* pam_responses;
static FILE *flog;

static int pam_callback(int num_msg, const struct pam_message** _pam_messages, struct pam_response** _pam_responses, void* _appdata_ptr)
{
  pam_responses = (struct pam_response*)calloc(num_msg, sizeof(struct pam_response));  //array of pam_response
  char* tmp;
  for(int a=0;a<num_msg;a++) {
    const struct pam_message *msg = _pam_messages[a];
    tmp = NULL;
    switch (msg->msg_style) {
      case PAM_PROMPT_ECHO_ON:
        tmp = strdup(pam_user);
        break;
      case PAM_PROMPT_ECHO_OFF:
        tmp = strdup(pam_pass);
        break;
    }
    pam_responses[a].resp = tmp;
    pam_responses[a].resp_retcode = 0;
  }
  *_pam_responses = pam_responses;
  return 0;
}

static void logmsg(const char* msg) {
  fputs(msg, flog);
  fflush(flog);
}

static void clean(char *str) {
  char* eol = strchr(str, '\n');
  if (eol == NULL) return;
  *eol = 0;
}

int main(int argc, char**argv) {
  char *newargv[] = { NULL, NULL, NULL };
  char *newenviron[] = { NULL };
  pam_handle_t *pam_handle;
  struct pam_conv conv;
  char user[256];
  char pwd[256];
  char backend[256];
  char uidstr[256];
  char gidstr[256];
  char app[256];
  char msg[256];
  const char* xid;

  conv.conv = &pam_callback;
  conv.appdata_ptr = NULL;

  if (debug) {
    flog = fopen("/var/log/jflogon-session.log", "w");
  }

  if (debug) logmsg("reading user\n");
  fgets(user, 256, stdin);
  clean(user);
  pam_user = user;
  if (debug) logmsg("reading pass\n");
  fgets(pwd, 256, stdin);
  clean(pwd);
  pam_pass = pwd;
  if (debug) logmsg("reading backend\n");
  fgets(backend, 256, stdin);
  clean(backend);

  if (debug) logmsg("pam_start\n");
  int res = pam_start(backend, user, &conv, &pam_handle);
  if (res != PAM_SUCCESS) {
    printf("ERROR:pam_start() failed\n");
    return 1;
  }

  if (debug) logmsg("pam_authenticate\n");
  res = pam_authenticate(pam_handle, 0);
  if (res != PAM_SUCCESS) {
    pam_end(pam_handle, 0);
    if (debug) logmsg("pam_authenticate:failed\n");
    printf("ERROR:Authentication failed\n");
    return 2;
  }

  //signal jflogon to shutdown wayland compositor
  if (debug) logmsg("pam_authenticate:success\n");
  printf("SUCCESS:Authentication accepted\n");
  fflush(stdout);

  if (debug) logmsg("reading uid\n");
  fgets(uidstr, 256, stdin);
  clean(uidstr);
  if (debug) logmsg("reading gid\n");
  fgets(gidstr, 256, stdin);
  clean(gidstr);
  if (debug) logmsg("reading app\n");
  fgets(app, 256, stdin);
  clean(app);

  pam_set_item(pam_handle, PAM_TTY, "tty1");

  if (debug) logmsg("pam_setcred\n");
  res = pam_setcred(pam_handle, PAM_ESTABLISH_CRED);
  if (res != PAM_SUCCESS) {
    pam_end(pam_handle, 0);
    printf("ERROR:pam_setcred() failed\n");
    return 1;
  }

  if (debug) logmsg("pam_open_session\n");
  res = pam_open_session(pam_handle, 0);
  if (res != PAM_SUCCESS) {
    pam_end(pam_handle, 0);
    printf("ERROR:pam_open_session() failed\n");
    return 1;
  }

  if (debug) logmsg("fork\n");
  int uid = atoi(uidstr);
  int gid = atoi(gidstr);
  int pid = fork();
  if (pid == 0) {
    setsid();
    if (debug) {
      xid = pam_getenv(pam_handle, "XDG_SESSION_ID");
      sprintf(msg, "XDG_SESSION_ID=%s\n", xid);
      logmsg(msg);
    }
//    environ = pam_getenvlist(pam_handle);
    setgid(gid);
    setuid(uid);
    newargv[0] = app;
    execv(app, newargv);
    printf("ERROR:execv() failed");
    return 1;
  }
  int status;
  waitpid(pid, &status, 0);

  if (debug) logmsg("pam_close_session\n");
  pam_close_session(pam_handle, 0);

  if (debug) logmsg("pam_setcred\n");
  pam_setcred(pam_handle, PAM_DELETE_CRED);

  if (debug) logmsg("pam_end\n");
  pam_end(pam_handle, 0);

  return 0;
}
