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

jboolean vmInit() {
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

#include "vm-snapshots-jni.cpp"
#include "vm-snapshots-ffm.cpp"

#include "vm-stats-jni.cpp"
#include "vm-stats-ffm.cpp"

#include "vm-jni.cpp"
#include "vm-ffm.cpp"
