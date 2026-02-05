struct MonitorContext {
  HANDLE handle;
  jboolean polling;
  jboolean close;
  jboolean closed;
};

JNIEXPORT jlong JNICALL Java_javaforce_jni_MonitorFolderJNI_monitorFolderCreate
  (JNIEnv *e, jclass c, jstring path)
{
  MonitorContext* ctx = (MonitorContext*)malloc(sizeof(MonitorContext));
  memset(ctx, 0, sizeof(MonitorContext));
  const char *cpath = e->GetStringUTFChars(path,NULL);
  ctx->handle = CreateFile(cpath, FILE_LIST_DIRECTORY, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, NULL, OPEN_EXISTING, FILE_FLAG_BACKUP_SEMANTICS, NULL);
  e->ReleaseStringUTFChars(path, cpath);
  return (jlong)ctx;
}

struct FILE_NOTIFY_STRUCT {
  DWORD NextEntryOffset;
  DWORD Action;
  DWORD FileNameLength;
  WCHAR FileName[0];
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

  alignas(DWORD) char buffer[1024];
  int size;

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

      const char* event = NULL;
      const jchar* path = NULL;

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
      path = (jchar*)(buffer + pos);

      jstring jevent = e->NewStringUTF(event);
      jstring jpath = e->NewString(path, strlen / 2);

      e->CallVoidMethod(listener, mid, jevent, jpath);

      e->DeleteLocalRef(jevent);
      e->DeleteLocalRef(jpath);

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
  while (!ctx->closed) {
    if (ctx->polling) {
      CancelIoEx(ctx->handle, NULL);
    }
    Sleep(100);
  }
  CloseHandle(ctx->handle);
  free(ctx);
}
