MonitorContext* monitorFolderCreate(const char* path)
{
  MonitorContext* ctx = (MonitorContext*)malloc(sizeof(MonitorContext));
  memset(ctx, 0, sizeof(MonitorContext));
  ctx->handle = CreateFile(path, FILE_LIST_DIRECTORY, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);
  return ctx;
}

void monitorFolderPoll(MonitorContext* ctx, void* listener)
{
  if (ctx == NULL) return;

  alignas(DWORD) char buffer[1024];
  int size;
  char path8[1024];

  void (*folderChangeEvent)(const char*, const char*) = (void (*)(const char*, const char*))listener;

  while (1) {
    if (ctx->close) {
      ctx->closed = JNI_TRUE;
      return;
    }
    ctx->polling = JNI_TRUE;
    size = 0;
    ReadDirectoryChangesW(ctx->handle, &buffer, sizeof(buffer), FALSE, 0x0f, (LPDWORD)&size, NULL, NULL);
    ctx->polling = JNI_FALSE;

    int pos = 0;
    while (size > sizeof(FILE_NOTIFY_STRUCT)) {
      FILE_NOTIFY_STRUCT *info = (FILE_NOTIFY_STRUCT*)(buffer + pos);

      char* event = NULL;
      jchar* path16 = NULL;

      switch (info->Action) {
        case FILE_ACTION_ADDED: event = "CREATED"; break;
        case FILE_ACTION_REMOVED: event = "DELETED"; break;
        case FILE_ACTION_MODIFIED: event = "MODIFIED"; break;
        case FILE_ACTION_RENAMED_OLD_NAME: event = "RENAMED"; break;
        case FILE_ACTION_RENAMED_NEW_NAME: event = "RENAMED"; break;
        default: event = "UNKNOWN"; break;
      }

      int strlen = info->FileNameLength;  //in bytes

      pos += sizeof(FILE_NOTIFY_STRUCT);
      size -= sizeof(FILE_NOTIFY_STRUCT);

      if (strlen == 0) {
        continue;
      }
      if (strlen > size) {
        printf("MonitorFolder:buffer too small:size=%d expected=%d\n", size, strlen);
        break;
      }
      path16 = (jchar*)(buffer + pos);

      strcpy8_16_len(path8, path16, strlen / 2);

      (*folderChangeEvent)(event, path8);

      pos += strlen;
      size -= strlen;
    }
  }
}

void monitorFolderClose(MonitorContext* ctx)
{
  if (ctx == NULL) return;
  ctx->close = JNI_TRUE;
  while (!ctx->closed) {
    if (ctx->polling) {
      CancelIoEx(ctx->handle, NULL);
    }
    Sleep(100);
  }
  CloseHandle(ctx->handle);
  free(ctx);
}

extern "C" {
  JNIEXPORT MonitorContext* (*_monitorFolderCreate)(const char*) = &monitorFolderCreate;
  JNIEXPORT void (*_monitorFolderPoll)(MonitorContext* ctx, void*) = &monitorFolderPoll;
  JNIEXPORT void (*_monitorFolderClose)(MonitorContext* ctx) = &monitorFolderClose;

  JNIEXPORT jboolean JNICALL MonitorFolderAPIinit() {return JNI_TRUE;}
}
