MonitorContext* monitorFolderCreate(const char* path)
{
  MonitorContext* ctx = (MonitorContext*)malloc(sizeof(MonitorContext));
  memset(ctx, 0, sizeof(MonitorContext));
  ctx->fd = inotify_init();
  ctx->wd = inotify_add_watch(ctx->fd, path, 0xfc0);
  return ctx;
}

void monitorFolderPoll(MonitorContext* ctx, void* listener)
{
  if (ctx == NULL) return;

  void (*folderChangeEvent)(const char*, const char*) = (void (*)(const char*, const char*))listener;

  char inotify_buffer[512];
  while (1) {
    if (ctx->close) {
      ctx->closed = true;
      return;
    }
    int size = read(ctx->fd, inotify_buffer, 512);
    if (size == -1) {
      ctx->closed = true;
      return;
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

      (*folderChangeEvent)(event, path);

      pos += strlen;
      size -= strlen;
    }
  }
}

void monitorFolderClose(MonitorContext* ctx)
{
  if (ctx == NULL) return;
  ctx->close = JNI_TRUE;
  close(ctx->fd);
  ctx->fd = 0;
  while (!ctx->closed) {
    sleep_ms(100);
  }
  free(ctx);
}

extern "C" {
  JNIEXPORT MonitorContext* (*_monitorFolderCreate)(const char*) = &monitorFolderCreate;
  JNIEXPORT void (*_monitorFolderPoll)(MonitorContext*, void*) = &monitorFolderPoll;
  JNIEXPORT void (*_monitorFolderClose)(MonitorContext*) = &monitorFolderClose;

  JNIEXPORT jboolean JNICALL MonitorFolderAPIinit() {return JNI_TRUE;}
}
