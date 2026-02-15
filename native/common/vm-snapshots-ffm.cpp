//#define VM_SNAPSHOTS_DEBUG

jboolean vmSnapshotCreate(const char* name, const char* xml, jint flags)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  void *ssptr = (*_virDomainSnapshotCreateXML)(dom, xml, flags);

  if (ssptr != NULL) {
    (*_virDomainSnapshotFree)(ssptr);
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  return ssptr != NULL;
}

JFArray* vmSnapshotList(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  JFArray* list = NULL;

  void** ptrs;

  int cnt = (*_virDomainListAllSnapshots)(dom, &ptrs, 0);

#ifdef VM_SNAPSHOTS_DEBUG
  printf("snapshot count=%d\n", cnt);
#endif

  if (cnt > 0) {
    list = JFArray::create(cnt, sizeof(char*), ARRAY_TYPE_STRING);
    for(int i=0;i<cnt;i++) {
      void* ss = ptrs[i];
      const char* name = (*_virDomainSnapshotGetName)(ss);
      const char* desc = snapshot_get_desc(ss);
      const char* parent = snapshot_get_parent(ss);
      int len = strlen(name) + 1 + strlen(desc) + 1 + strlen(parent);
      char* item = (char*)malloc(len + 1);
      sprintf(item, "%s\t%s\t%s", name, desc, parent);
#ifdef VM_SNAPSHOTS_DEBUG
      printf("snapshot=[%s]\n", item);
#endif
      char* str = (char*)malloc(strlen(item) + 1);
      strcpy(str, item);
      list->setString(i, str);
      if (parent != ss_empty) free((void*)parent);
      if (desc != ss_empty) free((void*)desc);
      free(item);
      (*_virDomainSnapshotFree)(ss);
    }
    free(ptrs);
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  return list;
}

jboolean vmSnapshotExists(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  jboolean cond = (*_virDomainHasCurrentSnapshot)(dom, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return cond;
}

const char* vmSnapshotGetCurrent(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return NULL;
  }

  void* ss = (*_virDomainSnapshotCurrent)(dom, 0);

#ifdef VM_SNAPSHOTS_DEBUG
  printf("snapshot.current.ptr=[%p]\n", ss);
#endif

  char* ssname = NULL;

  if (ss != NULL) {
    const char* cssname = (*_virDomainSnapshotGetName)(ss);
#ifdef VM_SNAPSHOTS_DEBUG
    printf("snapshot.current.name=[%s]\n", cssname);
#endif
    ssname = (char*)malloc(strlen(cssname) + 1);
    strcpy(ssname, cssname);
    (*_virDomainSnapshotFree)(ss);
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  return ssname;
}

jboolean vmSnapshotRestore(const char* name, const char* snapshot)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  void *ss = (*_virDomainSnapshotLookupByName)(dom, snapshot, 0);

  int result = (*_virDomainRevertToSnapshot)(ss, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return result == 0;
}

jboolean vmSnapshotDelete(const char* name, const char* snapshot)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  void *ss = (*_virDomainSnapshotLookupByName)(dom, snapshot, 0);

  int result = (*_virDomainSnapshotDelete)(ss, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return result == 0;
}
