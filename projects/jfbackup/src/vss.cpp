/** vss - command line interface to volume shadow storage.
  *
  * @author Peter Quiring
  *
  * see https://github.com/albertony/vss
  *
  */

#define VSSVersion "0.1"

#include <windows.h>
#include <vss.h>
#include <vswriter.h>
#include <vsbackup.h>
#include <combaseapi.h>  //StringFromGUID2

#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <cstring>

HMODULE lib;
HRESULT (*pVssInit)(IVssBackupComponents **ppVss);
IVssBackupComponents *pVss;
IVssAsync *pAsync = NULL;
IVssEnumObject *pEnum = NULL;

const int debug = 0;

int mountshadow(const char* path, const char* shadow);

/*
struct GUID {
  unsigned long  Data1;
  unsigned short Data2;
  unsigned short Data3;
  unsigned char  Data4[8];
};
GUID = {445ac2ff-0e87-4f12-9790-bb4091102dd7}
*/

#define char16 short

char16* str8_to_str16(char* str8, char16* str16) {
  char16* dest = str16;
  while (*str8) {
    *(dest++) = *(str8++);
  }
  *(dest++) = 0;
  return str16;
}

char* str16_to_str8(char16* str16, char*str8) {
  char* dest = str8;
  while (*str16) {
    *(dest++) = (*(str16++) & 0xff);
  }
  *(dest++) = 0;
  return str8;
}

char* guid_to_string(GUID *guid, char *str) {
  sprintf(str, "{%02x-%02x-%02x-%02x%02x-%02x%02x%02x%02x%02x%02x}", guid->Data1, guid->Data2, guid->Data3,
    guid->Data4[0], guid->Data4[1], guid->Data4[2], guid->Data4[3],
    guid->Data4[4], guid->Data4[5], guid->Data4[6], guid->Data4[7]
  );
  return str;
}

void usage() {
  printf("vss/%s\n", VSSVersion);
  printf("Usage : vss command [options]\n");
  printf("  Volume Shadow Storage commands:\n");
  printf("  listvols : list volumes\n");
  printf("  listshadows : list shadows\n");
  printf("  createshadow {drive} [mount] : create shadow (optional mount)\n");
  printf("  deleteshadow {shadow} : delete shadow (/all for all shadows)\n");
  printf("  mountshadow {path} {shadow} : mount shadow\n");
  printf("  unmountshadow {path} : unmount shadow\n");
  std::exit(0);
}

void getFunction(HMODULE handle, void **funcPtr, const char *name) {
  void *func;
  func = (void*)GetProcAddress(handle, name);
  if (func == NULL) {
    printf("Error:Can not find function:%s\n", name);
    std::exit(1);
  } else {
    *funcPtr = func;
  }
}

void init_lib() {
  lib = LoadLibrary("vssapi.dll");
  if (lib == NULL) {
    printf("Error:LoadLibrary failed\n");
    std::exit(1);
  }
  getFunction(lib, (void**)&pVssInit, "CreateVssBackupComponentsInternal");
  int res = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
  if (res != S_OK) {
    printf("CoInitializeEx Failed:%x\n", res);
    std::exit(1);
  }
  res = CoInitializeSecurity(
            NULL,                           //  Allow *all* VSS writers to communicate back!
            -1,                             //  Default COM authentication service
            NULL,                           //  Default COM authorization service
            NULL,                           //  reserved parameter
            RPC_C_AUTHN_LEVEL_PKT_PRIVACY,  //  Strongest COM authentication level
            RPC_C_IMP_LEVEL_IMPERSONATE,    //  Minimal impersonation abilities
            NULL,                           //  Default COM authentication settings
            EOAC_DYNAMIC_CLOAKING,          //  Cloaking
            NULL                            //  Reserved parameter
            );
  if (res != S_OK) {
    printf("CoInitializeSecurity Failed:%x\n", res);
    std::exit(1);
  }
  res = (*pVssInit)(&pVss);
  if (res != S_OK) {
    printf("CreateVssBackupComponents Failed:%x\n", res);
    std::exit(1);
  }
  res = pVss->InitializeForBackup(NULL);
  if (res != S_OK) {
    printf("InitializeForBackup Failed:%x\n", res);
    std::exit(1);
  }
}

