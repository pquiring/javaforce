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

jlong pamOpen(const char* user, const char* pass, const char* backend) {
  pam_user = user;
  pam_pass = pass;
  pam_handle_t *handle;
  pam_conv conv;
  conv.conv = &pam_callback;
  conv.appdata_ptr = NULL;
  if (_pam_start == NULL) {
    printf("pamOpen:libpam not loaded!\n");
    return 0;
  }
  int res = (*_pam_start)(backend, pam_user, &conv, &handle);
  if (res != 0) {
    printf("pam_start() failed:%d:%d\n", res, errno);
    return 0;
  }

  if (_pam_authenticate == NULL) {
    printf("pamAuthenticate:libpam not loaded!\n");
    return JNI_FALSE;
  }
  res = (*_pam_authenticate)(handle, PAM_SILENT);
  printf("pam_authenticate():%d:%d\n", res, errno);
  if (pam_responses != NULL) {
//      free(pam_responses);  //crashes if password was wrong - memory leak for now???
    pam_responses = NULL;
  }

  if (res != 0) {
    (*_pam_end)(handle, 0);
    handle = NULL;
  }

  pam_user = NULL;
  pam_pass = NULL;

  return (jlong)handle;
}

jboolean pamClose(jlong ctx) {
  if (ctx == 0) return JNI_FALSE;
  pam_handle_t *handle = (pam_handle_t*)ctx;
  (*_pam_end)(handle, 0);
  return JNI_TRUE;
}

jboolean pamOpenSession(jlong ctx) {
  if (ctx == 0) return JNI_FALSE;
  pam_handle_t *handle = (pam_handle_t*)ctx;
  (*_pam_open_session)(handle, 0);
  return JNI_TRUE;
}

jboolean pamCloseSession(jlong ctx) {
  if (ctx == 0) return JNI_FALSE;
  pam_handle_t *handle = (pam_handle_t*)ctx;
  (*_pam_close_session)(handle, 0);
  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jlong (*_pamOpen)(const char* user, const char* pass, const char* backend) = &pamOpen;
  JNIEXPORT jboolean (*_pamClose)(jlong) = &pamClose;
  JNIEXPORT jboolean (*_pamOpenSession)(jlong) = &pamOpenSession;
  JNIEXPORT jboolean (*_pamCloseSession)(jlong) = &pamCloseSession;
}
