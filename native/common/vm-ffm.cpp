//VMHost

jlong vmTotalMemory()
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

jlong vmFreeMemory()
{
  void* conn = connect();
  if (conn == NULL) return 0;

  jlong value = (*_virNodeGetFreeMemory)(conn);

  disconnect(conn);

  return value;
}

jlong vmCpuLoad()
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

jboolean vmConnect(const char* remote)
{
  void* conn = connect_remote(remote);

  if (conn == NULL) {
    return JNI_FALSE;
  }

  disconnect(conn);

  return JNI_TRUE;
}

//VirtualMachine

jboolean vmRegister(const char* xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainDefineXML)(conn, xml);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  (*_virDomainFree)(dom);
  disconnect(conn);

  return JNI_TRUE;
}

jboolean vmUnregister(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jboolean vmStart(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jboolean vmStop(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jboolean vmPowerOff(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jboolean vmRestart(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jboolean vmSuspend(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jboolean vmResume(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

jint vmGetState(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

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

JFArray* vmList()
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

  JFArray* array = JFArray::create(count,sizeof(const char*),ARRAY_TYPE_STRING);
  for(int idx=0;idx<count;idx++) {
    char* desc = (char*)(*_virDomainGetMetadata)(doms[idx], VIR_DOMAIN_METADATA_DESCRIPTION, NULL, VIR_DOMAIN_AFFECT_CURRENT);
#ifdef VM_DEBUG
    printf("VM:%s\n", desc);
#endif
    array->setString(idx, desc);  //desc must be free()ed later
    (*_virDomainFree)(doms[idx]);
  }

  free(doms);

  disconnect(conn);

  return array;
}

const char* vmGet(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return NULL;
  }

  const char* desc = (*_virDomainGetMetadata)(dom, VIR_DOMAIN_METADATA_DESCRIPTION, NULL, VIR_DOMAIN_AFFECT_CURRENT);

  disconnect(conn);

  return desc;  //must be free()ed
}

jboolean vmMigrate(const char* name, const char* desthost, jboolean live)
{
  char durl[1024];
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* dom = (*_virDomainLookupByName)(conn, name);

  if (dom == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  create_remote_uri(durl, desthost);

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

JFArray* vmStorageList()
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
  JFArray* array = JFArray::create(count, sizeof(const char*), ARRAY_TYPE_STRING);
  for(int idx=0;idx<count;idx++) {
    uuid[0] = 0;
    (*_virStoragePoolGetUUIDString)(pools[idx], uuid);
#ifdef VM_DEBUG
    printf("Storage:%s\n", uuid);
#endif
    char *str = (char*)malloc(strlen(uuid) + 1);
    strcpy(str, uuid);
    array->setString(idx, str);
    (*_virStoragePoolFree)(pools[idx]);
  }

  free(pools);

  disconnect(conn);

  return array;
}

jboolean vmStorageRegister(const char* xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* pool = (*_virStoragePoolDefineXML)(conn, xml, 0);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

  return JNI_TRUE;
}

jboolean vmStorageUnregister(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* pool = (*_virStoragePoolLookupByName)(conn, name);

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

jboolean vmStorageStart(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* pool = (*_virStoragePoolLookupByName)(conn, name);

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

jboolean vmStorageStop(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* pool = (*_virStoragePoolLookupByName)(conn, name);

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

jint vmStorageGetState(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* pool = (*_virStoragePoolLookupByName)(conn, name);

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

const char* vmStorageGetUUID(const char* name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void* pool = (*_virStoragePoolLookupByName)(conn, name);

  if (pool == NULL) {
    disconnect(conn);
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  uuid[0] = 0;
  (*_virStoragePoolGetUUIDString)(pool, uuid);

  (*_virStoragePoolFree)(pool);

  disconnect(conn);

  char *str = (char*)malloc(strlen(uuid) + 1);
  strcpy(str, uuid);

  return str;
}

//Disk

jboolean vmDiskCreate(const char* name, const char* xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* pool = (*_virStoragePoolLookupByName)(conn, name);

  if (pool == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  void* vol = (*_virStorageVolCreateXML)(pool, xml, 0);

  if (vol != NULL) {
    (*_virStorageVolFree)(vol);
  }

  disconnect(conn);

  return vol != NULL;
}

//Network

JFArray* vmNetworkListPhys()
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

  JFArray* array = JFArray::create(count, sizeof(char*), ARRAY_TYPE_STRING);
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virInterfaceGetName)(ifaces[idx]);
#ifdef VM_DEBUG
    printf("net_iface:%s\n", name);
#endif
    char *str = (char*)malloc(strlen(name) + 1);
    strcpy(str, name);
    array->setString(idx, str);
    (*_virInterfaceFree)(ifaces[idx]);
  }

  free(ifaces);

  disconnect(conn);

  return array;
}

//Device

JFArray* vmDeviceList(jint type)
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

  JFArray* array = JFArray::create(count, sizeof(char*), ARRAY_TYPE_STRING);
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virNodeDeviceGetName)(devs[idx]);
    char* xml = (*_virNodeDeviceGetXMLDesc)(devs[idx], 0);
#ifdef VM_DEBUG
    printf("Device:%s=%s\n", name, xml);
#endif
    sprintf(devstr, "%s=%s", name, xml);
    char* str = (char*)malloc(strlen(devstr) + 1);
    strcpy(str, devstr);
    array->setString(idx, str);
    (*_virNodeDeviceFree)(devs[idx]);
  }

  free(devs);

  disconnect(conn);

  return array;
}

jboolean vmSecretCreate(const char* xml, const char* passwd)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  void* secret = (*_virSecretDefineXML)(conn, xml, 0);

  if (secret == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virSecretSetValue)(secret, passwd, strlen(passwd), 0);

  (*_virSecretFree)(secret);

  disconnect(conn);

  return res == 0;
}

extern "C" {

  JNIEXPORT jlong (*_vmTotalMemory)() = &vmTotalMemory;
  JNIEXPORT jlong (*_vmFreeMemory)() = &vmFreeMemory;
  JNIEXPORT jlong (*_vmCpuLoad)() = &vmCpuLoad;
  JNIEXPORT jboolean (*_vmConnect)(const char*) = &vmConnect;
  JNIEXPORT jboolean (*_vmGetAllStats)(jint,jint,jint,jint,jint) = &vmGetAllStats;

  JNIEXPORT JFArray* (*_vmDeviceList)(jint) = &vmDeviceList;

  JNIEXPORT jboolean (*_vmDiskCreate)(const char*,const char*) = &vmDiskCreate;

  JNIEXPORT JFArray* (*_vmNetworkListPhys)() = &vmNetworkListPhys;

  JNIEXPORT JFArray* (*_vmStorageList)() = &vmStorageList;
  JNIEXPORT jboolean (*_vmStorageRegister)(const char*) = &vmStorageRegister;
  JNIEXPORT jboolean (*_vmStorageUnregister)(const char*) = &vmStorageUnregister;
  JNIEXPORT jboolean (*_vmStorageStart)(const char*) = &vmStorageStart;
  JNIEXPORT jboolean (*_vmStorageStop)(const char*) = &vmStorageStop;
  JNIEXPORT jint (*_vmStorageGetState)(const char*) = &vmStorageGetState;
  JNIEXPORT const char* (*_vmStorageGetUUID)(const char*) = &vmStorageGetUUID;

  JNIEXPORT jboolean (*_vmInit)() = &vmInit;
  JNIEXPORT JFArray* (*_vmList)() = &vmList;
  JNIEXPORT jboolean (*_vmRegister)(const char*) = &vmRegister;
  JNIEXPORT jboolean (*_vmUnregister)(const char*) = &vmUnregister;
  JNIEXPORT jboolean (*_vmStart)(const char*) = &vmStart;
  JNIEXPORT jboolean (*_vmStop)(const char*) = &vmStop;
  JNIEXPORT jint (*_vmGetState)(const char*) = &vmGetState;
  JNIEXPORT jboolean (*_vmPowerOff)(const char*) = &vmPowerOff;
  JNIEXPORT jboolean (*_vmRestart)(const char*) = &vmRestart;
  JNIEXPORT jboolean (*_vmSuspend)(const char*) = &vmSuspend;
  JNIEXPORT jboolean (*_vmResume)(const char*) = &vmResume;
  JNIEXPORT const char* (*_vmGet)(const char*) = &vmGet;
  JNIEXPORT jboolean (*_vmMigrate)(const char*,const char*,jboolean) = &vmMigrate;
  JNIEXPORT jboolean (*_vmSnapshotCreate)(const char*,const char*,jint) = &vmSnapshotCreate;
  JNIEXPORT JFArray* (*_vmSnapshotList)(const char*) = &vmSnapshotList;
  JNIEXPORT jboolean (*_vmSnapshotExists)(const char*) = &vmSnapshotExists;
  JNIEXPORT const char* (*_vmSnapshotGetCurrent)(const char*) = &vmSnapshotGetCurrent;
  JNIEXPORT jboolean (*_vmSnapshotRestore)(const char*,const char*) = &vmSnapshotRestore;
  JNIEXPORT jboolean (*_vmSnapshotDelete)(const char*,const char*) = &vmSnapshotDelete;

  JNIEXPORT jboolean (*_vmSecretCreate)(const char*,const char*) = &vmSecretCreate;

  JNIEXPORT jboolean VMAPIinit() {return JNI_TRUE;}

}
