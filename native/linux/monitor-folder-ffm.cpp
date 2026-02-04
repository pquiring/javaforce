jlong monitorFolderCreate(const char* path)
{
  monitor_handle* handle = (monitor_handle*)malloc(sizeof(monitor_handle));
  handle->fd = inotify_init();
  handle->wd = inotify_add_watch(handle->fd, path, IN_ALL_EVENTS);
  return (jlong)handle;
}

void monitorFolderPoll(jlong handle_ptr, void* listener)
{
  monitor_handle* handle = (monitor_handle*)handle_ptr;
  if (handle == NULL) return;

  void (*folderChangeEvent)(const char*, const char*) = (void (*)(const char*, const char*))listener;

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

      (*folderChangeEvent)(event, path);

      pos += strlen;
      size -= strlen;
    }
  }
}

void monitorFolderClose(jlong handle_ptr)
{
  monitor_handle* handle = (monitor_handle*)handle_ptr;
  if (handle == NULL) return;
  close(handle->fd);
  free(handle);
}

extern "C" {
  JNIEXPORT jlong (*_monitorFolderOpen)(const char*) = &monitorFolderCreate;
  JNIEXPORT void (*_monitorFolderPool)(jlong, void*) = &monitorFolderPoll;
  JNIEXPORT void (*_monitorFolderClose)(jlong) = &monitorFolderClose;

  JNIEXPORT jboolean JNICALL MonitorFolderAPIinit() {return JNI_TRUE;}
}
