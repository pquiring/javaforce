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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_LnxNative_authUser
  (JNIEnv *e, jclass c, jstring user, jstring pass, jstring backend)
{
  const char *cbackend = e->GetStringUTFChars(backend,NULL);
  pam_user = e->GetStringUTFChars(user,NULL);
  pam_pass = e->GetStringUTFChars(pass,NULL);
  pam_handle_t *handle;
  pam_conv conv;
  conv.conv = &pam_callback;
  conv.appdata_ptr = NULL;

  int res = (*_pam_start)(cbackend, pam_user, &conv, &handle);
  if (res != 0) {
    e->ReleaseStringUTFChars(backend, cbackend);
    e->ReleaseStringUTFChars(user, pam_user);
    e->ReleaseStringUTFChars(pass, pam_pass);

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

  e->ReleaseStringUTFChars(backend, cbackend);
  e->ReleaseStringUTFChars(user, pam_user);
  e->ReleaseStringUTFChars(pass, pam_pass);

  pam_user = NULL;
  pam_pass = NULL;

  return res == 0;
}
