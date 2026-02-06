struct MonitorContext {
  int fd;
  int wd;
  jboolean close;
  jboolean closed;
};

JNIEXPORT jlong JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderCreate
  (JNIEnv *e, jclass c, jstring path)
{
  MonitorContext* ctx = (MonitorContext*)malloc(sizeof(MonitorContext));
  memset(ctx, 0, sizeof(MonitorContext));
  ctx->fd = inotify_init1(IN_NONBLOCK);
  const char *cpath = e->GetStringUTFChars(path,NULL);
  ctx->wd = inotify_add_watch(ctx->fd, cpath, 0xfc0);
  e->ReleaseStringUTFChars(path, cpath);
  return (jlong)ctx;
}

struct _inotify_event {
  int      wd;       /* Watch descriptor */
  uint32_t mask;     /* Mask describing event */
  uint32_t cookie;   /* Unique cookie associating related events (for rename(2)) */
  uint32_t len;      /* Size of name field */
  char     name[0];  /* Optional null-terminated name */
};

JNIEXPORT void JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderPoll
  (JNIEnv *e, jclass c, jlong ctx_ptr, jobject listener)
{
  MonitorContext* ctx = (MonitorContext*)ctx_ptr;
  if (ctx == NULL) return;

  jclass cls;
  jmethodID mid;

  cls = e->GetObjectClass(listener);
  mid = e->GetMethodID(cls, "folderEvent", "(Ljava/lang/String;Ljava/lang/String;)V");

  char inotify_buffer[512];
  while (1) {
    if (ctx->close) {
      ctx->closed = true;
      return;
    }
    int size = read(ctx->fd, inotify_buffer, 512);
    if (size == -1) {
      sleep_ms(100);
      continue;
    }
    int pos = 0;
    while (size > sizeof(_inotify_event)) {
      _inotify_event *ievent = (_inotify_event*)(inotify_buffer + pos);

      const char* event = NULL;
      const char* path = NULL;

      switch (ievent->mask & IN_ALL_EVENTS) {
        case IN_CREATE: event = "CREATED"; break;
        case IN_DELETE: event = "DELETED"; break;
        case IN_MOVED_FROM: event = "MOVED_FROM"; break;
        case IN_MOVED_TO: event = "MOVED_TO"; break;
        case IN_DELETE_SELF: event = "DELETE_SELF"; break;
        case IN_MOVE_SELF: event = "MOVED_SELF"; break;
        default: event = "UNKNOWN"; break;
      }

      int strlen = ievent->len;

      pos += sizeof(_inotify_event);
      size -= sizeof(_inotify_event);

      if (strlen == 0 || strlen > size) continue;
      path = (char*)(inotify_buffer + pos);

      jstring jevent = e->NewStringUTF(event);
      jstring jpath = e->NewStringUTF(path);

      e->CallVoidMethod(listener, mid, jevent, jpath);

      pos += strlen;
      size -= strlen;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderClose
  (JNIEnv *e, jclass c, jlong ctx_ptr)
{
  MonitorContext* ctx = (MonitorContext*)ctx_ptr;
  if (ctx == NULL) return;
  ctx->close = JNI_TRUE;
  close(ctx->fd);
  ctx->fd = 0;
  while (!ctx->closed) {
    sleep_ms(100);
  }
  free(ctx);
}
