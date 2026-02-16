//VMHost

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmInit() {
  return vmInit();
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_VMJNI_vmTotalMemory
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return 0;

  virNodeMemoryStats* params;
  int nparams = 0;

  (*_virNodeGetMemoryStats)(conn, VIR_NODE_MEMORY_STATS_ALL_CELLS, NULL, &nparams, 0);

  params = (virNodeMemoryStats*)malloc(sizeof(virNodeMemoryStats) * nparams);
  memset(params, 0, sizeof(virNodeMemoryStats) * nparams);

  (*_virNodeGetMemoryStats)(conn, VIR_NODE_MEMORY_STATS_ALL_CELLS, params, &nparams, 0);

  disconnect(conn);

#ifdef VM_DEBUG
  for(int a=0;a<nparams;a++) {
    printf("RAMStat:%s:%lld\n", params[a].field, params[a].value);
  }
/*
RAMStat:total:16201572
RAMStat:free:14622268
RAMStat:buffers:123404
RAMStat:cached:531576
*/
#endif

  for(int i=0;i<nparams;i++) {
    if (strcmp(params[i].field, "total") == 0) {
      return params[i].value * 1024LL;
    }
  }

  return 0;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_VMJNI_vmFreeMemory
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return 0;

  jlong value = (*_virNodeGetFreeMemory)(conn);

  disconnect(conn);

  return value;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_VMJNI_vmCpuLoad
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return 0;

  virNodeCPUStats* params;
  int nparams = 0;

  (*_virNodeGetCPUStats)(conn, VIR_NODE_CPU_STATS_ALL_CPUS, NULL, &nparams, 0);

  params = (virNodeCPUStats*)malloc(sizeof(virNodeCPUStats) * nparams);
  memset(params, 0, sizeof(virNodeCPUStats) * nparams);

  (*_virNodeGetCPUStats)(conn, VIR_NODE_CPU_STATS_ALL_CPUS, params, &nparams, 0);

  disconnect(conn);

#ifdef VM_DEBUG
  for(int a=0;a<nparams;a++) {
    printf("CPUStat:%s:%lld\n", params[a].field, params[a].value);
  }
/*
CPUStat:kernel:   130330000000
CPUStat:user:     191880000000
CPUStat:idle:  179262550000000
CPUStat:iowait:   231530000000
*/
#endif

  jlong total = 0;
  jlong inuse = 0;
  for(int i=0;i<nparams;i++) {
    total += params[i].value;
    if (strcmp(params[i].field, "idle") != 0) {
      inuse += params[i].value;
    }
  }

  return (inuse / total) * 100LL;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmConnect
  (JNIEnv *e, jclass o, jstring remote)
{
  const char* cremote = e->GetStringUTFChars(remote, NULL);

  void* conn = connect_remote(cremote);

  e->ReleaseStringUTFChars(remote, cremote);

  if (conn == NULL) {
    return JNI_FALSE;
  }

  disconnect(conn);

  return JNI_TRUE;
}

//VirtualMachine

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmRegister
  (JNIEnv *e, jobject o, jstring xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void* dom = (*_virDomainDefineXML)(conn, cxml);

  e->ReleaseStringUTFChars(xml, cxml);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmUnregister
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainUndefine)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmStart
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainCreate)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmStop
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainShutdown)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmPowerOff
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainDestroy)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmRestart
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainReset)(dom, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmSuspend
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainSuspend)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmResume
  (JNIEnv *e, jobject o, jstring name)
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

  int res = (*_virDomainResume)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_VMJNI_vmGetState
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return -1;
  }

  int state;
  int reason;
  int res = (*_virDomainGetState)(dom, &state, &reason, 0);

  (*_virDomainFree)(dom);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  if (res != 0) return -1;

  switch (state) {
    case VIR_DOMAIN_SHUTOFF: return 0;  //off
    case VIR_DOMAIN_SHUTDOWN:  //still on
    case VIR_DOMAIN_RUNNING: return 1;  //on
    case VIR_DOMAIN_PMSUSPENDED: return 2;  //suspend
  }
  return 3;  //error
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_vmList
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** doms;

  int count = (*_virConnectListAllDomains)(conn, &doms, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:VirtualMachine_list() failed : count=%d\n", count);
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* desc = (*_virDomainGetMetadata)(doms[idx], VIR_DOMAIN_METADATA_DESCRIPTION, NULL, VIR_DOMAIN_AFFECT_CURRENT);
#ifdef VM_DEBUG
    printf("VM:%s\n", desc);
#endif
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(desc));
    free((void*)desc);
    (*_virDomainFree)(doms[idx]);
  }

  free(doms);

  disconnect(conn);

  return array;
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_VMJNI_vmGet
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return NULL;
  }

  const char* desc = (*_virDomainGetMetadata)(dom, VIR_DOMAIN_METADATA_DESCRIPTION, NULL, VIR_DOMAIN_AFFECT_CURRENT);

  jstring ret = e->NewStringUTF(desc);

  free((void*)desc);

  disconnect(conn);

  return ret;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmMigrate
  (JNIEnv *e, jobject o, jstring name, jstring desthost, jboolean live)
{
  char durl[1024];
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* dom = (*_virDomainLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  const char* cdesthost = e->GetStringUTFChars(desthost, NULL);

  create_remote_uri(durl, cdesthost);

  e->ReleaseStringUTFChars(desthost, cdesthost);

  int flags = VIR_MIGRATE_PEER2PEER | VIR_MIGRATE_UNDEFINE_SOURCE | VIR_MIGRATE_PERSIST_DEST;
  if (live) {
    flags |= VIR_MIGRATE_LIVE | VIR_MIGRATE_TUNNELLED;
  } else {
    flags |= VIR_MIGRATE_OFFLINE;
  }

  int res = (*_virDomainMigrateToURI3)(dom, durl, NULL, 0, flags);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return res == 0;
}

//Storage

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_vmStorageList
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** pools;

  int count = (*_virConnectListAllStoragePools)(conn, &pools, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:Storage_list() failed : count=%d\n", count);
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    uuid[0] = 0;
    (*_virStoragePoolGetUUIDString)(pools[idx], uuid);
#ifdef VM_DEBUG
    printf("Storage:%s\n", uuid);
#endif
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(uuid));
    (*_virStoragePoolFree)(pools[idx]);
  }

  free(pools);

  disconnect(conn);

  return array;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmStorageRegister
  (JNIEnv *e, jobject o, jstring xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void* pool = (*_virStoragePoolDefineXML)(conn, cxml, 0);

  e->ReleaseStringUTFChars(xml, cxml);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmStorageUnregister
  (JNIEnv *e, jobject o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* pool = (*_virStoragePoolLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virStoragePoolUndefine)(pool);

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmStorageStart
  (JNIEnv *e, jobject o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* pool = (*_virStoragePoolLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virStoragePoolCreate)(pool, 0);

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmStorageStop
  (JNIEnv *e, jobject o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* pool = (*_virStoragePoolLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virStoragePoolDestroy)(pool);

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_VMJNI_vmStorageGetState
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* pool = (*_virStoragePoolLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  virStoragePoolInfo info;
  int res = (*_virStoragePoolGetInfo)(pool, &info);

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  if (res != 0) return -1;

  switch (info.state) {
    case VIR_STORAGE_POOL_INACTIVE: return 0;
    case VIR_STORAGE_POOL_RUNNING: return 1;
    case VIR_STORAGE_POOL_BUILDING: return 2;
  }
  return 3;
}

JNIEXPORT jstring JNICALL Java_javaforce_jni_VMJNI_vmStorageGetUUID
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* pool = (*_virStoragePoolLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (pool == NULL) {
    disconnect(conn);
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  uuid[0] = 0;
  (*_virStoragePoolGetUUIDString)(pool, uuid);

  (*_virStoragePoolFree)(pool);

  disconnect(conn);

  return e->NewStringUTF(uuid);
}

//Disk

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmDiskCreate
  (JNIEnv *e, jclass o, jstring name, jstring xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* pool = (*_virStoragePoolLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void* vol = (*_virStorageVolCreateXML)(pool, cxml, 0);

  e->ReleaseStringUTFChars(xml, cxml);

  if (vol != NULL) {
    (*_virStorageVolFree)(vol);
  }

  disconnect(conn);

  return vol != NULL;
}

//Network

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_vmNetworkListPhys
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** ifaces;

  int count = (*_virConnectListAllInterfaces)(conn, &ifaces, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:NetworkInterface_listPhys() failed : count=%d\n", count);
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virInterfaceGetName)(ifaces[idx]);
#ifdef VM_DEBUG
    printf("net_iface:%s\n", name);
#endif
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(name));
    (*_virInterfaceFree)(ifaces[idx]);
  }

  free(ifaces);

  disconnect(conn);

  return array;
}

//Device

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_vmDeviceList
  (JNIEnv *e, jclass o, jint type)
{
  char devstr[4*1024];
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** devs;

  int devtype = 0;
  switch (type) {
    case 1: devtype = VIR_CONNECT_LIST_NODE_DEVICES_CAP_USB_DEV; break;
    case 2: devtype = VIR_CONNECT_LIST_NODE_DEVICES_CAP_PCI_DEV; break;
  }

  int count = (*_virConnectListAllNodeDevices)(conn, &devs, devtype);

  if (count < 0) {
    disconnect(conn);
    printf("VM:Device_list() failed : count=%d\n", count);
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virNodeDeviceGetName)(devs[idx]);
    char* xml = (*_virNodeDeviceGetXMLDesc)(devs[idx], 0);
#ifdef VM_DEBUG
    printf("Device:%s=%s\n", name, xml);
#endif
    sprintf(devstr, "%s=%s", name, xml);
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(devstr));
    (*_virNodeDeviceFree)(devs[idx]);
  }

  free(devs);

  disconnect(conn);

  return array;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_vmSecretCreate
  (JNIEnv *e, jclass o, jstring xml, jstring passwd)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void* secret = (*_virSecretDefineXML)(conn, cxml, 0);

  e->ReleaseStringUTFChars(xml, cxml);

  if (secret == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  const char* cpasswd = e->GetStringUTFChars(passwd, NULL);

  int res = (*_virSecretSetValue)(secret, cpasswd, strlen(cpasswd), 0);

  e->ReleaseStringUTFChars(passwd, cpasswd);

  (*_virSecretFree)(secret);

  disconnect(conn);

  return res == 0;
}

static JNINativeMethod javaforce_vm_VMHost[] = {
  {"vmTotalMemory", "()J", (void *)&Java_javaforce_jni_VMJNI_vmTotalMemory},
  {"vmFreeMemory", "()J", (void *)&Java_javaforce_jni_VMJNI_vmFreeMemory},
  {"vmCpuLoad", "()J", (void *)&Java_javaforce_jni_VMJNI_vmCpuLoad},
  {"vmConnect", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmConnect},
  {"vmGetAllStats", "(IIIII)Z", (void *)&Java_javaforce_jni_VMJNI_vmGetAllStats},
};

static JNINativeMethod javaforce_vm_Device[] = {
  {"vmDeviceList", "(I)[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmDeviceList},
};

static JNINativeMethod javaforce_vm_Disk[] = {
  {"vmDiskCreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmDiskCreate},
};

static JNINativeMethod javaforce_vm_NetworkInterface[] = {
  {"vmNetworkListPhys", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmNetworkListPhys},
};

static JNINativeMethod javaforce_vm_Storage[] = {
  {"vmStorageList", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmStorageList},
  {"vmStorageRegister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmStorageRegister},
  {"vmStorageUnregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmStorageUnregister},
  {"vmStorageStart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmStorageStart},
  {"vmStorageStop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmStorageStop},
  {"vmStorageGetState", "(Ljava/lang/String;)I", (void *)&Java_javaforce_jni_VMJNI_vmStorageGetState},
  {"vmStorageGetUUID", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmStorageGetUUID},
};

static JNINativeMethod javaforce_vm_VirtualMachine[] = {
  {"vmInit", "()Z", (void *)&Java_javaforce_jni_VMJNI_vmInit},
  {"vmList", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmList},
  {"vmRegister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmRegister},
  {"vmUnregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmUnregister},
  {"vmStart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmStart},
  {"vmStop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmStop},
  {"vmGetState", "(Ljava/lang/String;)I", (void *)&Java_javaforce_jni_VMJNI_vmGetState},
  {"vmPowerOff", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmPowerOff},
  {"vmRestart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmRestart},
  {"vmSuspend", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmSuspend},
  {"vmResume", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmResume},
  {"vmGet", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmGet},
  {"vmMigrate", "(Ljava/lang/String;Ljava/lang/String;Z)Z", (void *)&Java_javaforce_jni_VMJNI_vmMigrate},
  {"vmSnapshotCreate", "(Ljava/lang/String;Ljava/lang/String;I)Z", (void *)&Java_javaforce_jni_VMJNI_vmSnapshotCreate},
  {"vmSnapshotList", "(Ljava/lang/String;)[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmSnapshotList},
  {"vmSnapshotExists", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmSnapshotExists},
  {"vmSnapshotGetCurrent", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_vmSnapshotGetCurrent},
  {"vmSnapshotRestore", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmSnapshotRestore},
  {"vmSnapshotDelete", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmSnapshotDelete},
};

static JNINativeMethod javaforce_vm_Secret[] = {
  {"vmSecretCreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_vmSecretCreate},
};

#include "register.h"

extern "C" void vm_register(JNIEnv *env);

void vm_register(JNIEnv *env) {
  //register java natives
  jclass cls;

  cls = findClass(env, "javaforce/jni/VMJNI");
  registerNatives(env, cls, javaforce_vm_VMHost, sizeof(javaforce_vm_VMHost)/sizeof(JNINativeMethod));

  registerNatives(env, cls, javaforce_vm_Device, sizeof(javaforce_vm_Device)/sizeof(JNINativeMethod));

  registerNatives(env, cls, javaforce_vm_Disk, sizeof(javaforce_vm_Disk)/sizeof(JNINativeMethod));

  registerNatives(env, cls, javaforce_vm_NetworkInterface, sizeof(javaforce_vm_NetworkInterface)/sizeof(JNINativeMethod));

  registerNatives(env, cls, javaforce_vm_Storage, sizeof(javaforce_vm_Storage)/sizeof(JNINativeMethod));

  registerNatives(env, cls, javaforce_vm_VirtualMachine, sizeof(javaforce_vm_VirtualMachine)/sizeof(JNINativeMethod));

  registerNatives(env, cls, javaforce_vm_Secret, sizeof(javaforce_vm_Secret)/sizeof(JNINativeMethod));
}
