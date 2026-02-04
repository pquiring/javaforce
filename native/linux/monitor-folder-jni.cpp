struct monitor_handle {
  int fd;
  int wd;
};

JNIEXPORT jlong JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderCreate
  (JNIEnv *e, jclass c, jstring path)
{
  monitor_handle* handle = (monitor_handle*)malloc(sizeof(monitor_handle));
  handle->fd = inotify_init();
  const char *cpath = e->GetStringUTFChars(path,NULL);
  handle->wd = inotify_add_watch(handle->fd, cpath, IN_ALL_EVENTS);
  e->ReleaseStringUTFChars(path, cpath);
  return (jlong)handle;
}

struct _inotify_event {
  int      wd;       /* Watch descriptor */
  uint32_t mask;     /* Mask describing event */
  uint32_t cookie;   /* Unique cookie associating related events (for rename(2)) */
  uint32_t len;      /* Size of name field */
  char     name[0];  /* Optional null-terminated name */
};

JNIEXPORT void JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderPoll
  (JNIEnv *e, jclass c, jlong handle_ptr, jobject listener)
{
  monitor_handle* handle = (monitor_handle*)handle_ptr;
  if (handle == NULL) return;

  jclass cls;
  jmethodID mid;

  cls = e->GetObjectClass(listener);
  mid = e->GetMethodID(cls, "folderEvent", "(Ljava/lang/String;Ljava/lang/String;)V");

  char inotify_buffer[512];
  while (1) {
    int size = read(handle->fd, inotify_buffer, 512);
    if (size == -1) return;
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
  (JNIEnv *e, jclass c, jlong handle_ptr)
{
  monitor_handle* handle = (monitor_handle*)handle_ptr;
  if (handle == NULL) return;
  close(handle->fd);
  free(handle);
}