int listvols() {
  //list volumes that can be shadowed
  char path[16];
  char16 path16[8];
  BOOL supported;
  int cnt = 0;
  for(int drv='C';drv<='Z';drv++) {
    sprintf(path, "%c:\\", drv);
    int res = pVss->IsVolumeSupported(GUID_NULL, (VSS_PWSZ)str8_to_str16(path, path16), &supported);
    if (res != S_OK) {
      if (debug) {
        printf("Error:%x for volume %s\n", res, path);
      }
    } else {
      if (supported) {
        printf("%s\n", path);
        cnt++;
      }
    }
  }
  printf("Found %d supported volumes.\n", cnt);
  return 0;
}

int listshadows() {
  VSS_OBJECT_PROP prop;
  VSS_SNAPSHOT_PROP *snap;
  ULONG copied;
  char guid[64];
  char str8vol[64];
  char str8org[64];
  char16 str16[128];
  int cnt = 0;
  int res = pVss->SetContext(VSS_CTX_ALL);
  if (res != S_OK) {
    printf("SetContext Failed:%x\n", res);
    std::exit(1);
  }
  res = pVss->Query(GUID_NULL, VSS_OBJECT_NONE, VSS_OBJECT_SNAPSHOT, &pEnum);
  if (pEnum == NULL) {
    printf("Query Failed:%x\n", res);
    return 1;
  }
  while (pEnum->Next(1, &prop, &copied) == S_OK) {
    if (prop.Type == VSS_OBJECT_SNAPSHOT) {
      snap = (VSS_SNAPSHOT_PROP *)&prop.Obj;
      guid_to_string(&snap->m_SnapshotId, guid);
      printf("Shadow Volume: %s Shadow ID: %s Volume: %s\n"
        , (char*)str16_to_str8((char16*)snap->m_pwszSnapshotDeviceObject, str8vol)
        , guid
        , (char*)str16_to_str8((char16*)snap->m_pwszOriginalVolumeName, str8org)
      );
      cnt++;
    }
  }
  printf("Found %d shadow copies.\n", cnt);
  return 0;
}

