jlong monitorFolderCreate(const char* path)
{
  HANDLE handle = CreateFile(path, FILE_LIST_DIRECTORY, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);
  return (jlong)handle;
}

void monitorFolderPoll(jlong handle_ptr, void* listener)
{
  HANDLE handle = (HANDLE)handle_ptr;
  if (handle == NULL) return;

  alignas(DWORD) char buffer[1024];
  int size;
  char path8[1024];

  void (*folderChangeEvent)(const char*, const char*) = (void (*)(const char*, const char*))listener;

  while (1) {
    ReadDirectoryChangesW(handle, &buffer, sizeof(buffer), FALSE, 0x0f, (LPDWORD)&size, NULL, NULL);

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
      }

      int strlen = info->FileNameLength;

      pos += sizeof(FILE_NOTIFY_STRUCT);
      size -= sizeof(FILE_NOTIFY_STRUCT);

      if (strlen == 0 || strlen*2 > size) continue;
      path16 = (jchar*)(buffer + pos);

      strcpy8_16_len(path8, path16, strlen);

      if (event != NULL) {
        (*folderChangeEvent)(event, path8);
      }

      pos += strlen;
      size -= strlen;
    }
  }
}

void monitorFolderClose(jlong handle_ptr)
{
  HANDLE handle = (HANDLE)handle_ptr;
  if (handle == NULL) return;
  CloseHandle(handle);
}

extern "C" {
  JNIEXPORT jlong (*_monitorFolderOpen)(const char*) = &monitorFolderCreate;
  JNIEXPORT void (*_monitorFolderPool)(jlong, void*) = &monitorFolderPoll;
  JNIEXPORT void (*_monitorFolderClose)(jlong) = &monitorFolderClose;

  JNIEXPORT jboolean JNICALL MonitorFolderAPIinit() {return JNI_TRUE;}
}
