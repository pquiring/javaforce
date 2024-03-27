//libvirt api

#include <libvirt/libvirt.h>

JF_LIB_HANDLE virt;

//#define VM_DEBUG

//common
void* (*_virConnectOpen)(const char* name);
int (*_virConnectClose)(void* conn);
unsigned long long (*_virNodeGetFreeMemory)(void* conn);
int (*_virNodeGetCPUStats)(void* conn, int cpu, void* params, int* nparams, int flags);
int (*_virNodeGetMemoryStats)(void* conn, int cell, void* params, int* nparams, int flags);

//domains
void* (*_virDomainDefineXML)(void* conn, const char* xml);
void (*_virDomainFree)(void* dom);
int (*_virDomainUndefine)(void* dom);
void* (*_virDomainLookupByName)(void* conn, const char* name);
int (*_virDomainCreate)(void* dom);
int (*_virDomainShutdown)(void* dom);
int (*_virDomainDestroy)(void* dom);
int (*_virDomainReset)(void* dom, int flags);
int (*_virDomainSuspend)(void* dom);
int (*_virDomainResume)(void* dom);
int (*_virDomainGetState)(void* dom, int* state, int* reason, int flags);
int (*_virConnectListAllDomains)(void* conn, void*** doms, int flags);
char* (*_virDomainGetMetadata)(void* dom, int type, const char* uri, int flags);
int (*_virDomainMigrateToURI3)(void* dom, const char* duri, void* params, int nparams, int flags);

//storage
void* (*_virStoragePoolDefineXML)(void* conn, const char* xml, int flags);
int (*_virStoragePoolFree)(void* pool);
int (*_virStoragePoolUndefine)(void* pool);
void* (*_virStoragePoolLookupByName)(void* conn, const char* name);
int (*_virStoragePoolCreate)(void* pool, int flags);
int (*_virStoragePoolDestroy)(void* pool);
int (*_virStoragePoolGetInfo)(void* pool, virStoragePoolInfo* info);
int (*_virConnectListAllStoragePools)(void* conn, void*** pools, int flags);
int (*_virStoragePoolGetUUIDString)(void* pool, char* buf);
void* (*_virStorageVolCreateXML)(void* pool, const char* xml, int flags);
int (*_virStorageVolFree)(void* vol);

//disk

//network (interface)
int (*_virConnectListAllInterfaces)(void* conn, void*** ifaces, int flags);
const char* (*_virInterfaceGetName)(void* iface);
int (*_virInterfaceFree)(void* iface);

//network (virtual)
int (*_virConnectListAllNetworks)(void* conn, void*** nets, int flags);
void* (*_virNetworkLookupByName)(void* conn, const char* name);
int (*_virNetworkGetUUIDString)(void* net, char* uuid);
const char* (*_virNetworkGetName)(void* net);
const char* (*_virNetworkGetBridgeName)(void* net);
int (*_virNetworkCreate)(void* net);
int (*_virNetworkDestroy)(void* net);
int (*_virNetworkFree)(void* net);
void* (*_virNetworkDefineXML)(void* conn, const char* xml);
int (*_virNetworkUndefine)(void* net);

//network (port)
int (*_virNetworkListAllPorts)(void* net, void*** ports, int flags);
int (*_virNetworkPortGetUUIDString)(void* net, char* uuid);
int (*_virNetworkPortFree)(void* net);

//device
int (*_virConnectListAllNodeDevices)(void* conn, void*** devs, int flags);
const char* (*_virNodeDeviceGetName)(void* dev);
int (*_virNodeDeviceFree)(void* dev);

