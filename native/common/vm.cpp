//libvirt api

#include <libvirt/libvirt.h>

JF_LIB_HANDLE virt;

//common
void* (*_virConnectOpen)(const char* name);
int (*_virConnectClose)(void* conn);

//domains
void* (*_virDomainDefineXML)(void* conn, const char* xml);
void (*_virDomainFree)(void* dom);
int (*_virDomainUndefine)(void* dom);
void* (*_virDomainLookupByName)(void* conn, const char* name);
int (*_virDomainCreate)(void* dom);
int (*_virDomainShutdown)(void* dom);
int (*_virDomainDestroy)(void* dom);
int (*_virDomainReset)(void* dom);
int (*_virDomainSuspend)(void* dom);
int (*_virDomainResume)(void* dom);
int (*_virDomainGetState)(void* dom, int* state, int* reason, int flags);
int (*_virConnectListAllDomains)(void* conn, void*** doms, int flags);
char* (*_virDomainGetMetadata)(void* dom, int type, const char* uri, int flags);
void* (*_virDomainMigrate3)(void* dom, void* dconn, void* params, int nparams, int flags);

//storage
void* (*_virStoragePoolDefineXML)(void* conn, const char* xml);
int (*_virStoragePoolFree)(void* pool);
int (*_virStoragePoolUndefine)(void* pool);
void* (*_virStoragePoolLookupByName)(void* conn, const char* name);
int (*_virStoragePoolCreate)(void* pool);
int (*_virStoragePoolDestroy)(void* pool);
int (*_virStoragePoolGetInfo)(void* pool, virStoragePoolInfo* info);
int (*_virConnectListAllStoragePools)(void* conn, void*** pools, int flags);
int (*_virStoragePoolGetUUIDString)(void* pool, char* buf);

//disk

//network (interface)
int (*_virConnectListAllInterfaces)(void* conn, void*** ifaces, int flags);
const char* (*_virInterfaceGetName)(void* iface);
int (*_virInterfaceFree)(void* iface);
void* (*_virNetworkDefineXML)(void* conn, const char* xml);

//network (virtual)
int (*_virConnectListAllNetworks)(void* conn, void*** nets, int flags);
void* (*_virNetworkLookupByName)(void* conn, const char* name);
int (*_virNetworkGetUUIDString)(void* net, char* uuid);
int (*_virNetworkFree)(void* net);

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
  if (virt == NULL) return;

  //common
  getFunction(virt, (void**)&_virConnectOpen, "virConnectOpen");
  getFunction(virt, (void**)&_virConnectClose, "virConnectClose");

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
  getFunction(virt, (void**)&_virDomainMigrate3, "virDomainMigrate3");

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

  //disk

  //network (interface)
  getFunction(virt, (void**)&_virConnectListAllInterfaces, "virConnectListAllInterfaces");
  getFunction(virt, (void**)&_virInterfaceGetName, "virInterfaceGetName");
  getFunction(virt, (void**)&_virInterfaceFree, "virInterfaceFree");
  getFunction(virt, (void**)&_virNetworkDefineXML, "virNetworkDefineXML");

  //network (virtual)
  getFunction(virt, (void**)&_virConnectListAllNetworks, "virConnectListAllNetworks");
  getFunction(virt, (void**)&_virNetworkLookupByName, "virNetworkLookupByName");
  getFunction(virt, (void**)&_virNetworkGetUUIDString, "virNetworkGetUUIDString");
  getFunction(virt, (void**)&_virNetworkFree, "virNetworkFree");

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

static void* connect(const char* host) {
  char url[1024];
  sprintf(url, "qemu://%s", host);
  return (*_virConnectOpen)(url);
}

static void* connect() {
  return connect("localhost");
}

static void disconnect(void* ptr) {
  (*_virConnectClose)(ptr);
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

  int res = (*_virDomainShutdown)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

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

  int res = (*_virDomainReset)(dom);

  (*_virDomainFree)(dom);
  disconnect(conn);

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
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* desc = (*_virDomainGetMetadata)(doms[idx], VIR_DOMAIN_METADATA_DESCRIPTION, NULL, VIR_DOMAIN_AFFECT_CURRENT);
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

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VirtualMachine_migrate
  (JNIEnv *e, jobject o, jstring name, jstring desthost, jobject status)
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

  const char* cdesthost = e->GetStringUTFChars(desthost, NULL);

  void* dconn = connect(cdesthost);

  e->ReleaseStringUTFChars(desthost, cdesthost);

  if (dconn == NULL) {
    disconnect(conn);
    return JNI_FALSE;
  }

  void* ddom = (*_virDomainMigrate3)(dom, dconn, NULL, 0, 0);

  (*_virDomainFree)(dom);
  disconnect(dconn);
  disconnect(conn);

  if (ddom == NULL) return JNI_FALSE;
  (*_virDomainFree)(ddom);

  return JNI_TRUE;
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
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    uuid[0] = 0;
    (*_virStoragePoolGetUUIDString)(pools[idx], uuid);
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(uuid));
    (*_virStoragePoolFree)(pools[idx]);
  }

  free(pools);

  disconnect(conn);

  return array;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_nregister
  (JNIEnv *e, jobject o, jstring name, jstring xml)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  const char* cxml = e->GetStringUTFChars(xml, NULL);

  void* pool = (*_virStoragePoolDefineXML)(conn, cxml);

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

  int res = (*_virStoragePoolCreate)(pool);

  (*_virStoragePoolFree)(pool);
  disconnect(conn);

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

  if (res != 0) return -1;
  switch (info.state) {
    case VIR_STORAGE_POOL_INACTIVE: return 0;
    case VIR_STORAGE_POOL_RUNNING: return 1;
    case VIR_STORAGE_POOL_BUILDING: return 2;
  }
  return 3;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Storage_format
  (JNIEnv *e, jobject o, jstring path, jint type)
{
  //type : 1=EXT4
  //TODO:format storage pool
  return JNI_FALSE;
}

