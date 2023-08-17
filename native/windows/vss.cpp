/** vss - volume shadow storage native API.
  *
  * @author Peter Quiring
  *
  */

#include <vss.h>
#include <vswriter.h>
#include <vsbackup.h>

#include "../common/vector.cpp"

HMODULE lib;
HRESULT (*pVssInit)(IVssBackupComponents **ppVss);
IVssBackupComponents *pVss;
IVssAsync *pAsync = NULL;
IVssEnumObject *pEnum = NULL;

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

static char16* str8_to_str16(char* str8, char16* str16) {
  char16* dest = str16;
  while (*str8) {
    *(dest++) = *(str8++);
  }
  *(dest++) = 0;
  return str16;
}

static char* str16_to_str8(char16* str16, char*str8) {
  char* dest = str8;
  while (*str16) {
    *(dest++) = (*(str16++) & 0xff);
  }
  *(dest++) = 0;
  return str8;
}

static char* guid_to_string(GUID *guid, char *str) {
  sprintf(str, "{%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x}", guid->Data1, guid->Data2, guid->Data3,
    guid->Data4[0], guid->Data4[1], guid->Data4[2], guid->Data4[3],
    guid->Data4[4], guid->Data4[5], guid->Data4[6], guid->Data4[7]
  );
  return str;
}