void vm_init() {
  virt = loadLibrary("/usr/lib/x86_64-linux-gnu/libvirt.so");
  if (virt == NULL) {
    printf("VM:Error:Unable to open libvirt.so\n");
    return;
  }

  //common
  getFunction(virt, (void**)&_virConnectOpen, "virConnectOpen");
  getFunction(virt, (void**)&_virConnectClose, "virConnectClose");
  getFunction(virt, (void**)&_virNodeGetFreeMemory, "virNodeGetFreeMemory");
  getFunction(virt, (void**)&_virNodeGetCPUStats, "virNodeGetCPUStats");
  getFunction(virt, (void**)&_virNodeGetMemoryStats, "virNodeGetMemoryStats");

  //domains
  getFunction(virt, (void**)&_virDomainDefineXML, "virDomainDefineXML");
  getFunction(virt, (void**)&_virDomainFree, "virDomainFree");
  getFunction(virt, (void**)&_virDomainUndefine, "virDomainUndefine");
  getFunction(virt, (void**)&_virDomainLookupByName, "virDomainLookupByName");
  getFunction(virt, (void**)&_virDomainCreate, "virDomainCreate");
  getFunction(virt, (void**)&_virDomainShutdown, "virDomainShutdown");
  getFunction(virt, (void**)&_virDomainDestroy, "virDomainDestroy");
  getFunction(virt, (void**)&_virDomainReset, "virDomainReset");
  getFunction(virt, (void**)&_virDomainSuspend, "virDomainSuspend");
  getFunction(virt, (void**)&_virDomainResume, "virDomainResume");
  getFunction(virt, (void**)&_virDomainGetState, "virDomainGetState");
  getFunction(virt, (void**)&_virConnectListAllDomains, "virConnectListAllDomains");
  getFunction(virt, (void**)&_virDomainGetMetadata, "virDomainGetMetadata");
  getFunction(virt, (void**)&_virDomainMigrateToURI3, "virDomainMigrateToURI3");

  //storage
  getFunction(virt, (void**)&_virStoragePoolDefineXML, "virStoragePoolDefineXML");
  getFunction(virt, (void**)&_virStoragePoolFree, "virStoragePoolFree");
  getFunction(virt, (void**)&_virStoragePoolUndefine, "virStoragePoolUndefine");
  getFunction(virt, (void**)&_virStoragePoolLookupByName, "virStoragePoolLookupByName");
  getFunction(virt, (void**)&_virStoragePoolCreate, "virStoragePoolCreate");
  getFunction(virt, (void**)&_virStoragePoolDestroy, "virStoragePoolDestroy");
  getFunction(virt, (void**)&_virStoragePoolGetInfo, "virStoragePoolGetInfo");
  getFunction(virt, (void**)&_virConnectListAllStoragePools, "virConnectListAllStoragePools");
  getFunction(virt, (void**)&_virStoragePoolGetUUIDString, "virStoragePoolGetUUIDString");
  getFunction(virt, (void**)&_virStorageVolCreateXML, "virStorageVolCreateXML");
  getFunction(virt, (void**)&_virStorageVolFree, "virStorageVolFree");

  //disk

  //network (interface)
  getFunction(virt, (void**)&_virConnectListAllInterfaces, "virConnectListAllInterfaces");
  getFunction(virt, (void**)&_virInterfaceGetName, "virInterfaceGetName");
  getFunction(virt, (void**)&_virInterfaceFree, "virInterfaceFree");

  //network (virtual)
  getFunction(virt, (void**)&_virConnectListAllNetworks, "virConnectListAllNetworks");
  getFunction(virt, (void**)&_virNetworkLookupByName, "virNetworkLookupByName");
  getFunction(virt, (void**)&_virNetworkGetUUIDString, "virNetworkGetUUIDString");
  getFunction(virt, (void**)&_virNetworkGetName, "virNetworkGetName");
  getFunction(virt, (void**)&_virNetworkGetBridgeName, "virNetworkGetBridgeName");
  getFunction(virt, (void**)&_virNetworkCreate, "virNetworkCreate");
  getFunction(virt, (void**)&_virNetworkDestroy, "virNetworkDestroy");
  getFunction(virt, (void**)&_virNetworkFree, "virNetworkFree");
  getFunction(virt, (void**)&_virNetworkDefineXML, "virNetworkDefineXML");
  getFunction(virt, (void**)&_virNetworkUndefine, "virNetworkUndefine");

  //network (port)
  getFunction(virt, (void**)&_virNetworkListAllPorts, "virNetworkListAllPorts");
  getFunction(virt, (void**)&_virNetworkPortGetUUIDString, "virNetworkPortGetUUIDString");
  getFunction(virt, (void**)&_virNetworkPortFree, "virNetworkPortFree");

  //device
  getFunction(virt, (void**)&_virConnectListAllNodeDevices, "virConnectListAllNodeDevices");
  getFunction(virt, (void**)&_virNodeDeviceGetName, "virNodeDeviceGetName");
  getFunction(virt, (void**)&_virNodeDeviceFree, "virNodeDeviceFree");
}

