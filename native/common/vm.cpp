//libvirt api

#include <libvirt/libvirt.h>

#include <sys/stat.h>
#include <string.h>
#include <stdio.h>
#include <fcntl.h>

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
int (*_virDomainGetUUIDString)(void* dom, char* buf);
int	(*_virConnectGetAllDomainStats)(void* conn, int stats, void*** retStats, int flags);
void (*_virDomainStatsRecordListFree)(void** stats);

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
void* (*_virDomainSnapshotCreateXML)(void* dom, const char* xml, int flags);
int (*_virDomainListAllSnapshots)(void* dom, void*** snaps, int flags);
int (*_virDomainHasCurrentSnapshot)(void* dom, int flags);
void* (*_virDomainSnapshotCurrent)(void* dom, int flags);
int (*_virDomainRevertToSnapshot)(void* snap, int flags);
int (*_virDomainSnapshotDelete)(void* snap, int flags);
int (*_virDomainSnapshotFree)(void* snap);
const char* (*_virDomainSnapshotGetName)(void *snap);
void* (*_virDomainSnapshotGetParent)(void *snap, int flags);
const char* (*_virDomainSnapshotGetXMLDesc)(void *snap, int flags);
void* (*_virDomainSnapshotLookupByName)(void* dom, const char* name, int flags);

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
char* (*_virNodeDeviceGetXMLDesc)(void* dev, int flags);
int (*_virNodeDeviceFree)(void* dev);

