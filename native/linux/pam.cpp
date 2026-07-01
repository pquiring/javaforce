static const char *pam_user, *pam_pass;
static struct pam_response* pam_responses;

static int pam_callback(int num_msg, const struct pam_message** _pam_messages, struct pam_response** _pam_responses, void* _appdata_ptr)
{
  pam_responses = (struct pam_response*)calloc(num_msg, sizeof(pam_response));  //array of pam_response
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

jboolean authUser(const char* user, const char* pass, const char* backend)
{
  pam_user = user;
  pam_pass = pass;
  pam_handle_t *handle;
  pam_conv conv;
  conv.conv = &pam_callback;
  conv.appdata_ptr = NULL;

  int res = (*_pam_start)(backend, pam_user, &conv, &handle);
  if (res != 0) {
    printf("pam_start() failed:%d:%d\n", res, errno);
    return JNI_FALSE;
  }
  res = (*_pam_authenticate)(handle, PAM_SILENT);
  printf("pam_authenticate():%d:%d\n", res, errno);
  (*_pam_end)(handle, 0);
  if (pam_responses != NULL) {
//      free(pam_responses);  //crashes if password was wrong - memory leak for now???
    pam_responses = NULL;
  }

  pam_user = NULL;
  pam_pass = NULL;

  return res == 0;
}

extern "C" {
  JNIEXPORT jboolean (*_authUser)(const char*,const char*,const char*) = &authUser;
}