int createshadow(const char* drv, const char* mount) {
  VSS_ID id_set, id_drv;
  char16 drv16[128];
  int res = pVss->SetContext(VSS_CTX_BACKUP);
  if (res != S_OK) {
    printf("SetContext Failed:%x\n", res);
    std::exit(1);
  }
  res = pVss->SetBackupState(true, true, VSS_BT_FULL, false);
  if (res != S_OK) {
    printf("SetContext Failed:%x\n", res);
    std::exit(1);
  }
  res = pVss->GatherWriterMetadata(&pAsync);
  if (res != S_OK) {
    printf("GatherWriterMetadata Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  pAsync->Wait(INFINITE);
  res = pVss->StartSnapshotSet(&id_set);
  if (res != S_OK) {
    printf("StartSnapshotSet Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  res = pVss->AddToSnapshotSet((VSS_PWSZ)str8_to_str16((char*)drv, drv16), GUID_NULL, &id_drv);
  if (res != S_OK) {
    printf("AddToSnapshotSet Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  res = pVss->PrepareForBackup(&pAsync);
  if (res != S_OK) {
    printf("PrepareForBackup Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  pAsync->Wait(INFINITE);
  res = pVss->DoSnapshotSet(&pAsync);
  if (res != S_OK) {
    printf("DoSnapshotSet Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  pAsync->Wait(INFINITE);
  res = pVss->FreeWriterMetadata();
  if (res != S_OK) {
    printf("FreeWriterMetadata Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  char guid[128];
  char str8vol[64];
  VSS_SNAPSHOT_PROP snap;
  res = pVss->GetSnapshotProperties(id_drv, &snap);
  if (res != S_OK) {
    printf("GetSnapshotProperties Failed:%x\n", res);
    pVss->AbortBackup();
    std::exit(1);
  }
  printf("Volume shadow created for Volume %s\n", drv);
  printf("Shadow Volume: %s\n", (char*)str16_to_str8((char16*)snap.m_pwszSnapshotDeviceObject, str8vol));
  printf("ShadowSet ID: %s\n", guid_to_string(&id_set, guid));
  printf("Shadow ID: %s\n", guid_to_string(&id_drv, guid));
  if (mount != NULL) {
    mountshadow(mount, str8vol);
  }
  return 0;
}

int deleteshadow(const char* shadow) {
  VSS_OBJECT_PROP prop;
  VSS_SNAPSHOT_PROP *snap;
  ULONG copied;
  char guid[64];
  VSS_ID notdone;
  LONG done;
  int res = pVss->SetContext(VSS_CTX_ALL);
  if (res != S_OK) {
    printf("SetContext Failed:%x\n", res);
    std::exit(1);
  }
  res = pVss->Query(GUID_NULL, VSS_OBJECT_NONE, VSS_OBJECT_SNAPSHOT, &pEnum);
  if (pEnum == NULL) {
    printf("Query Failed:%x\n", res);
    return 1;
  }
  printf("Searching for %s\n", shadow);
  while (pEnum->Next(1, &prop, &copied) == S_OK) {
    if (prop.Type == VSS_OBJECT_SNAPSHOT) {
      snap = (VSS_SNAPSHOT_PROP *)&prop.Obj;
      guid_to_string(&snap->m_SnapshotId, guid);
      if (stricmp(guid, shadow) == 0) {
        printf("Delete %s\n", shadow);
        res = pVss->DeleteSnapshots(snap->m_SnapshotId, VSS_OBJECT_SNAPSHOT, TRUE, &done, &notdone);
        if (res != S_OK) {
          printf("DeleteSnapshots Failed:%x\n", res);
        }
      }
    }
  }
  return 0;
}

int deleteshadowall() {
  VSS_OBJECT_PROP prop;
  VSS_SNAPSHOT_PROP *snap;
  ULONG copied;
  char guid[64];
  VSS_ID notdone;
  LONG done;
  int res = pVss->SetContext(VSS_CTX_ALL);
  if (res != S_OK) {
    printf("SetContext Failed:%x\n", res);
    std::exit(1);
  }
  res = pVss->Query(GUID_NULL, VSS_OBJECT_NONE, VSS_OBJECT_SNAPSHOT, &pEnum);
  if (pEnum == NULL) {
    printf("Query Failed:%x\n", res);
    return 1;
  }
  while (pEnum->Next(1, &prop, &copied) == S_OK) {
    if (prop.Type == VSS_OBJECT_SNAPSHOT) {
      snap = (VSS_SNAPSHOT_PROP *)&prop.Obj;
      guid_to_string(&snap->m_SnapshotId, guid);
      int res = pVss->DeleteSnapshots(snap->m_SnapshotId, VSS_OBJECT_SNAPSHOT, TRUE, &done, &notdone);
      if (res != S_OK) {
        printf("DeleteSnapshots Failed:%x\n", res);
      }
    }
  }
  return 0;
}


int mountshadow(const char* path, const char* shadow) {
  char shadow2[1024];
  sprintf(shadow2, "%s\\", shadow);
  if (!CreateSymbolicLink(path, shadow2, SYMBOLIC_LINK_FLAG_DIRECTORY)) {
    printf("CreateSymbolicLink Failed\n");
    return 1;
  } else {
    printf("Link created!");
  }
  return 0;
}

int unmountshadow(const char* path) {
  if (!RemoveDirectory(path)) {
    printf("RemoveDirectory Failed\n");
    return 1;
  } else {
    printf("Link destroyed!");
  }
  return -1;
}

int main(int argc, const char** argv) {
  argc--;  //ignore argv[0]
  if (argc < 1) {usage();}
  init_lib();
  int code = 1;
  if (stricmp(argv[1], "LISTVOLS") == 0) {
    code = listvols();
  }
  else if (stricmp(argv[1], "LISTSHADOWS") == 0) {
    code = listshadows();
  }
  else if (stricmp(argv[1], "CREATESHADOW") == 0) {
    if (argc < 2) {usage();}
    if (argc == 2) {
      code = createshadow(argv[2], NULL);
    } else {
      code = createshadow(argv[2], argv[3]);
    }
  }
  else if (stricmp(argv[1], "DELETESHADOW") == 0) {
    if (stricmp(argv[2], "/ALL") == 0) {
      code = deleteshadowall();
    } else {
      code = deleteshadow(argv[2]);
    }
  }
  else if (stricmp(argv[1], "MOUNTSHADOW") == 0) {
    if (argc < 3) {usage();}
    code = mountshadow(argv[2], argv[3]);
  }
  else if (stricmp(argv[1], "UNMOUNTSHADOW") == 0) {
    if (argc < 2) {usage();}
    code = unmountshadow(argv[2]);
  }
  else {
    usage();
  }
  return code;
}
