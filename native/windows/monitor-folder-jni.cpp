JNIEXPORT jlong JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderCreate
  (JNIEnv *e, jclass c, jstring path)
{
  const char *cpath = e->GetStringUTFChars(path,NULL);
  HANDLE handle = CreateFile(cpath, FILE_LIST_DIRECTORY, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);
  e->ReleaseStringUTFChars(path, cpath);
  return (jlong)handle;
}

struct FILE_NOTIFY_STRUCT {
  DWORD NextEntryOffset;
  DWORD Action;
  DWORD FileNameLength;
  WCHAR FileName[0];
};

JNIEXPORT void JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderPoll
  (JNIEnv *e, jclass c, jlong handle_ptr, jobject listener)
{
  HANDLE handle = (HANDLE)handle_ptr;
  if (handle == NULL) return;

  jclass cls;
  jmethodID mid;

  cls = e->GetObjectClass(listener);
  mid = e->GetMethodID(cls, "folderEvent", "(Ljava/lang/String;Ljava/lang/String;)V");

  alignas(DWORD) char buffer[1024];
  int size;

  while (1) {
    ReadDirectoryChangesW(handle, &buffer, sizeof(buffer), FALSE, 0x0f, (LPDWORD)&size, NULL, NULL);

    int pos = 0;
    while (size > sizeof(FILE_NOTIFY_STRUCT)) {
      FILE_NOTIFY_STRUCT *info = (FILE_NOTIFY_STRUCT*)(buffer + pos);

      const char* event = NULL;
      const jchar* path = NULL;

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
      path = (jchar*)(buffer + pos);

      if (event != NULL) {
        jstring jevent = e->NewStringUTF(event);
        jstring jpath = e->NewString(path, strlen);

        e->CallVoidMethod(listener, mid, jevent, jpath);
      }

      pos += strlen;
      size -= strlen;
    }
  }
}

JNIEXPORT void JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderClose
  (JNIEnv *e, jclass c, jlong handle_ptr)
{
  HANDLE handle = (HANDLE)handle_ptr;
  if (handle == NULL) return;
  FindCloseChangeNotification(handle);
}
