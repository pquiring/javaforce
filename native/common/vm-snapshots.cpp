JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nsnapshotCreate
  (JNIEnv *e, jclass o, jstring name, jstring xml, jint flags)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void *ssptr = (*_virDomainSnapshotCreateXML)(dom, cxml, flags);

  if (ssptr != NULL) {
    (*_virDomainSnapshotFree)(ssptr);
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  e->ReleaseStringUTFChars(xml, cxml);
  return ssptr != NULL;
}

const char* snapshot_get_desc(void* ss) {
  const char* xml = (*_virDomainSnapshotGetXMLDesc)(ss, 0);

  if (xml == NULL) return "";

  const char *p1 = strstr(xml, "<description>");  

  const char *p2 = strstr(xml, "</description>");  

  if (p1 == NULL || p2 == NULL) {
    p1 = xml; 
    p2 = xml;
  } else {
    p1 += 13;
  }

  int len = p2 - p1;
  char * name = (char*)malloc(len + 1);
  memcpy(name, p1, len);
  name[len] = 0;  

  free((void*)xml);

  return name;
}

const char* snapshot_get_parent(void* ss) {
  void* parent = (*_virDomainSnapshotGetParent)(ss, 0);
  if (parent == NULL) return NULL;
  const char* name = (*_virDomainSnapshotGetName)(ss);
  (*_virDomainSnapshotFree)(parent);
  return name;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_VirtualMachine_nsnapshotList
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  jobjectArray list = NULL;

  void** ptrs;

  int cnt = (*_virDomainListAllSnapshots)(dom, &ptrs, 0);

  if (cnt > 0) {
    list = e->NewObjectArray(cnt,e->FindClass("java/lang/String"),e->NewStringUTF(""));
    for(int i=0;i<cnt;i++) {
      void* ss = ptrs[i];
      const char* name = (*_virDomainSnapshotGetName)(ss);
      const char* desc = snapshot_get_desc(ss);
      const char* parent = snapshot_get_parent(ss);
      int len = strlen(name) + 1 + strlen(desc) + 1 + strlen(parent);
      char* item = (char*)malloc(len + 1);
      sprintf(item, "%s|%s|%s", name, desc, parent);
      e->SetObjectArrayElement(list, i, e->NewStringUTF(item));
      free((void*)parent);
      free(item);
      (*_virDomainSnapshotFree)(ss);
    }
    free(ptrs);
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  return list;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nsnapshotExists
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }
  
  jboolean cond = (*_virDomainHasCurrentSnapshot)(dom, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return cond;
}

JNIEXPORT jstring JNICALL Java_javaforce_vm_VirtualMachine_nsnapshotGetCurrent
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }
  
  void* ss = (*_virDomainSnapshotCurrent)(dom, 0);

  const char* curname = (*_virDomainSnapshotGetName)(ss);

  (*_virDomainSnapshotFree)(ss);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return e->NewStringUTF(curname);
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nsnapshotRestore
  (JNIEnv *e, jclass o, jstring name, jstring snapshot)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  const char* csnapshot = e->GetStringUTFChars(snapshot, NULL);
  void *ss = (*_virDomainSnapshotLookupByName)(dom, csnapshot, 0);
  e->ReleaseStringUTFChars(snapshot, csnapshot);

  int result = (*_virDomainRevertToSnapshot)(ss, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return result == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nsnapshotDelete
  (JNIEnv *e, jclass o, jstring name, jstring snapshot)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  const char* csnapshot = e->GetStringUTFChars(snapshot, NULL);
  void *ss = (*_virDomainSnapshotLookupByName)(dom, csnapshot, 0);
  e->ReleaseStringUTFChars(snapshot, csnapshot);

  int result = (*_virDomainSnapshotDelete)(ss, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return result == 0;
}