static void vssGetFunction(HMODULE handle, void **funcPtr, const char *name) {
  void *func;
  func = (void*)GetProcAddress(handle, name);
  if (func == NULL) {
    printf("Error:Can not find function:%s\n", name);
    return;
  } else {
    *funcPtr = func;
  }
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_vssInit(JNIEnv *e, jclass c) {
  lib = LoadLibrary("vssapi.dll");
  if (lib == NULL) {
    printf("VSS:LoadLibrary Failed\n");
    return JNI_FALSE;
  }
  vssGetFunction(lib, (void**)&pVssInit, "CreateVssBackupComponentsInternal");
  int res = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
  if (res != S_OK) {
    printf("VSS:CoInitializeEx Failed:%x\n", res);
    return JNI_FALSE;
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
    printf("VSS:CoInitializeSecurity Failed:%x\n", res);
    return JNI_FALSE;
  }
  res = (*pVssInit)(&pVss);
  if (res != S_OK) {
    printf("VSS:CreateVssBackupComponents Failed:%x\n", res);
    return JNI_FALSE;
  }
  res = pVss->InitializeForBackup(NULL);
  if (res != S_OK) {
    printf("VSS:InitializeForBackup Failed:%x\n", res);
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_WinNative_vssListVols(JNIEnv *e, jclass c) {
  //list volumes that can be shadowed
  char path[16];
  char16 path16[8];
  BOOL supported;
  Vector<jstring> strlst;
  jclass strcls = e->FindClass("java/lang/String");
  int cnt = 0;
  for(int drv='C';drv<='Z';drv++) {
    sprintf(path, "%c:\\", drv);
    int res = pVss->IsVolumeSupported(GUID_NULL, (VSS_PWSZ)str8_to_str16(path, path16), &supported);
    if (res == S_OK && supported) {
      strlst.add(e->NewStringUTF(path));
      cnt++;
    }
  }
  jobjectArray strs = e->NewObjectArray(cnt, strcls, NULL);
  for(int idx=0;idx<cnt;idx++) {
    e->SetObjectArrayElement(strs, idx, strlst.getAt(idx));
  }
  return strs;
}

JNIEXPORT jobjectArray JNICALL Java_javaforce_jni_WinNative_vssListShadows(JNIEnv *e, jclass c) {
  VSS_OBJECT_PROP prop;
  VSS_SNAPSHOT_PROP *snap;
  ULONG copied;
  char guid[64];
  char str8vol[64];
  char str8org[64];
  char16 str16[128];
  int cnt = 0;
  jclass strcls = e->FindClass("java/lang/String");
  jclass strarraycls = e->FindClass("[Ljava/lang/String;");
  int res = pVss->SetContext(VSS_CTX_ALL);
  if (res != S_OK) {
    printf("VSS:SetContext Failed:%x\n", res);
    return NULL;
  }
  res = pVss->Query(GUID_NULL, VSS_OBJECT_NONE, VSS_OBJECT_SNAPSHOT, &pEnum);
  if (pEnum == NULL) {
    printf("VSS:Query Failed:%x\n", res);
    return NULL;
  }
  Vector<jobjectArray> strlst;
  while (pEnum->Next(1, &prop, &copied) == S_OK) {
    if (prop.Type == VSS_OBJECT_SNAPSHOT) {
      snap = (VSS_SNAPSHOT_PROP *)&prop.Obj;
      jobjectArray strs = e->NewObjectArray(3, strcls, NULL);
      guid_to_string(&snap->m_SnapshotId, guid);
      e->SetObjectArrayElement(strs, 0, e->NewStringUTF(guid));
      str16_to_str8((char16*)snap->m_pwszSnapshotDeviceObject, str8vol);
      e->SetObjectArrayElement(strs, 1, e->NewStringUTF(str8vol));
      str16_to_str8((char16*)snap->m_pwszOriginalVolumeName, str8org);
      e->SetObjectArrayElement(strs, 2, e->NewStringUTF(str8org));
      strlst.add(strs);
      cnt++;
    }
  }
  jobjectArray strstrs = e->NewObjectArray(cnt, strarraycls, NULL);
  for(int idx=0;idx<cnt;idx++) {
    e->SetObjectArrayElement(strstrs, idx, strlst.getAt(idx));
  }
  return strstrs;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_vssCreateShadow(JNIEnv *e, jclass c, jstring drv, jstring mount) {
  VSS_ID id_set, id_drv;
  char16 drv16[128];
  int res = pVss->SetContext(VSS_CTX_BACKUP);
  if (res != S_OK) {
    printf("VSS:SetContext Failed:%x\n", res);
    return JNI_FALSE;
  }
  res = pVss->SetBackupState(true, true, VSS_BT_FULL, false);
  if (res != S_OK) {
    printf("VSS:SetContext Failed:%x\n", res);
    return JNI_FALSE;
  }
  res = pVss->GatherWriterMetadata(&pAsync);
  if (res != S_OK) {
    printf("VSS:GatherWriterMetadata Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  pAsync->Wait(INFINITE);
  res = pVss->StartSnapshotSet(&id_set);
  if (res != S_OK) {
    printf("VSS:StartSnapshotSet Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  const char *c_drv = e->GetStringUTFChars(drv, NULL);
  res = pVss->AddToSnapshotSet((VSS_PWSZ)str8_to_str16((char*)c_drv, drv16), GUID_NULL, &id_drv);
  e->ReleaseStringUTFChars(drv, c_drv);
  if (res != S_OK) {
    printf("VSS:AddToSnapshotSet Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  res = pVss->PrepareForBackup(&pAsync);
  if (res != S_OK) {
    printf("VSS:PrepareForBackup Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  pAsync->Wait(INFINITE);
  res = pVss->DoSnapshotSet(&pAsync);
  if (res != S_OK) {
    printf("VSS:DoSnapshotSet Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  pAsync->Wait(INFINITE);
  res = pVss->FreeWriterMetadata();
  if (res != S_OK) {
    printf("VSS:FreeWriterMetadata Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  char guid_set[128];
  char guid_vol[128];
  char str8vol[64];
  VSS_SNAPSHOT_PROP snap;
  res = pVss->GetSnapshotProperties(id_drv, &snap);
  if (res != S_OK) {
    printf("VSS:GetSnapshotProperties Failed:%x\n", res);
    pVss->AbortBackup();
    return JNI_FALSE;
  }
  jboolean ret = JNI_TRUE;
  char* shadow_volume = str16_to_str8((char16*)snap.m_pwszSnapshotDeviceObject, str8vol);
  char* shadow_set_id = guid_to_string(&id_set, guid_set);
  char* shadow_vol_id = guid_to_string(&id_drv, guid_vol);
  if (mount != NULL) {
    jstring shadow = e->NewStringUTF(str8vol);
    ret = Java_javaforce_jni_WinNative_vssMountShadow(e, c, mount, shadow);
    e->DeleteLocalRef(shadow);
  }
  return ret;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_vssDeleteShadow(JNIEnv *e, jclass c, jstring shadow) {
  VSS_OBJECT_PROP prop;
  VSS_SNAPSHOT_PROP *snap;
  ULONG copied;
  char guid[64];
  VSS_ID notdone;
  LONG done;
  int res = pVss->SetContext(VSS_CTX_ALL);
  if (res != S_OK) {
    printf("VSS:SetContext Failed:%x\n", res);
    return JNI_FALSE;
  }
  res = pVss->Query(GUID_NULL, VSS_OBJECT_NONE, VSS_OBJECT_SNAPSHOT, &pEnum);
  if (pEnum == NULL) {
    printf("VSS:Query Failed:%x\n", res);
    return 1;
  }
  const char *c_shadow = e->GetStringUTFChars(shadow, NULL);
  jboolean ret = JNI_TRUE;
  while (pEnum->Next(1, &prop, &copied) == S_OK) {
    if (prop.Type == VSS_OBJECT_SNAPSHOT) {
      snap = (VSS_SNAPSHOT_PROP *)&prop.Obj;
      guid_to_string(&snap->m_SnapshotId, guid);
      if (stricmp(guid, c_shadow) == 0) {
        res = pVss->DeleteSnapshots(snap->m_SnapshotId, VSS_OBJECT_SNAPSHOT, TRUE, &done, &notdone);
        if (res != S_OK) {
          printf("VSS:DeleteSnapshots Failed:%x\n", res);
          ret = JNI_FALSE;
        }
      }
    }
  }
  e->ReleaseStringUTFChars(shadow, c_shadow);
  return ret;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_vssDeleteShadowAll(JNIEnv *e, jclass c) {
  VSS_OBJECT_PROP prop;
  VSS_SNAPSHOT_PROP *snap;
  ULONG copied;
  char guid[64];
  VSS_ID notdone;
  LONG done;
  int res = pVss->SetContext(VSS_CTX_ALL);
  if (res != S_OK) {
    printf("VSS:SetContext Failed:%x\n", res);
    return JNI_FALSE;
  }
  res = pVss->Query(GUID_NULL, VSS_OBJECT_NONE, VSS_OBJECT_SNAPSHOT, &pEnum);
  if (pEnum == NULL) {
    printf("VSS:Query Failed:%x\n", res);
    return 1;
  }
  while (pEnum->Next(1, &prop, &copied) == S_OK) {
    if (prop.Type == VSS_OBJECT_SNAPSHOT) {
      snap = (VSS_SNAPSHOT_PROP *)&prop.Obj;
      guid_to_string(&snap->m_SnapshotId, guid);
      int res = pVss->DeleteSnapshots(snap->m_SnapshotId, VSS_OBJECT_SNAPSHOT, TRUE, &done, &notdone);
      if (res != S_OK) {
        printf("VSS:DeleteSnapshots Failed:%x\n", res);
      }
    }
  }
  return 0;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_vssMountShadow(JNIEnv *e, jclass c, jstring shadow, jstring mount) {
  const char *c_shadow = e->GetStringUTFChars(shadow, NULL);
  const char *c_mount = e->GetStringUTFChars(mount, NULL);
  char shadow2[1024];
  sprintf(shadow2, "%s\\", c_shadow);
  jboolean ret = JNI_TRUE;
  if (!CreateSymbolicLink(c_mount, shadow2, SYMBOLIC_LINK_FLAG_DIRECTORY)) {
    printf("VSS:CreateSymbolicLink Failed\n");
    ret = JNI_FALSE;
  }
  e->ReleaseStringUTFChars(shadow, c_shadow);
  e->ReleaseStringUTFChars(mount, c_mount);
  return ret;
}

JNIEXPORT jboolean JNICALL Java_javaforce_jni_WinNative_vssUnmountShadow(JNIEnv *e, jclass c, jstring mount) {
  const char *c_mount = e->GetStringUTFChars(mount, NULL);
  jboolean ret = JNI_TRUE;
  if (!RemoveDirectory(c_mount)) {
    printf("VSS:RemoveDirectory Failed\n");
    ret = JNI_FALSE;
  }
  e->ReleaseStringUTFChars(mount, c_mount);
  return ret;
}