//secret
void*	(*_virSecretDefineXML)(void* conn, const char* xml, int flags);
int	(*_virSecretFree)(void* secret);
int	(*_virSecretSetValue)(void* secret, const char* value, size_t value_size, int flags);

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_init(JNIEnv *e, jclass o) {
  virt = loadLibrary("/usr/lib/x86_64-linux-gnu/libvirt.so");
  if (virt == NULL) {
    printf("VM:Error:Unable to open libvirt.so\n");
    return JNI_FALSE;
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
  getFunction(virt, (void**)&_virDomainGetUUIDString, "virDomainGetUUIDString");
  getFunction(virt, (void**)&_virConnectGetAllDomainStats, "virConnectGetAllDomainStats");
  getFunction(virt, (void**)&_virDomainStatsRecordListFree, "virDomainStatsRecordListFree");

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
  getFunction(virt, (void**)&_virDomainSnapshotCreateXML, "virDomainSnapshotCreateXML");
  getFunction(virt, (void**)&_virDomainListAllSnapshots, "virDomainListAllSnapshots");
  getFunction(virt, (void**)&_virDomainHasCurrentSnapshot, "virDomainHasCurrentSnapshot");
  getFunction(virt, (void**)&_virDomainSnapshotCurrent, "virDomainSnapshotCurrent");
  getFunction(virt, (void**)&_virDomainRevertToSnapshot, "virDomainRevertToSnapshot");
  getFunction(virt, (void**)&_virDomainSnapshotDelete, "virDomainSnapshotDelete");
  getFunction(virt, (void**)&_virDomainSnapshotFree, "virDomainSnapshotFree");
  getFunction(virt, (void**)&_virDomainSnapshotGetName, "virDomainSnapshotGetName");
  getFunction(virt, (void**)&_virDomainSnapshotGetParent, "virDomainSnapshotGetParent");
  getFunction(virt, (void**)&_virDomainSnapshotGetXMLDesc, "virDomainSnapshotGetXMLDesc");
  getFunction(virt, (void**)&_virDomainSnapshotLookupByName, "virDomainSnapshotLookupByName");

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
  getFunction(virt, (void**)&_virNodeDeviceGetXMLDesc, "virNodeDeviceGetXMLDesc");
  getFunction(virt, (void**)&_virNodeDeviceFree, "virNodeDeviceFree");

  //secret
  getFunction(virt, (void**)&_virSecretDefineXML, "virSecretDefineXML");
  getFunction(virt, (void**)&_virSecretFree, "virSecretFree");
  getFunction(virt, (void**)&_virSecretSetValue, "virSecretSetValue");

  return JNI_TRUE;
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
  sprintf(out, "qemu+ssh://root@%s/system?no_verify=1&no_tty=1&keyfile=/root/cluster/%s", host, host);
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

JNIEXPORT jlong JNICALL Java_javaforce_jni_VMJNI_totalMemory
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

JNIEXPORT jlong JNICALL Java_javaforce_jni_VMJNI_freeMemory
  (JNIEnv *e, jclass o)
{
  void* conn = connect();
  if (conn == NULL) return 0;

  jlong value = (*_virNodeGetFreeMemory)(conn);

  disconnect(conn);

  return value;
}

JNIEXPORT jlong JNICALL Java_javaforce_jni_VMJNI_cpuLoad
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

#include "vm-snapshots.cpp"

#include "vm-stats.cpp"

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_connect
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_register
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_unregister
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_start
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_stop
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_poweroff
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_restart
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_suspend
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_resume
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

JNIEXPORT jint JNICALL Java_javaforce_jni_VMJNI_getState
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_list
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

JNIEXPORT jstring JNICALL Java_javaforce_jni_VMJNI_get
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_migrate
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_storageList
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_storageRegister
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_storageUnregister
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_storageStart
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_storageStop
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

JNIEXPORT jint JNICALL Java_javaforce_jni_VMJNI_storageGetState
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

JNIEXPORT jstring JNICALL Java_javaforce_jni_VMJNI_storageGetUUID
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_diskCreate
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_networkListPhys
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

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_VMJNI_deviceList
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

JNIEXPORT jboolean JNICALL Java_javaforce_jni_VMJNI_secretCreate
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
  {"totalMemory", "()J", (void *)&Java_javaforce_jni_VMJNI_totalMemory},
  {"freeMemory", "()J", (void *)&Java_javaforce_jni_VMJNI_freeMemory},
  {"cpuLoad", "()J", (void *)&Java_javaforce_jni_VMJNI_cpuLoad},
  {"connect", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_connect},
  {"getAllStats", "(IIIII)Z", (void *)&Java_javaforce_jni_VMJNI_getAllStats},
};

static JNINativeMethod javaforce_vm_Device[] = {
  {"deviceList", "(I)[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_deviceList},
};

static JNINativeMethod javaforce_vm_Disk[] = {
  {"diskCreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_diskCreate},
};

static JNINativeMethod javaforce_vm_NetworkInterface[] = {
  {"networkListPhys", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_networkListPhys},
};

static JNINativeMethod javaforce_vm_Storage[] = {
  {"storageList", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_storageList},
  {"storageRegister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_storageRegister},
  {"storageUnregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_storageUnregister},
  {"storageStart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_storageStart},
  {"storageStop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_storageStop},
  {"storageGetState", "(Ljava/lang/String;)I", (void *)&Java_javaforce_jni_VMJNI_storageGetState},
  {"storageGetUUID", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_storageGetUUID},
};

static JNINativeMethod javaforce_vm_VirtualMachine[] = {
  {"init", "()Z", (void *)&Java_javaforce_jni_VMJNI_init},
  {"list", "()[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_list},
  {"register", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_register},
  {"unregister", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_unregister},
  {"start", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_start},
  {"stop", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_stop},
  {"getState", "(Ljava/lang/String;)I", (void *)&Java_javaforce_jni_VMJNI_getState},
  {"poweroff", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_poweroff},
  {"restart", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_restart},
  {"suspend", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_suspend},
  {"resume", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_resume},
  {"get", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_get},
  {"migrate", "(Ljava/lang/String;Ljava/lang/String;ZLjavaforce/webui/tasks/Status;)Z", (void *)&Java_javaforce_jni_VMJNI_migrate},
  {"snapshotCreate", "(Ljava/lang/String;Ljava/lang/String;I)Z", (void *)&Java_javaforce_jni_VMJNI_snapshotCreate},
  {"snapshotList", "(Ljava/lang/String;)[Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_snapshotList},
  {"snapshotExists", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_snapshotExists},
  {"snapshotGetCurrent", "(Ljava/lang/String;)Ljava/lang/String;", (void *)&Java_javaforce_jni_VMJNI_snapshotGetCurrent},
  {"snapshotRestore", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_snapshotRestore},
  {"snapshotDelete", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_snapshotDelete},
};

static JNINativeMethod javaforce_vm_Secret[] = {
  {"secretCreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_jni_VMJNI_secretCreate},
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