//common code

//see https://libvirt.org/uri.html

static void* connect(const char* host) {
  char url[1024];
  sprintf(url, "qemu://%s", host);
  void* conn = (*_virConnectOpen)(url);
  if (conn == NULL) {
    printf("VM:connect() failed\n");
  }
  return conn;
}

static void* connect() {
  return connect("/system");
}

static void create_remote_uri(char* out, const char *host) {
  sprintf(out, "qemu+ssh://root@%s/system?no_verify=1&keyfile=/root/cluster/%s", host, host);
}

static void* connect_remote(const char* host) {
  char url[1024];
  create_remote_uri(url, host);
  void* conn = (*_virConnectOpen)(url);
  if (conn == NULL) {
    printf("VM:connect_remote(%s) failed\n", host);
  }
  return conn;
}

static void disconnect(void* ptr) {
  (*_virConnectClose)(ptr);
}

//VMHost

JNIEXPORT jlong JNICALL Java_javaforce_vm_VMHost_total_1memory
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

JNIEXPORT jlong JNICALL Java_javaforce_vm_VMHost_free_1memory
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return 0;

  jlong value = (*_virNodeGetFreeMemory)(conn);

  disconnect(conn);

  return value;
}

JNIEXPORT jlong JNICALL Java_javaforce_vm_VMHost_cpu_1load
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VMHost_connect
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nregister
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nunregister
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nstart
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nstop
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_npoweroff
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nrestart
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nsuspend
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nrestore
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

JNIEXPORT jint JNICALL Java_javaforce_vm_VirtualMachine_ngetState
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_VirtualMachine_nlist
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** doms;

  int count = (*_virConnectListAllDomains)(conn, &doms, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:VirtualMachine_nlist() failed : count=%d\n", count);
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

JNIEXPORT jstring JNICALL Java_javaforce_vm_VirtualMachine_nget
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_nmigrate
  (JNIEnv *e, jobject o, jstring name, jstring desthost, jboolean live, jobject status)
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

  int flags = VIR_MIGRATE_PEER2PEER | VIR_MIGRATE_TUNNELLED | VIR_MIGRATE_UNDEFINE_SOURCE | VIR_MIGRATE_PERSIST_DEST;
  if (live) {
    flags |= VIR_MIGRATE_LIVE;
  } else {
    flags |= VIR_MIGRATE_OFFLINE;
  }

  int res = (*_virDomainMigrateToURI3)(dom, durl, NULL, 0, flags);

  (*_virDomainFree)(dom);
  disconnect(conn);

  return res == 0;
}