//Disk

JNIEXPORT jboolean JNICALL Java_javaforce_vm_Disk_ncreate
  (JNIEnv *e, jclass o, jint type, jint prov, jlong size, jstring path)
{
  //type : 1=QCOW2 2=VMDK
  //prov : 1=THIN 2=THICK
  return JNI_FALSE;
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
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virInterfaceGetName)(ifaces[idx]);
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(name));
    (*_virInterfaceFree)(ifaces[idx]);
  }

  free(ifaces);

  disconnect(conn);

  return array;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_NetworkInterface_nlistVirt
  (JNIEnv *e, jclass o, jstring name)
{
  void* conn = connect();
  if (conn == NULL) return NULL;

  void** nets;

  int count = (*_virConnectListAllNetworks)(conn, &nets, 0);

  if (count < 0) {
    disconnect(conn);
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    uuid[0] = 0;
    (*_virNetworkGetUUIDString)(nets[idx], uuid);
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(uuid));
    (*_virNetworkFree)(nets[idx]);
  }

  free(nets);

  disconnect(conn);

  return array;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkInterface_ncreate
  (JNIEnv *e, jclass o, jstring name, jstring xml)
{
  //TODO:create network virtual
  return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkInterface_nremove
  (JNIEnv *e, jclass o, jstring name)
{
  //TODO:remove network virtual
  return JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_vm_NetworkVirtual_nlistPort
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

  void** nets;

  int count = (*_virNetworkListAllPorts)(net, &nets, 0);

  if (count < 0) {
    (*_virNetworkFree)(net);
    disconnect(conn);
    return NULL;
  }

  char uuid[VIR_UUID_STRING_BUFLEN];  //includes space for NULL
  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    uuid[0] = 0;
    (*_virNetworkGetUUIDString)(nets[idx], uuid);
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(uuid));
    (*_virNetworkPortFree)(nets[idx]);
  }

  free(nets);

  (*_virNetworkFree)(net);

  disconnect(conn);

  return array;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_nassign
  (JNIEnv *e, jclass o, jstring name, jstring ip, jstring mask)
{
  //TODO:assign IP address to virt nic
  return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_ncreate
  (JNIEnv *e, jclass o, jstring name, jstring xml)
{
  //TODO:create network port
  return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_NetworkVirtual_nremove
  (JNIEnv *e, jclass o, jstring name)
{
  //TODO:remove network port
  return JNI_FALSE;
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
    return NULL;
  }

  jobjectArray array = e->NewObjectArray(count, e->FindClass("java/lang/String"), e->NewStringUTF(""));
  for(int idx=0;idx<count;idx++) {
    const char* name = (*_virNodeDeviceGetName)(devs[idx]);
    e->SetObjectArrayElement(array, idx, e->NewStringUTF(name));
    free((void*)name);
    (*_virNodeDeviceFree)(devs[idx]);
  }

  free(devs);

  disconnect(conn);

  return array;
}

static JNINativeMethod javaforce_vm_Device[] = {
  {"nlist", "(I)[Ljava/lang/String;", (void *)&Java_javaforce_vm_Device_nlist},
};

static JNINativeMethod javaforce_vm_Disk[] = {
  {"ncreate", "(IIJLjava/lang/String;)Z", (void *)&Java_javaforce_vm_Disk_ncreate},
};

static JNINativeMethod javaforce_vm_NetworkInterface[] = {
  {"nlistPhys", "()[Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkInterface_nlistPhys},
  {"nlistVirt", "(Ljava/lang/String;)[Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkInterface_nlistVirt},
  {"ncreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkInterface_ncreate},
};

static JNINativeMethod javaforce_vm_NetworkVirtual[] = {
  {"nlistPort", "(Ljava/lang/String;)[Ljava/lang/String;", (void *)&Java_javaforce_vm_NetworkVirtual_nlistPort},
  {"ncreate", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_ncreate},
  {"nremove", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_nremove},
  {"nassign", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_vm_NetworkVirtual_nassign},
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
  {"format", "(Ljava/lang/String;I)Z", (void *)&Java_javaforce_vm_Storage_format},
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
  {"migrate", "(Ljava/lang/String;Ljava/lang/String;Ljavaforce/vm/Status;)Z", (void *)&Java_javaforce_vm_VirtualMachine_migrate},
};

#include "register.h"

void vm_register(JNIEnv *env) {
  //register java natives
  jclass cls;

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
