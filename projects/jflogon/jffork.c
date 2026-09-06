#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <security/pam_appl.h>

static int debug = 1;

static const char *pam_user, *pam_pass;
static struct pam_response* pam_responses;

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

int main(int argc, char**argv) {
  char *newargv[] = { NULL, NULL, NULL };
  char *newenviron[] = { NULL };
  pam_handle_t *pam_handle;
  struct pam_conv conv;
  FILE *log;

  char user[256];
  char pwd[256];
  char backend[256];
  char uidstr[256];
  char gidstr[256];
  char app[256];

  if (debug) {
    log = fopen("/tmp/jffork.log", "w");
  }

  if (debug) fputs("reading user\n", log);
  fgets(user, 256, stdin);
  if (debug) fputs("reading pass\n", log);
  fgets(pwd, 256, stdin);
  if (debug) fputs("reading backend\n", log);
  fgets(backend, 256, stdin);

  if (debug) fputs("pam_start\n", log);
  int res = pam_start(backend, user, &conv, &pam_handle);
  if (res != PAM_SUCCESS) {
    printf("ERROR:pam_start() failed\n");
    return 1;
  }

  if (debug) fputs("pam_authenticate\n", log);
  res = pam_authenticate(pam_handle, 0);
  if (res != PAM_SUCCESS) {
    pam_end(pam_handle, 0);
    if (debug) fputs("pam_authenticate:failed\n", log);
    printf("ERROR:Authentication failed\n");
    return 2;
  }

  //signal jflogon to shutdown wayland compositor
  if (debug) fputs("pam_authenticate:success\n", log);
  printf("OKAY:Authentication accepted\n");

  if (debug) fputs("reading uid\n", log);
  fgets(uidstr, 256, stdin);
  if (debug) fputs("reading gid\n", log);
  fgets(gidstr, 256, stdin);
  if (debug) fputs("reading app\n", log);
  fgets(app, 256, stdin);

  if (debug) fputs("pam_setcred\n", log);
  res = pam_setcred(pam_handle, PAM_ESTABLISH_CRED);
  if (res != PAM_SUCCESS) {
    pam_end(pam_handle, 0);
    printf("ERROR:pam_setcred() failed\n");
    return 1;
  }

  if (debug) fputs("pam_open_session\n", log);
  res = pam_open_session(pam_handle, 0);
  if (res != PAM_SUCCESS) {
    pam_end(pam_handle, 0);
    printf("ERROR:pam_open_session() failed\n");
    return 1;
  }

  if (debug) fputs("fork\n", log);
  int uid = atoi(uidstr);
  int gid = atoi(gidstr);
  int pid = fork();
  if (pid == 0) {
    setsid();
    setgid(gid);
    setuid(uid);
    newargv[0] = app;
    execv(app, newargv);
    printf("ERROR:execv() failed");
    return 0;
  }
  int status;
  waitpid(pid, &status, 0);

  if (debug) fputs("pam_close_session\n", log);
  pam_close_session(pam_handle, 0);

  if (debug) fputs("pam_setcred\n", log);
  pam_setcred(pam_handle, PAM_DELETE_CRED);

  if (debug) fputs("pam_end\n", log);
  pam_end(pam_handle, 0);

  return 0;
}