//Storage

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_Storage_nlist
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** pools;

  int count = (*_virConnectListAllStoragePools)(conn, &pools, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:Storage_nlist() failed : count=%d\n", count);
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_nregister
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_nunregister
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_nstart
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_nstop
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

JNIEXPORT jint JNICALL Java_javaforce_vm_Storage_ngetState
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_nformat
  (JNIEnv *e, jobject o, jstring path, jint type)
{
  //type : 1=EXT4
  //TODO:format storage pool
  return JNI_FALSE;
}

//Disk

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Disk_ncreate
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_NetworkInterface_nlistPhys
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** ifaces;

  int count = (*_virConnectListAllInterfaces)(conn, &ifaces, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:NetworkInterface_nlistPhys() failed : count=%d\n", count);
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_NetworkVirtual_nlistVirt
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** nets;

  int count = (*_virConnectListAllNetworks)(conn, &nets, 0);

  if (count < 0) {
    disconnect(conn);
    printf("VM:NetworkInterface_nlistVirt() failed : count=%d\n", count);
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virNetworkGetName)(nets[idx]);
    const char* bridge = (*_virNetworkGetBridgeName)(nets[idx]);
#ifdef VM_DEBUG
    printf("net_virt:%s:%s\n", name, bridge);
#endif
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(name));
    (*_virNetworkFree)(nets[idx]);
  }

  free(nets);

  disconnect(conn);

  return array;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_ncreatevirt
  (JNIEnv *e, jclass o, jstring xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void* net = (*_virNetworkDefineXML)(conn, cxml);

  e->ReleaseStringUTFChars(xml, cxml);

  if (net == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  (*_virNetworkFree)(net);
  disconnect(conn);

  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_ncreateport
  (JNIEnv *e, jclass o, jstring name, jstring xml)
{
  //TODO:create network virtual port
  return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_nstart
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* net = (*_virNetworkLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (net == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virNetworkCreate)(net);

  (*_virNetworkFree)(net);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_nstop
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* net = (*_virNetworkLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (net == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virNetworkDestroy)(net);

  (*_virNetworkFree)(net);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_nremove
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* net = (*_virNetworkLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (net == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  int res = (*_virNetworkUndefine)(net);

  (*_virNetworkFree)(net);
  disconnect(conn);

  if (res < 0) {
    printf("Error:%d\n", res);
  }

  return res == 0;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_NetworkVirtual_nlistPort
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  const char* cname = e->GetStringUTFChars(name, NULL);

#ifdef VM_DEBUG
  printf("listPort for %s\n", cname);
#endif

  void* net = (*_virNetworkLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (net == NULL) {
    disconnect(conn);
    return NULL;
  }

  void** nets;

  int count = (*_virNetworkListAllPorts)(net, &nets, 0);

  if (count < 0) {
    (*_virNetworkFree)(net);
    disconnect(conn);
    printf("VM:NetworkVirtual_nlistPort() failed : count=%d\n", count);
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    uuid[0] = 0;
    (*_virNetworkGetUUIDString)(nets[idx], uuid);
#ifdef VM_DEBUG
    printf("net_pool:%s\n", uuid);
#endif
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(uuid));
    (*_virNetworkPortFree)(nets[idx]);
  }

  free(nets);

  (*_virNetworkFree)(net);

  disconnect(conn);

  return array;
}

JNIEXPORT jstring JNICALL Java_javaforce_vm_NetworkVirtual_ngetbridge
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  const char* cname = e->GetStringUTFChars(name, NULL);

  void* net = (*_virNetworkLookupByName)(conn, cname);

  e->ReleaseStringUTFChars(name, cname);

  if (net == NULL) {
    disconnect(conn);
    return NULL;
  }

  const char* cbridge = (*_virNetworkGetBridgeName)(net);

  jstring bridge = e->NewStringUTF(cbridge);

  (*_virNetworkFree)(net);

  disconnect(conn);

  return bridge;
}

//NetworkPort

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkPort_nremove
  (JNIEnv *e, jclass o, jstring parent, jstring name)
{
  //TODO
  return JNI_FALSE;
}

//Device

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_Device_nlist
  (JNIEnv *e, jclass o, jint type)
{
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
    printf("VM:Device_nlist() failed : count=%d\n", count);
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virNodeDeviceGetName)(devs[idx]);
#ifdef VM_DEBUG
    printf("Device:%s\n", name);
#endif
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(name));
    (*_virNodeDeviceFree)(devs[idx]);
  }

  free(devs);

  disconnect(conn);

  return array;
}

static JNINativeMethod javaforce_vm_VMHost[] = {
  {"total_memory", "()J", (void *)&Java_javaforce_vm_VMHost_total_1memory},
  {"free_memory", "()J", (void *)&Java_javaforce_vm_VMHost_free_1memory},
  {"cpu_load", "()J", (void *)&Java_javaforce_vm_VMHost_cpu_1load},
  {"connect", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VMHost_connect},
};

static JNINativeMethod javaforce_vm_Device[] = {
  {"nlist", "(I)[Ljava/lang/String;", (void *)&Java_javaforce_vm_Device_nlist},
};

static JNINativeMethod javaforce_vm_Disk[] = {
  {"ncreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_Disk_ncreate},
};

static JNINativeMethod javaforce_vm_NetworkInterface[] = {
  {"nlistPhys", "()[Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkInterface_nlistPhys},
};

static JNINativeMethod javaforce_vm_NetworkVirtual[] = {
  {"nlistVirt", "()[Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkVirtual_nlistVirt},
  {"nlistPort", "(Ljava/lang/String;)[Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkVirtual_nlistPort},
  {"ncreatevirt", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_ncreatevirt},
  {"ncreateport", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_ncreateport},
  {"ngetbridge", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkVirtual_ngetbridge},
  {"nstart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_nstart},
  {"nstop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_nstop},
  {"nremove", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_nremove},
};

static JNINativeMethod javaforce_vm_NetworkPort[] = {
  {"nremove", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkPort_nremove},
};

static JNINativeMethod javaforce_vm_Storage[] = {
  {"nlist", "()[Ljava/lang/String;", (void *)&Java_javaforce_vm_Storage_nlist},
  {"nregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_Storage_nregister},
  {"nunregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_Storage_nunregister},
  {"nstart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_Storage_nstart},
  {"nstop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_Storage_nstop},
  {"ngetState", "(Ljava/lang/String;)I", (void *)&Java_javaforce_vm_Storage_ngetState},
  {"nformat", "(Ljava/lang/String;I)Z", (void *)&Java_javaforce_vm_Storage_nformat},
};

static JNINativeMethod javaforce_vm_VirtualMachine[] = {
  {"nlist", "()[Ljava/lang/String;", (void *)&Java_javaforce_vm_VirtualMachine_nlist},
  {"nregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nregister},
  {"nunregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nunregister},
  {"nstart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nstart},
  {"nstop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nstop},
  {"ngetState", "(Ljava/lang/String;)I", (void *)&Java_javaforce_vm_VirtualMachine_ngetState},
  {"npoweroff", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_npoweroff},
  {"nrestart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nrestart},
  {"nsuspend", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nsuspend},
  {"nrestore", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nrestore},
  {"nget", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_vm_VirtualMachine_nget},
  {"nmigrate", "(Ljava/lang/String;Ljava/lang/String;ZLjavaforce/vm/Status;)Z", (void *)&Java_javaforce_vm_VirtualMachine_nmigrate},
};

#include "register.h"

void vm_register(JNIEnv *env) {
  //register java natives
  jclass cls;

  cls = findClass(env, "javaforce/vm/VMHost");
  registerNatives(env, cls, javaforce_vm_VMHost, sizeof(javaforce_vm_VMHost)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/Device");
  registerNatives(env, cls, javaforce_vm_Device, sizeof(javaforce_vm_Device)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/Disk");
  registerNatives(env, cls, javaforce_vm_Disk, sizeof(javaforce_vm_Disk)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/NetworkInterface");
  registerNatives(env, cls, javaforce_vm_NetworkInterface, sizeof(javaforce_vm_NetworkInterface)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/NetworkVirtual");
  registerNatives(env, cls, javaforce_vm_NetworkVirtual, sizeof(javaforce_vm_NetworkVirtual)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/NetworkPort");
  registerNatives(env, cls, javaforce_vm_NetworkPort, sizeof(javaforce_vm_NetworkPort)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/Storage");
  registerNatives(env, cls, javaforce_vm_Storage, sizeof(javaforce_vm_Storage)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/vm/VirtualMachine");
  registerNatives(env, cls, javaforce_vm_VirtualMachine, sizeof(javaforce_vm_VirtualMachine)/sizeof(JNINativeMethod));
}
